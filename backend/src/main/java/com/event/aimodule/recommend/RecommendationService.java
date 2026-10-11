package com.event.aimodule.recommend;

import com.event.aimodule.AiApi;
import com.event.eventmodule.api.EventApi;
import com.event.profilemodule.communication.ProfileApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Not cached on purpose: it is a handful of cheap queries, and a cache would keep suggesting an event
 * for a while after the student registered for it.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService implements AiApi {

    private final EventApi eventApi;
    private final ProfileApi profileApi;
    private final RecommendationScorer scorer = new RecommendationScorer();

    @Override
    public List<RecommendedEvent> recommendationsFor(Long userId, int limit) {
        return scorer.rank(
                eventApi.upcomingOpenEvents(),
                new StudentSignals(profileApi.interestNames(userId),
                        profileApi.department(userId).orElse(null),
                        eventApi.registeredEvents(userId)),
                Instant.now(),
                Math.max(1, Math.min(limit, 20)));
    }
}
