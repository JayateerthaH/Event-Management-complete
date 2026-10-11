package com.event.aimodule.search;

import com.event.eventmodule.api.EventCard;
import com.event.eventmodule.api.EventSearchCriteria;

import java.util.List;

/**
 * @param aiUsed  true when the LLM interpreted the query; false for the keyword fallback
 * @param filters the filters actually applied (shown to the user so the interpretation is transparent)
 * @param note    optional explanation, e.g. why the fallback was used
 */
public record AiSearchResponse(String query, boolean aiUsed, EventSearchCriteria filters, String note,
                               List<EventCard> results) {
}
