package com.event.aimodule.assistant;

import java.util.List;

/** What the organizer has typed so far plus a few key points. */
public record DescriptionRequest(String title, String category, String eventDate, String venue, String cost,
                                 List<String> keyPoints) {
}
