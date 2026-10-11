package com.event.aimodule.feedback;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record FeedbackInsights(
        Sentiment sentiment,
        @JsonPropertyDescription("2-3 sentence overview for the organizer.")
        String summary,
        @JsonPropertyDescription("Up to 4 things attendees liked.")
        List<String> highlights,
        @JsonPropertyDescription("Up to 4 concrete, actionable improvements.")
        List<String> improvements
) {
    public enum Sentiment { POSITIVE, MIXED, NEGATIVE }
}
