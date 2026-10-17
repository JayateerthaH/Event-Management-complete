package com.event.socialmodule.service;

import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventCard;
import com.event.profilemodule.communication.ProfileApi;
import com.event.socialmodule.entity.*;
import com.event.socialmodule.entity.BuddyRequest.Status;
import com.event.socialmodule.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Event buddies: students opt in per event (or to the interest search), send "Say hi" requests, and become
 * friends only when the other person accepts. Contact details are shown to friends only. Blocked people never
 * see each other, ignored requests are never revealed to the sender, and requests are rate limited.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BuddyService {

    static final int MAX_REQUESTS_PER_DAY = 20;
    static final int MAX_SEARCH_RESULTS = 50;

    private final BuddySettingsRepository settingsRepository;
    private final BuddyOptInRepository optInRepository;
    private final BuddyRequestRepository requestRepository;
    private final UserBlockRepository blockRepository;
    private final UserReportRepository reportRepository;
    private final BuddyNotificationRepository notificationRepository;
    private final EventApi eventApi;
    private final ProfileApi profileApi;
    private final org.springframework.context.ApplicationEventPublisher activityPublisher;

    public enum Relation { NONE, REQUESTED, INCOMING, FRIENDS }

    public record SettingsView(String displayName, String contact, boolean discoverable, boolean shareAttendance,
                               List<String> myInterests) {}

    public record SettingsUpdate(String displayName, String contact, Boolean discoverable, Boolean shareAttendance) {}

    /** Another student as you see them. contact is set only for friends. */
    public record Person(Long userId, String name, String department, String yearOfStudy, List<String> interests,
                         List<String> sharedInterests, Relation relation, Long requestId, String contact) {}

    public record EventBuddies(boolean registered, boolean optedIn, boolean profileReady, int optedInCount,
                               List<Person> people, List<Person> friendsGoing) {}

    public record RequestView(Long id, Person person, Long eventId, String eventName, LocalDateTime createdAt) {}

    public record Requests(List<RequestView> incoming, List<RequestView> outgoing) {}

    public record FriendView(Person person, List<EventCard> upcomingEvents) {}

    public record NotificationView(Long id, String type, String message, Long eventId, Long actorUserId, boolean read,
                                   LocalDateTime createdAt) {}

    public record ReportView(Long id, Long reporterId, String reporterName, Long reportedId, String reportedName,
                             String reason, String status, LocalDateTime createdAt) {}

    // ---------- settings ----------

    @Transactional(readOnly = true)
    public SettingsView settings(Long me) {
        BuddySettings s = settingsRepository.findById(me).orElseGet(() -> new BuddySettings(me));
        ProfileApi.PublicProfile profile = profileApi.publicProfiles(List.of(me)).get(me);
        String name = s.getDisplayName() != null ? s.getDisplayName() : profile == null ? null : profile.name();
        return new SettingsView(name, s.getContact(), s.isDiscoverable(), s.isShareAttendance(),
                profile == null ? List.of() : profile.interests());
    }

    public SettingsView updateSettings(Long me, SettingsUpdate update) {
        BuddySettings s = settingsRepository.findById(me).orElseGet(() -> new BuddySettings(me));
        if (update.displayName() != null) {
            String name = update.displayName().trim();
            if (name.isEmpty()) throw bad("Enter the name other students will see");
            if (name.length() > 60) throw bad("The name can be at most 60 characters");
            s.setDisplayName(name);
        }
        if (update.contact() != null) {
            String contact = update.contact().trim();
            if (contact.length() > 120) throw bad("The contact can be at most 120 characters");
            s.setContact(contact.isEmpty() ? null : contact);
        }
        if (update.discoverable() != null) {
            if (update.discoverable() && s.getDisplayName() == null) throw bad("Enter the name other students will see first");
            s.setDiscoverable(update.discoverable());
        }
        if (update.shareAttendance() != null) s.setShareAttendance(update.shareAttendance());
        s.setUpdatedAt(LocalDateTime.now());
        settingsRepository.save(s);
        return settings(me);
    }

    @Transactional(readOnly = true)
    public List<String> allInterests() {
        return profileApi.allInterestNames();
    }

    // ---------- event buddies ----------

    @Transactional(readOnly = true)
    public EventBuddies eventBuddies(Long me, Long eventId) {
        boolean registered = eventApi.isRegistered(me, eventId);
        boolean optedIn = registered && optInRepository.existsByUserIdAndEventId(me, eventId);
        boolean profileReady = settingsRepository.findById(me).map(s -> s.getDisplayName() != null).orElse(false);
        Set<Long> hidden = hiddenFrom(me);

        List<Long> others = optInRepository.findByEventId(eventId).stream().map(BuddyOptIn::getUserId)
                .filter(id -> !id.equals(me) && !hidden.contains(id) && eventApi.isRegistered(id, eventId))
                .toList();
        // Only students who opted in themselves can see who else did.
        List<Person> people = optedIn ? people(me, others) : List.of();

        List<Long> friendsGoing = friendIds(me).stream()
                .filter(id -> !hidden.contains(id) && sharesAttendance(id) && eventApi.isRegistered(id, eventId))
                .toList();
        return new EventBuddies(registered, optedIn, profileReady, others.size(), people, people(me, friendsGoing));
    }

    public EventBuddies optIn(Long me, Long eventId) {
        if (!eventApi.isRegistered(me, eventId)) throw bad("Register for the event first");
        requireName(me);
        if (!optInRepository.existsByUserIdAndEventId(me, eventId)) {
            optInRepository.save(new BuddyOptIn(me, eventId));
            activityPublisher.publishEvent(new com.event.sharedmodule.UserActivityEvent(me, "buddy_opt_in", eventId));
        }
        return eventBuddies(me, eventId);
    }

    public EventBuddies optOut(Long me, Long eventId) {
        optInRepository.deleteByUserIdAndEventId(me, eventId);
        return eventBuddies(me, eventId);
    }

    // ---------- search by interests ----------

    @Transactional(readOnly = true)
    public List<Person> search(Long me, String query, Collection<String> interests) {
        BuddySettings mine = settingsRepository.findById(me).orElse(null);
        if (mine == null || !mine.isDiscoverable()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Turn on \"Let others find me\" to search for people");
        }
        Set<Long> hidden = hiddenFrom(me);
        Map<Long, BuddySettings> candidates = settingsRepository.findByDiscoverableTrueAndDisplayNameIsNotNull().stream()
                .filter(s -> !s.getUserId().equals(me) && !hidden.contains(s.getUserId()))
                .collect(Collectors.toMap(BuddySettings::getUserId, Function.identity()));
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Set<String> wanted = interests == null ? Set.of() : interests.stream()
                .filter(Objects::nonNull).map(i -> i.trim().toLowerCase(Locale.ROOT)).filter(i -> !i.isEmpty())
                .collect(Collectors.toSet());

        return people(me, candidates.keySet()).stream()
                .filter(p -> q.isEmpty() || p.name().toLowerCase(Locale.ROOT).contains(q)
                        || (p.department() != null && p.department().toLowerCase(Locale.ROOT).contains(q))
                        || p.interests().stream().anyMatch(i -> i.toLowerCase(Locale.ROOT).contains(q)))
                .filter(p -> wanted.isEmpty() || p.interests().stream().anyMatch(i -> wanted.contains(i.toLowerCase(Locale.ROOT))))
                .sorted(Comparator.comparingInt((Person p) -> -p.sharedInterests().size())
                        .thenComparing(Person::name, String.CASE_INSENSITIVE_ORDER))
                .limit(MAX_SEARCH_RESULTS)
                .toList();
    }

    // ---------- requests ----------

    public Person sendRequest(Long me, Long to, Long eventId) {
        if (to == null || to.equals(me)) throw bad("Choose another student");
        requireName(me);
        if (hiddenFrom(me).contains(to)) throw notFound();
        if (!canReach(me, to, eventId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can say hi to students who opted in at the same event, or who can be found by interests");
        }
        List<BuddyRequest> existing = requestRepository.between(me, to);
        if (existing.stream().anyMatch(r -> r.getStatus() == Status.ACCEPTED)) return person(me, to);
        Optional<BuddyRequest> incoming = existing.stream()
                .filter(r -> r.getStatus() == Status.PENDING && r.getFromUserId().equals(to)).findFirst();
        if (incoming.isPresent()) {   // they already asked: saying hi back means yes
            accept(me, incoming.get().getId());
            return person(me, to);
        }
        // Already asked, or they ignored it: the sender just keeps seeing "Requested".
        if (existing.stream().anyMatch(r -> r.getFromUserId().equals(me)
                && (r.getStatus() == Status.PENDING || r.getStatus() == Status.IGNORED))) {
            return person(me, to);
        }
        if (requestRepository.countByFromUserIdAndCreatedAtAfter(me, LocalDateTime.now().minusDays(1)) >= MAX_REQUESTS_PER_DAY) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "You've sent " + MAX_REQUESTS_PER_DAY + " requests today. Try again tomorrow.");
        }
        requestRepository.save(new BuddyRequest(me, to, eventId));
        activityPublisher.publishEvent(new com.event.sharedmodule.UserActivityEvent(me, "buddy_request", eventId));
        String where = eventName(eventId).map(n -> " (" + n + ")").orElse("");
        notify(to, BuddyNotification.Type.BUDDY_REQUEST, me, eventId, nameOf(me) + " wants to connect" + where);
        return person(me, to);
    }

    @Transactional(readOnly = true)
    public Requests requests(Long me) {
        Set<Long> hidden = hiddenFrom(me);
        List<BuddyRequest> pending = requestRepository.involving(me, List.of(Status.PENDING)).stream()
                .filter(r -> !hidden.contains(r.otherThan(me))).toList();
        Map<Long, Person> byId = people(me, pending.stream().map(r -> r.otherThan(me)).toList()).stream()
                .collect(Collectors.toMap(Person::userId, Function.identity()));
        Function<BuddyRequest, RequestView> view = r -> new RequestView(r.getId(), byId.get(r.otherThan(me)),
                r.getEventId(), eventName(r.getEventId()).orElse(null), r.getCreatedAt());
        return new Requests(
                pending.stream().filter(r -> r.getToUserId().equals(me)).map(view).toList(),
                pending.stream().filter(r -> r.getFromUserId().equals(me)).map(view).toList());
    }

    public Person accept(Long me, Long requestId) {
        BuddyRequest r = incomingPending(me, requestId);
        r.setStatus(Status.ACCEPTED);
        r.setRespondedAt(LocalDateTime.now());
        requestRepository.save(r);
        activityPublisher.publishEvent(new com.event.sharedmodule.UserActivityEvent(me, "buddy_accepted", r.getEventId()));
        notify(r.getFromUserId(), BuddyNotification.Type.BUDDY_ACCEPTED, me, r.getEventId(),
                nameOf(me) + " accepted your request. You're now friends.");
        return person(me, r.getFromUserId());
    }

    public void ignore(Long me, Long requestId) {
        BuddyRequest r = incomingPending(me, requestId);
        r.setStatus(Status.IGNORED);   // the sender is never told
        r.setRespondedAt(LocalDateTime.now());
        requestRepository.save(r);
    }

    public void cancel(Long me, Long requestId) {
        BuddyRequest r = requestRepository.findById(requestId)
                .filter(x -> x.getFromUserId().equals(me) && x.getStatus() == Status.PENDING)
                .orElseThrow(BuddyService::notFound);
        r.setStatus(Status.CANCELLED);
        r.setRespondedAt(LocalDateTime.now());
        requestRepository.save(r);
    }

    // ---------- friends ----------

    @Transactional(readOnly = true)
    public List<FriendView> friends(Long me) {
        Set<Long> hidden = hiddenFrom(me);
        Date now = new Date();
        return people(me, friendIds(me).stream().filter(id -> !hidden.contains(id)).toList()).stream()
                .sorted(Comparator.comparing(Person::name, String.CASE_INSENSITIVE_ORDER))
                .map(p -> new FriendView(p, !sharesAttendance(p.userId()) ? List.of()
                        : eventApi.registeredEvents(p.userId()).stream()
                                .filter(e -> e.eventDate() != null && e.eventDate().after(now))
                                .sorted(Comparator.comparing(EventCard::eventDate))
                                .limit(5).toList()))
                .toList();
    }

    public void removeFriend(Long me, Long other) {
        requestRepository.between(me, other).stream().filter(r -> r.getStatus() == Status.ACCEPTED).forEach(r -> {
            r.setStatus(Status.REMOVED);
            r.setRespondedAt(LocalDateTime.now());
            requestRepository.save(r);
        });
    }

    // ---------- safety ----------

    public void block(Long me, Long other) {
        if (other == null || other.equals(me)) throw bad("Choose another student");
        if (!blockRepository.existsByBlockerIdAndBlockedId(me, other)) blockRepository.save(new UserBlock(me, other));
        removeFriend(me, other);
        requestRepository.between(me, other).stream().filter(r -> r.getStatus() == Status.PENDING).forEach(r -> {
            r.setStatus(r.getFromUserId().equals(me) ? Status.CANCELLED : Status.IGNORED);
            r.setRespondedAt(LocalDateTime.now());
            requestRepository.save(r);
        });
    }

    public void unblock(Long me, Long other) {
        blockRepository.deleteByBlockerIdAndBlockedId(me, other);
    }

    @Transactional(readOnly = true)
    public List<Person> blocked(Long me) {
        List<Long> ids = blockRepository.findByBlockerId(me).stream().map(UserBlock::getBlockedId).toList();
        Map<Long, String> names = namesOf(ids);
        return ids.stream().map(id -> new Person(id, names.get(id), null, null, List.of(), List.of(),
                Relation.NONE, null, null)).toList();
    }

    /** Reporting also blocks, so the reporter is never contacted again by that person. */
    public void report(Long me, Long other, String reason) {
        if (other == null || other.equals(me)) throw bad("Choose another student");
        String text = reason == null ? "" : reason.trim();
        if (text.isEmpty()) throw bad("Tell us what happened");
        if (text.length() > 500) throw bad("The reason can be at most 500 characters");
        reportRepository.save(new UserReport(me, other, text));
        block(me, other);
    }

    @Transactional(readOnly = true)
    public List<ReportView> reports() {
        List<UserReport> all = reportRepository.findAllByOrderByCreatedAtDesc();
        Set<Long> ids = new HashSet<>();
        all.forEach(r -> { ids.add(r.getReporterId()); ids.add(r.getReportedId()); });
        Map<Long, String> names = namesOf(ids);
        return all.stream().map(r -> new ReportView(r.getId(), r.getReporterId(), names.get(r.getReporterId()),
                r.getReportedId(), names.get(r.getReportedId()), r.getReason(),
                r.getStatus().name(), r.getCreatedAt())).toList();
    }

    public void resolveReport(Long reportId) {
        UserReport r = reportRepository.findById(reportId).orElseThrow(BuddyService::notFound);
        r.setStatus(UserReport.Status.RESOLVED);
        r.setResolvedAt(LocalDateTime.now());
        reportRepository.save(r);
    }

    // ---------- notifications ----------

    @Transactional(readOnly = true)
    public List<NotificationView> notifications(Long me) {
        Set<Long> hidden = hiddenFrom(me);
        return notificationRepository.findTop30ByUserIdOrderByCreatedAtDesc(me).stream()
                .filter(n -> n.getActorUserId() == null || !hidden.contains(n.getActorUserId()))
                .map(n -> new NotificationView(n.getId(), n.getType().name(), n.getMessage(), n.getEventId(),
                        n.getActorUserId(), n.isRead(), n.getCreatedAt()))
                .toList();
    }

    public void markNotificationsRead(Long me) {
        notificationRepository.markAllRead(me);
    }

    /** Tells the user's friends that they registered for an event (if they share attendance). */
    public void friendRegistered(Long userId, Long eventId) {
        if (!sharesAttendance(userId)) return;
        Optional<String> event = eventName(eventId);
        if (event.isEmpty()) return;
        Set<Long> hidden = hiddenFrom(userId);
        String message = nameOf(userId) + " is going to " + event.get();
        for (Long friend : friendIds(userId)) {
            if (!hidden.contains(friend)) notify(friend, BuddyNotification.Type.FRIEND_GOING, userId, eventId, message);
        }
    }

    // ---------- helpers ----------

    private boolean canReach(Long me, Long to, Long eventId) {
        if (eventId != null && optInRepository.existsByUserIdAndEventId(me, eventId)
                && optInRepository.existsByUserIdAndEventId(to, eventId)
                && eventApi.isRegistered(me, eventId) && eventApi.isRegistered(to, eventId)) {
            return true;
        }
        boolean meFindable = settingsRepository.findById(me).map(BuddySettings::isDiscoverable).orElse(false);
        boolean toFindable = settingsRepository.findById(to).map(s -> s.isDiscoverable() && s.getDisplayName() != null).orElse(false);
        // Someone who already asked me can always be answered.
        boolean theyAsked = requestRepository.between(me, to).stream()
                .anyMatch(r -> r.getStatus() == Status.PENDING && r.getFromUserId().equals(to));
        return (meFindable && toFindable) || theyAsked;
    }

    private BuddyRequest incomingPending(Long me, Long requestId) {
        return requestRepository.findById(requestId)
                .filter(r -> r.getToUserId().equals(me) && r.getStatus() == Status.PENDING)
                .filter(r -> !hiddenFrom(me).contains(r.getFromUserId()))
                .orElseThrow(BuddyService::notFound);
    }

    private List<Long> friendIds(Long me) {
        return requestRepository.involving(me, List.of(Status.ACCEPTED)).stream()
                .map(r -> r.otherThan(me)).distinct().toList();
    }

    private Set<Long> hiddenFrom(Long me) {
        return new HashSet<>(blockRepository.hiddenFrom(me));
    }

    private boolean sharesAttendance(Long userId) {
        return settingsRepository.findById(userId).map(BuddySettings::isShareAttendance).orElse(true);
    }

    private Person person(Long me, Long other) {
        return people(me, List.of(other)).stream().findFirst().orElseThrow(BuddyService::notFound);
    }

    private List<Person> people(Long me, Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        Set<Long> withMe = new HashSet<>(ids);
        withMe.add(me);
        Map<Long, ProfileApi.PublicProfile> profiles = profileApi.publicProfiles(withMe);
        Map<Long, BuddySettings> settings = settingsRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(BuddySettings::getUserId, Function.identity()));
        Set<String> myInterests = Optional.ofNullable(profiles.get(me)).map(p -> new HashSet<>(p.interests()))
                .orElseGet(HashSet::new);
        List<BuddyRequest> mine = requestRepository.involving(me, List.of(Status.PENDING, Status.IGNORED, Status.ACCEPTED));

        List<Person> result = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(ids)) {
            ProfileApi.PublicProfile p = profiles.get(id);
            BuddySettings s = settings.get(id);
            List<String> interests = p == null ? List.of() : p.interests();
            Relation relation = Relation.NONE;
            Long requestId = null;
            for (BuddyRequest r : mine) {
                if (!r.involves(id)) continue;
                if (r.getStatus() == Status.ACCEPTED) { relation = Relation.FRIENDS; requestId = r.getId(); break; }
                if (r.getFromUserId().equals(me)) { relation = Relation.REQUESTED; requestId = r.getId(); }
                else if (r.getStatus() == Status.PENDING) { relation = Relation.INCOMING; requestId = r.getId(); }
            }
            result.add(new Person(id, s != null && s.getDisplayName() != null ? s.getDisplayName() : displayName(id, p),
                    p == null ? null : p.department(), p == null ? null : p.yearOfStudy(), interests,
                    interests.stream().filter(myInterests::contains).toList(), relation,
                    relation == Relation.FRIENDS ? null : requestId,
                    relation == Relation.FRIENDS && s != null ? s.getContact() : null));
        }
        return result;
    }

    /** The name each user chose for Friends, else their profile name. */
    private Map<Long, String> namesOf(Collection<Long> ids) {
        Map<Long, ProfileApi.PublicProfile> profiles = profileApi.publicProfiles(ids);
        Map<Long, String> chosen = settingsRepository.findAllById(ids).stream()
                .filter(s -> s.getDisplayName() != null)
                .collect(Collectors.toMap(BuddySettings::getUserId, BuddySettings::getDisplayName));
        Map<Long, String> names = new HashMap<>();
        for (Long id : ids) names.put(id, chosen.getOrDefault(id, displayName(id, profiles.get(id))));
        return names;
    }

    private String nameOf(Long userId) {
        return settingsRepository.findById(userId).map(BuddySettings::getDisplayName)
                .orElseGet(() -> displayName(userId, profileApi.publicProfiles(List.of(userId)).get(userId)));
    }

    private static String displayName(Long userId, ProfileApi.PublicProfile p) {
        return p != null && p.name() != null ? p.name() : "Student #" + userId;
    }

    private void requireName(Long me) {
        if (settingsRepository.findById(me).map(BuddySettings::getDisplayName).isEmpty()) {
            throw bad("Set the name other students will see first (Friends → Settings)");
        }
    }

    private Optional<String> eventName(Long eventId) {
        return eventId == null ? Optional.empty() : eventApi.eventProfile(eventId).map(p -> p.card().name());
    }

    private void notify(Long userId, BuddyNotification.Type type, Long actor, Long eventId, String message) {
        String text = message.length() > 255 ? message.substring(0, 252) + "..." : message;
        notificationRepository.save(new BuddyNotification(userId, type, actor, eventId, text));
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "That student or request isn't available");
    }
}
