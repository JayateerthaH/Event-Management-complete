package com.event.aimodule.search;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Structured filters the LLM extracts from a free-text query (JSON schema derived from this record). */
public record SearchIntent(
        @JsonPropertyDescription("1-4 short words that must appear in the event name or description, "
                + "excluding words captured by other fields. Empty string if none.")
        String keywords,
        @JsonPropertyDescription("One of the allowed category names, exactly as written, or empty string.")
        String category,
        @JsonPropertyDescription("Venue or place name only if the query names one, else empty string.")
        String venue,
        @JsonPropertyDescription("Department or field (e.g. CSE, ECE) only if the query names one, else empty string.")
        String department,
        DateRange dateRange,
        CostType costType
) {
    public enum DateRange { ANY, TODAY, TOMORROW, THIS_WEEK, THIS_MONTH }

    public enum CostType { ANY, FREE, PAID }
}
