package com.event.socialmodule.digest;

import com.event.aimodule.AiApi;
import com.event.authmodule.communication.UserDirectory;
import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventCard;
import com.event.notificationmodule.NotificationApi;
import com.event.socialmodule.entity.BuddyNotification;
import com.event.socialmodule.entity.BuddyRequest;
import com.event.socialmodule.repository.BuddyNotificationRepository;
import com.event.socialmodule.repository.BuddyRequestRepository;
import com.event.socialmodule.settings.NotificationPrefsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * The weekly digest: in the bell for everyone who keeps it on, and by email only for those who opted in
 * (capped well below Brevo's free 300 emails a day). Students with nothing this week get nothing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DigestService {

    static final int MAX_EMAILS_PER_WEEK = 250;
    static final int MAX_STUDENTS_PER_RUN = 5000;

    private final UserDirectory userDirectory;
    private final EventApi eventApi;
    private final AiApi aiApi;
    private final BuddyRequestRepository requestRepository;
    private final BuddyNotificationRepository notificationRepository;
    private final NotificationPrefsService prefs;
    private final DigestSentRepository sentRepository;
    private final NotificationApi notificationApi;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public record RunResult(LocalDate weekStart, int students, int sent, int emailed, int skippedNothingNew,
                            int skippedAlreadySent, int skippedTurnedOff) {}

    public RunResult runWeekly() {
        LocalDate weekStart = LocalDate.now(DigestBuilder.IST).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Instant now = Instant.now(), weekEnd = now.plus(7, ChronoUnit.DAYS);
        long emailed = sentRepository.countByWeekStartAndEmailedTrue(weekStart);
        int sent = 0, mailed = 0, nothing = 0, already = 0, off = 0;
        List<Long> students = userDirectory.studentIds();
        // Loaded once for everyone: open events happening in the next 7 days
        List<EventCard> candidates = eventApi.upcomingOpenEvents().stream().filter(e -> inWeek(e, now, weekEnd)).toList();
        for (Long userId : students.subList(0, Math.min(MAX_STUDENTS_PER_RUN, students.size()))) {
            try {
                if (sentRepository.existsByUserIdAndWeekStart(userId, weekStart)) { already++; continue; }
                if (!prefs.allows(userId, BuddyNotification.Type.DIGEST)) { off++; continue; }
                DigestBuilder.Week week = weekFor(userId, now, weekEnd, candidates);
                Optional<String> message = DigestBuilder.message(week);
                if (message.isEmpty()) { nothing++; continue; }
                notificationRepository.save(new BuddyNotification(userId, BuddyNotification.Type.DIGEST, null, null, message.get()));
                boolean email = false;
                if (prefs.get(userId).digestEmail() && emailed < MAX_EMAILS_PER_WEEK) {
                    String to = userDirectory.emails(List.of(userId)).get(userId);
                    if (to != null) {
                        notificationApi.sendEmail(to, "Your week on campus", DigestBuilder.emailHtml(week, frontendUrl.replaceAll("/+$", "")));
                        email = true;
                        emailed++;
                        mailed++;
                    }
                }
                sentRepository.save(new DigestSent(userId, weekStart, email));
                sent++;
            } catch (RuntimeException e) {
                log.warn("Weekly digest failed for user {}: {}", userId, e.getMessage());
            }
        }
        RunResult result = new RunResult(weekStart, students.size(), sent, mailed, nothing, already, off);
        log.info("Weekly digest: {}", result);
        return result;
    }

    DigestBuilder.Week weekFor(Long userId, Instant from, Instant to, List<EventCard> candidates) {
        List<EventCard> going = thisWeek(eventApi.registeredEvents(userId), from, to);
        Set<Long> goingIds = new HashSet<>();
        going.forEach(e -> goingIds.add(e.id()));
        List<EventCard> saved = thisWeek(eventApi.savedEvents(userId), from, to).stream()
                .filter(e -> !goingIds.contains(e.id())).toList();
        Set<Long> known = new HashSet<>(goingIds);
        saved.forEach(e -> known.add(e.id()));
        List<EventCard> picks = candidates.isEmpty() ? List.of()
                : aiApi.recommendationsFor(userId, 20, candidates).stream().map(r -> r.event())
                        .filter(e -> !known.contains(e.id())).limit(3).toList();

        List<Long> friends = requestRepository.involving(userId, List.of(BuddyRequest.Status.ACCEPTED)).stream()
                .map(r -> r.otherThan(userId)).distinct().toList();
        Map<Long, Integer> friendsGoing = new HashMap<>();
        for (EventCard e : picks) {
            int n = (int) friends.stream().filter(f -> eventApi.isRegistered(f, e.id())).count();
            if (n > 0) friendsGoing.put(e.id(), n);
        }
        return new DigestBuilder.Week(going, saved, picks, friendsGoing);
    }

    private static List<EventCard> thisWeek(List<EventCard> events, Instant from, Instant to) {
        return events.stream().filter(e -> inWeek(e, from, to))
                .sorted(Comparator.comparing(EventCard::eventDate)).toList();
    }

    private static boolean inWeek(EventCard e, Instant from, Instant to) {
        return e.eventDate() != null && e.eventDate().toInstant().isAfter(from) && e.eventDate().toInstant().isBefore(to);
    }
}
