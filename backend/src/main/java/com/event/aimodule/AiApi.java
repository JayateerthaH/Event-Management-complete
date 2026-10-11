package com.event.aimodule;

import com.event.aimodule.recommend.RecommendedEvent;

import java.util.List;

/** Public entry point of the AI module for other modules. */
public interface AiApi {

    /** Ranked event suggestions for a student, with reasons. */
    List<RecommendedEvent> recommendationsFor(Long userId, int limit);
}
