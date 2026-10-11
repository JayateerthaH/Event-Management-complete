package com.event.eventmodule.api;

import java.util.List;

/**
 * Everything an event page shows publicly, for the AI event assistant and announcement drafts.
 * No attendee data and no organizer contact details.
 */
public record EventProfile(
        EventCard card,
        boolean approved,
        String about,
        String organizedBy,
        String level,
        String language,
        Long durationInHours,
        List<String> departments,
        List<ScheduleItem> schedule,
        List<Faq> faqs,
        List<Speaker> speakers,
        List<String> whatToBring,
        List<String> highlights
) {
    public record ScheduleItem(String time, String title, String description) {}

    public record Faq(String question, String answer) {}

    public record Speaker(String name, String profession, String experience) {}
}
