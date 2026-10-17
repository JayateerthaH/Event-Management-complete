package com.event.socialmodule.service;

import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.SeatEvents;
import com.event.socialmodule.entity.BuddyNotification;
import com.event.socialmodule.repository.BuddyNotificationRepository;
import com.event.socialmodule.repository.BuddyOptInRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/** Keeps Event buddies and notifications in step with seats being released and given to the waitlist. */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeatListener {

    private final BuddyOptInRepository optInRepository;
    private final BuddyNotificationRepository notificationRepository;
    private final EventApi eventApi;

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onReleased(SeatEvents.SeatReleased e) {
        optInRepository.deleteByUserIdAndEventId(e.userId(), e.eventId());
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPromoted(SeatEvents.PromotedFromWaitlist e) {
        try {
            String event = eventApi.eventProfile(e.eventId()).map(p -> p.card().name()).orElse("an event");
            String message = "A seat opened: you're now registered for " + event + ". Your QR ticket is in My tickets and your email.";
            notificationRepository.save(new BuddyNotification(e.userId(), BuddyNotification.Type.SEAT_OPENED, null, e.eventId(),
                    message.length() > 255 ? message.substring(0, 252) + "..." : message));
        } catch (RuntimeException ex) {
            log.warn("Could not tell user {} about their seat at event {}: {}", e.userId(), e.eventId(), ex.getMessage());
        }
    }
}
