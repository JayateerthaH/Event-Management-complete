package com.event.socialmodule.follow;

import com.event.authmodule.communication.OrganizerDirectory;
import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventCard;
import com.event.sharedmodule.UserActivityEvent;
import com.event.socialmodule.entity.BuddyNotification;
import com.event.socialmodule.repository.BuddyNotificationRepository;
import com.event.socialmodule.settings.NotificationPrefsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Follow organizers (clubs, departments) and hear when they publish a new event. */
@Service
@RequiredArgsConstructor
@Transactional
public class FollowService {

    static final int MAX_FOLLOWING = 100;

    private final OrganizerFollowRepository followRepository;
    private final OrganizerDirectory organizerDirectory;
    private final EventApi eventApi;
    private final BuddyNotificationRepository notificationRepository;
    private final NotificationPrefsService prefs;
    private final ApplicationEventPublisher activityPublisher;

    public record OrganizerView(Long organizerId, String name, long followers, boolean following,
                                int upcomingCount, List<EventCard> upcoming) {}

    @Transactional(readOnly = true)
    public OrganizerView forEvent(Long me, Long eventId) {
        Long organizerId = eventApi.organizerOf(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        return view(me, organizerId, false);
    }

    public OrganizerView follow(Long me, Long organizerId) {
        if (organizerId == null || organizerId.equals(me)) throw bad("You can't follow yourself");
        if (!organizerDirectory.isOrganizer(organizerId) && eventApi.upcomingByOrganizer(organizerId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "That organizer isn't available");
        }
        if (!followRepository.existsByFollowerIdAndOrganizerId(me, organizerId)) {
            if (followRepository.findByFollowerIdOrderByCreatedAtDesc(me).size() >= MAX_FOLLOWING) {
                throw bad("You can follow up to " + MAX_FOLLOWING + " organizers");
            }
            followRepository.save(new OrganizerFollow(me, organizerId));
            activityPublisher.publishEvent(new UserActivityEvent(me, "organizer_followed", null));
        }
        return view(me, organizerId, false);
    }

    public OrganizerView unfollow(Long me, Long organizerId) {
        followRepository.deleteByFollowerIdAndOrganizerId(me, organizerId);
        return view(me, organizerId, false);
    }

    @Transactional(readOnly = true)
    public List<OrganizerView> following(Long me) {
        return followRepository.findByFollowerIdOrderByCreatedAtDesc(me).stream()
                .map(f -> view(me, f.getOrganizerId(), true)).toList();
    }

    /** Tells the organizer's followers about a newly approved event. */
    public void eventApproved(Long eventId, Long organizerId) {
        if (organizerId == null) return;
        String event = eventApi.eventProfile(eventId).map(p -> p.card().name()).orElse(null);
        if (event == null) return;
        String message = "New from " + nameOf(organizerId) + ": " + event;
        if (message.length() > 255) message = message.substring(0, 252) + "...";
        for (OrganizerFollow f : followRepository.findByOrganizerId(organizerId)) {
            if (prefs.allows(f.getFollowerId(), BuddyNotification.Type.NEW_EVENT)) {
                notificationRepository.save(new BuddyNotification(f.getFollowerId(), BuddyNotification.Type.NEW_EVENT,
                        organizerId, eventId, message));
            }
        }
    }

    private OrganizerView view(Long me, Long organizerId, boolean withEvents) {
        List<EventCard> upcoming = eventApi.upcomingByOrganizer(organizerId);
        return new OrganizerView(organizerId, nameOf(organizerId), followRepository.countByOrganizerId(organizerId),
                followRepository.existsByFollowerIdAndOrganizerId(me, organizerId), upcoming.size(),
                withEvents ? upcoming.stream().limit(3).toList() : List.of());
    }

    /** The club named on their events ("Organized by"), else the organizer account's name. */
    private String nameOf(Long organizerId) {
        return eventApi.latestOrganizedBy(organizerId)
                .or(() -> java.util.Optional.ofNullable(organizerDirectory.names(List.of(organizerId)).get(organizerId)))
                .orElse("Organizer #" + organizerId);
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
