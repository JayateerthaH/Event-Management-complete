package com.event.socialmodule.nudge;

import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventCard;
import com.event.eventmodule.api.EventRegisteredEvent;
import com.event.socialmodule.entity.BuddyNotification;
import com.event.socialmodule.entity.BuddyNotification.Type;
import com.event.socialmodule.repository.BuddyNotificationRepository;
import com.event.socialmodule.settings.NotificationPrefsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Saved ("Interested") events turn into attendance: a nudge the day before a saved event the student hasn't
 * registered for, and an alert when a saved event is almost full. Each is sent at most once per student per event.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SavedEventNudges {

    static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    static final double ALMOST_FULL = 0.8;

    private final EventApi eventApi;
    private final BuddyNotificationRepository notifications;
    private final NotificationPrefsService prefs;

    /** Every morning: saved events happening tomorrow. */
    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void tomorrowsSavedEvents() {
        LocalDate tomorrow = LocalDate.now(ZONE).plusDays(1);
        int sent = 0;
        for (EventCard e : eventApi.upcomingOpenEvents()) {
            if (e.eventDate() == null || !Instant.ofEpochMilli(e.eventDate().getTime()).atZone(ZONE).toLocalDate().equals(tomorrow)) continue;
            String message = e.name() + " is tomorrow and you saved it." + (e.isFull() ? "" : " Register while seats are left.");
            for (Long student : eventApi.saversOf(e.id())) {
                if (!eventApi.isRegistered(student, e.id()) && notify(student, Type.SAVED_SOON, e.id(), message)) sent++;
            }
        }
        if (sent > 0) log.info("Sent {} saved-event nudges", sent);
    }

    /** After a registration: when an event first reaches 80% full, tell students who saved it but haven't registered. */
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRegistered(EventRegisteredEvent r) {
        eventApi.eventProfile(r.eventId()).map(p -> p.card()).filter(c -> almostFull(c.maxPeople(), c.registeredCount())).ifPresent(c -> {
            long left = c.maxPeople() - c.registeredCount();
            String message = c.name() + " is almost full: " + left + (left == 1 ? " seat" : " seats") + " left.";
            for (Long student : eventApi.saversOf(c.id())) {
                if (!student.equals(r.userId()) && !eventApi.isRegistered(student, c.id())) notify(student, Type.ALMOST_FULL, c.id(), message);
            }
        });
    }

    static boolean almostFull(Long max, long registered) {
        return max != null && max > 0 && registered < max && registered >= Math.ceil(max * ALMOST_FULL);
    }

    private boolean notify(Long student, Type type, Long eventId, String message) {
        if (notifications.existsByUserIdAndTypeAndEventId(student, type, eventId) || !prefs.allows(student, type)) return false;
        notifications.save(new BuddyNotification(student, type, null, eventId, message));
        return true;
    }
}
