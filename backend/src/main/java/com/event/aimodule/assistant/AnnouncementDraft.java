package com.event.aimodule.assistant;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record AnnouncementDraft(
        @JsonPropertyDescription("Announcement title, under 80 characters.")
        String title,
        @JsonPropertyDescription("Announcement message, 40-120 words, plain text without markdown.")
        String message
) {
}
