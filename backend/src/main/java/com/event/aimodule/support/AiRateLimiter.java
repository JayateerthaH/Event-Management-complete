package com.event.aimodule.support;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window limiter per (user, feature): LLM calls cost money, so each user gets a fixed budget per window.
 * In-memory, so limits are per app instance.
 */
@Component
public class AiRateLimiter {

    private final Map<String, Deque<Instant>> calls = new ConcurrentHashMap<>();
    private final Clock clock;

    public AiRateLimiter() {
        this(Clock.systemUTC());
    }

    AiRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void check(String feature, String userId, int maxCalls, Duration window) {
        Instant now = clock.instant();
        Deque<Instant> history = calls.computeIfAbsent(feature + ":" + userId, k -> new ArrayDeque<>());
        synchronized (history) {
            while (!history.isEmpty() && !history.peekFirst().isAfter(now.minus(window))) {
                history.pollFirst();
            }
            if (history.size() >= maxCalls) {
                long wait = Duration.between(now, history.peekFirst().plus(window)).toSeconds() + 1;
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many AI requests. Please try again in " + wait + " seconds.");
            }
            history.addLast(now);
        }
    }
}
