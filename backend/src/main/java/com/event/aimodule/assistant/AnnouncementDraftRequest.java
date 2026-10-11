package com.event.aimodule.assistant;

/**
 * @param notes what the organizer wants to say, in their own words
 * @param tone  FRIENDLY (default), FORMAL or URGENT
 */
public record AnnouncementDraftRequest(String notes, String tone) {
}
