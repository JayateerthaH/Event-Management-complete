package com.event.eventmodule.api;

/**
 * Structured event search, the same filters the Events page offers.
 * dateRange: TODAY | TOMORROW | THIS_WEEK | THIS_MONTH; costType: FREE | PAID; nulls are ignored.
 */
public record EventSearchCriteria(
        String keywords,
        String category,
        String venue,
        String department,
        String dateRange,
        String costType
) {
}
