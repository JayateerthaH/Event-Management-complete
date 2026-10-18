package com.event.socialmodule.push;

import com.event.socialmodule.entity.BuddyNotification;
import jakarta.persistence.PostPersist;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Every saved bell notification is also a push candidate, wherever in the code it was created. */
@Component
public class BellNotificationEntityListener {

    private final ApplicationEventPublisher publisher;

    public BellNotificationEntityListener(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostPersist
    void created(BuddyNotification n) {
        publisher.publishEvent(new BellNotificationCreated(n.getUserId(), n.getType(), n.getEventId(), n.getMessage()));
    }
}
