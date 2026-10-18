package com.event.socialmodule.push;

import com.event.sharedmodule.EventReminderDue;
import com.event.socialmodule.entity.BuddyNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Turns bell notifications and event reminders into push messages. */
@Component
@RequiredArgsConstructor
public class PushListeners {

    private final PushService pushService;

    @TransactionalEventListener(fallbackExecution = true)
    public void onBell(BellNotificationCreated n) {
        pushService.send(n.userId(), message(n), urgent(n.type()));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onReminder(EventReminderDue r) {
        pushService.send(r.userId(), new PushMessage(r.eventName() + " is " + r.when(),
                "Your QR ticket is ready in My tickets.", "/tickets", "reminder-" + r.eventId()), true);
    }

    /** Time-sensitive ones skip quiet hours and the weekly cap. */
    static boolean urgent(BuddyNotification.Type type) {
        return switch (type) {
            case BUDDY_REQUEST, BUDDY_ACCEPTED, SEAT_OPENED, ALMOST_FULL -> true;
            case FRIEND_GOING, ACHIEVEMENT, NEW_EVENT, DIGEST, EVENT_RECAP, SAVED_SOON, FEEDBACK_ASK -> false;
        };
    }

    static PushMessage message(BellNotificationCreated n) {
        String title = switch (n.type()) {
            case BUDDY_REQUEST -> "New friend request";
            case BUDDY_ACCEPTED -> "You have a new friend";
            case FRIEND_GOING -> "A friend is going";
            case ACHIEVEMENT -> "Nice work";
            case NEW_EVENT -> "New event for you";
            case DIGEST -> "Your week on campus";
            case SEAT_OPENED -> "You got a seat";
            case EVENT_RECAP -> "Thanks for coming";
            case SAVED_SOON -> "Tomorrow: an event you saved";
            case ALMOST_FULL -> "Almost full";
            case FEEDBACK_ASK -> "How was it?";
        };
        String url = n.type() == BuddyNotification.Type.FEEDBACK_ASK ? "/dashboard?tab=feedback"
                : n.eventId() != null ? "/events/" + n.eventId()
                : switch (n.type()) {
                    case BUDDY_REQUEST -> "/friends?tab=requests";
                    case BUDDY_ACCEPTED -> "/friends";
                    default -> "/dashboard";
                };
        return new PushMessage(title, n.message(), url, n.type().name().toLowerCase() + (n.eventId() == null ? "" : "-" + n.eventId()));
    }
}
