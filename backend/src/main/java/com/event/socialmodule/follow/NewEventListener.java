package com.event.socialmodule.follow;

import com.event.eventmodule.api.EventApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/** "New from <club>": runs after the approval is committed, off the request thread. */
@Component
@RequiredArgsConstructor
@Slf4j
public class NewEventListener {

    private final FollowService followService;

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onApproved(EventApprovedEvent e) {
        try {
            followService.eventApproved(e.eventId(), e.organizerId());
        } catch (RuntimeException ex) {
            log.warn("Could not notify followers about event {}: {}", e.eventId(), ex.getMessage());
        }
    }
}
