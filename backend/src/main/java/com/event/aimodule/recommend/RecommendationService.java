package com.event.aimodule.recommend;

import com.event.aimodule.AiApi;
import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventProfile;
import com.event.profilemodule.communication.ProfileApi;
import com.event.sharedmodule.exception.ResourceNotFoundException;
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
    private final SimilarEventsScorer similarScorer = new SimilarEventsScorer();

    @Override
    public List<RecommendedEvent> recommendationsFor(Long userId, int limit) {
        return scorer.rank(
                eventApi.upcomingOpenEvents(),
                new StudentSignals(profileApi.interestNames(userId),
                        profileApi.department(userId).orElse(null),
                        eventApi.registeredEvents(userId),
                        eventApi.savedEvents(userId)),
                Instant.now(),
                Math.max(1, Math.min(limit, 20)));
    }

    /** Upcoming events like the given one; public, so only for approved events. */
    public List<RecommendedEvent> similarTo(Long eventId, int limit) {
        EventProfile target = eventApi.eventProfile(eventId).filter(EventProfile::approved)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        return similarScorer.rank(target.card(), eventApi.upcomingOpenEvents(), Instant.now(), Math.max(1, Math.min(limit, 12)));
    }
}
