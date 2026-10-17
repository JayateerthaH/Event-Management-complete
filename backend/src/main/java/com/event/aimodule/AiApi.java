package com.event.aimodule;

import com.event.aimodule.recommend.RecommendedEvent;

import java.util.List;

/** Public entry point of the AI module for other modules. */
public interface AiApi {

    /** Ranked event suggestions for a student, with reasons. */
    List<RecommendedEvent> recommendationsFor(Long userId, int limit);

    /** The same ranking over candidates the caller already loaded (e.g. one batch job for many students). */
    List<RecommendedEvent> recommendationsFor(Long userId, int limit, List<com.event.eventmodule.api.EventCard> candidates);
}
