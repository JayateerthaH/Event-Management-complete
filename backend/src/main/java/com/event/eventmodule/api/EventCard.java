package com.event.eventmodule.api;

import java.util.Date;

/** Public, read-only view of an approved event for other modules (no attendee data). */
public record EventCard(
        Long id,
        String name,
        String description,
        String category,
        String field,
        Date eventDate,
        String venue,
        Long maxPeople,
        String costOfRegistration,
        String imageUrl,
        long registeredCount
) {
    public boolean isFull() {
        return maxPeople != null && registeredCount >= maxPeople;
    }
}
