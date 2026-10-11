package com.event.aimodule.assistant;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record EventAnswer(
        @JsonPropertyDescription("Answer in 1-4 short sentences, plain text without markdown.")
        String answer,
        @JsonPropertyDescription("true only if the event information actually contains the answer; "
                + "false when you had to say the information is not available.")
        boolean answeredFromDetails
) {
}
