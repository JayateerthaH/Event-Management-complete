package com.event.analyticsmodule.engagement;

import com.event.eventmodule.api.EventRegisteredEvent;
import com.event.sharedmodule.UserActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records activity after the action commits, off the request thread. Analytics must never break or
 * slow down the action itself, so failures are only logged.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityRecorder {

    private final ActivityEventRepository repository;
    /** "active" is recorded once per user per day (per server; a restart may add one extra row). */
    private final Map<Long, LocalDate> activeToday = new ConcurrentHashMap<>();

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void on(UserActivityEvent e) {
        save(e.userId(), e.type(), e.eventId());
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void on(EventRegisteredEvent e) {
        save(e.userId(), "event_registered", e.eventId());
    }

    /** Called for every authenticated request; writes at most once per user per day. */
    public void markActive(Long userId) {
        if (userId == null) return;
        LocalDate today = LocalDate.now();
        if (today.equals(activeToday.put(userId, today))) return;
        save(userId, "active", null);
        if (activeToday.size() > 50_000) activeToday.clear();   // bounded memory
    }

    private void save(Long userId, String type, Long eventId) {
        try {
            repository.save(new ActivityEvent(userId, type, eventId));
        } catch (RuntimeException ex) {
            log.warn("Could not record activity {} for user {}: {}", type, userId, ex.getMessage());
        }
    }
}
