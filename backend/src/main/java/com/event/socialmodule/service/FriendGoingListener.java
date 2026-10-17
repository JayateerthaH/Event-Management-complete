package com.event.socialmodule.service;

import com.event.eventmodule.api.EventRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/** "Your friend is going": runs after the registration is committed, off the request thread. */
@Component
@RequiredArgsConstructor
@Slf4j
public class FriendGoingListener {

    private final BuddyService buddyService;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRegistered(EventRegisteredEvent event) {
        try {
            buddyService.friendRegistered(event.userId(), event.eventId());
        } catch (RuntimeException e) {
            log.warn("Could not notify friends of user {} about event {}: {}", event.userId(), event.eventId(), e.getMessage());
        }
    }
}
