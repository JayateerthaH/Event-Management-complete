package com.event.aimodule.recommend;

import com.event.eventmodule.api.EventCard;

import java.util.List;

/** What we know about a student when ranking events. */
public record StudentSignals(List<String> interests, String department, List<EventCard> history) {
}
