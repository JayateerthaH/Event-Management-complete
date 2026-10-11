package com.event.aimodule.recommend;

import com.event.eventmodule.api.EventCard;

import java.util.List;

/** An event suggestion with its score and the human-readable reasons behind it. */
public record RecommendedEvent(EventCard event, double score, List<String> reasons) {
}
