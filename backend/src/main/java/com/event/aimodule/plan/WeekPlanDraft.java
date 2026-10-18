package com.event.aimodule.plan;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** What the LLM returns for "plan my week": picks only from the candidate ids it was given. */
public record WeekPlanDraft(
        @JsonPropertyDescription("Two or three friendly sentences addressed to the student summarising their week, plain text.")
        String summary,
        @JsonPropertyDescription("Up to 3 suggested events from the candidates, best first.")
        List<Pick> picks
) {
    public record Pick(
            @JsonPropertyDescription("The candidate's id, exactly as given.")
            Long eventId,
            @JsonPropertyDescription("One short sentence on why it suits this student, under 20 words.")
            String why
    ) {}
}
