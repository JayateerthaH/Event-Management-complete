package com.event.aimodule.assistant;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record DescriptionDraft(
        @JsonPropertyDescription("Engaging event description, 80-150 words, plain text without markdown.")
        String description,
        @JsonPropertyDescription("3 to 5 frequently asked questions with short answers.")
        List<FaqDraft> faqs
) {
    public record FaqDraft(String question, String answer) {}
}
