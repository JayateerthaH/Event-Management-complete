package com.event.socialmodule.recap;

import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventProfile;
import com.event.eventmodule.api.FeedbackEntry;
import com.event.sharedmodule.EventCompletedEvent;
import com.event.socialmodule.entity.BuddyNotification;
import com.event.socialmodule.follow.OrganizerFollow;
import com.event.socialmodule.follow.OrganizerFollowRepository;
import com.event.socialmodule.repository.BuddyNotificationRepository;
import com.event.socialmodule.settings.NotificationPrefsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Post-event recap: attendees are thanked (with feedback, certificate and follow prompts) when the organizer
 * completes the event; organizers see how it went and how many followers it brought.
 */
@Service
@RequiredArgsConstructor
public class EventRecapService {

    private final EventApi eventApi;
    private final BuddyNotificationRepository notificationRepository;
    private final NotificationPrefsService prefs;
    private final OrganizerFollowRepository followRepository;

    public record Recap(boolean available, long registered, long checkedIn, Double showUpRate, int feedbackCount,
                       Double averageRating, long followers, long newFollowersSinceEvent) {}

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void thankAttendees(EventCompletedEvent e) {
        if (e.attendeeIds() == null) return;
        String club = eventApi.eventProfile(e.eventId()).map(EventProfile::organizedBy).orElse(null);
        String message = "Thanks for coming to " + e.eventName() + "! Tell the organizers how it went, check for your certificate"
                + (club == null ? ", and see what's next." : ", and follow " + club + " for their next event.");
        if (message.length() > 255) message = message.substring(0, 252) + "...";
        for (Long student : e.attendeeIds().stream().distinct().toList()) {
            if (prefs.allows(student, BuddyNotification.Type.EVENT_RECAP)) {
                notificationRepository.save(new BuddyNotification(student, BuddyNotification.Type.EVENT_RECAP, null, e.eventId(), message));
            }
        }
    }

    /** For the event's managers (callers check permission). Empty until the event has started. */
    @Transactional(readOnly = true)
    public Recap recap(Long eventId) {
        EventProfile event = eventApi.eventProfile(eventId).orElseThrow();
        var date = event.card().eventDate();
        if (date == null || date.getTime() > System.currentTimeMillis()) return new Recap(false, 0, 0, null, 0, null, 0, 0);
        long registered = event.card().registeredCount();
        long checkedIn = eventApi.checkedInCount(eventId);
        List<FeedbackEntry> feedback = eventApi.feedbackForEvent(eventId);
        Double rating = feedback.stream().filter(f -> f.stars() > 0).mapToLong(FeedbackEntry::stars).average().stream().boxed().findFirst().orElse(null);
        Long organizer = eventApi.organizerOf(eventId).orElse(null);
        List<OrganizerFollow> follows = organizer == null ? List.of() : followRepository.findByOrganizerId(organizer);
        LocalDateTime since = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
        long newFollowers = follows.stream().filter(f -> f.getCreatedAt() != null && f.getCreatedAt().isAfter(since)).count();
        return new Recap(true, registered, checkedIn, registered == 0 ? null : (double) checkedIn / registered,
                feedback.size(), rating, follows.size(), newFollowers);
    }
}
