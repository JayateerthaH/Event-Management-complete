package com.event.socialmodule.invite;

import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventProfile;
import com.event.socialmodule.entity.BuddyNotification;
import com.event.socialmodule.repository.BuddyNotificationRepository;
import com.event.socialmodule.settings.NotificationPrefsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Organizers invite students who checked in at their earlier events to a new one: a bell notification,
 * sent once per event, only to students who allow "new events" notifications.
 */
@RestController
@RequestMapping("/social/events/{eventId}/invite-past-attendees")
@RequiredArgsConstructor
public class InviteController {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH).withZone(ZoneId.of("Asia/Kolkata"));

    private final EventApi eventApi;
    private final EventInviteRepository inviteRepository;
    private final BuddyNotificationRepository notificationRepository;
    private final NotificationPrefsService prefs;

    public record InviteStatus(boolean canInvite, int eligible, LocalDateTime sentAt, Integer recipients, String reason) {}

    @GetMapping
    @Transactional(readOnly = true)
    public InviteStatus status(@PathVariable Long eventId, @RequestAttribute("userId") String userId,
                               @RequestAttribute("userRoles") String roles, @RequestAttribute("username") String email) {
        EventProfile event = manageable(eventId, userId, roles, email);
        var sent = inviteRepository.findById(eventId);
        if (sent.isPresent()) return new InviteStatus(false, 0, sent.get().getSentAt(), sent.get().getRecipients(), "Already sent");
        String reason = notInvitable(event);
        int eligible = reason == null ? recipients(eventId).size() : 0;
        if (reason == null && eligible == 0) reason = "Nobody to invite yet: no one has checked in at your earlier events";
        return new InviteStatus(reason == null, eligible, null, null, reason);
    }

    @PostMapping
    @Transactional
    public InviteStatus send(@PathVariable Long eventId, @RequestAttribute("userId") String userId,
                             @RequestAttribute("userRoles") String roles, @RequestAttribute("username") String email) {
        EventProfile event = manageable(eventId, userId, roles, email);
        if (inviteRepository.existsById(eventId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Past attendees were already invited to this event");
        String reason = notInvitable(event);
        if (reason != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);

        String host = event.organizedBy() != null ? event.organizedBy() : "An organizer whose event you attended";
        String when = event.card().eventDate() == null ? "" : " on " + DAY.format(java.time.Instant.ofEpochMilli(event.card().eventDate().getTime()));
        String message = host + " invites you to " + event.card().name() + when + ".";
        int sent = 0;
        for (Long student : recipients(eventId)) {
            notificationRepository.save(new BuddyNotification(student, BuddyNotification.Type.NEW_EVENT, null, eventId, message));
            sent++;
        }
        EventInvite invite = inviteRepository.save(new EventInvite(eventId, Long.valueOf(userId), sent));
        return new InviteStatus(false, 0, invite.getSentAt(), sent, "Already sent");
    }

    private List<Long> recipients(Long eventId) {
        return eventApi.pastAttendeesToInvite(eventId).stream()
                .filter(id -> prefs.allows(id, BuddyNotification.Type.NEW_EVENT)).toList();
    }

    private EventProfile manageable(Long eventId, String userId, String roles, String email) {
        if (!eventApi.canManageEvent(eventId, userId, roles, email)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return eventApi.eventProfile(eventId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static String notInvitable(EventProfile event) {
        if (!event.approved()) return "Invites open once an admin approves the event";
        var date = event.card().eventDate();
        if (date != null && date.getTime() < System.currentTimeMillis()) return "This event has already started";
        return null;
    }
}
