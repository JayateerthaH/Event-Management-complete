package com.event.eventmodule.entity.home;

import com.event.eventmodule.entity.Events;

import java.util.Date;

/** An organizer's own event in the dashboard: the public summary plus its registration count. */
public record OrganizerEventDto(
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
        Boolean isApproved,
        Boolean isCompleted,
        long registrations
) {
    public static OrganizerEventDto from(Events e, long registrations) {
        return new OrganizerEventDto(e.getId(), e.getName(), e.getDescription(), e.getCategory(),
                e.getEventField(), e.getEventDate(), e.getVenue(), e.getMaxPeople(),
                e.getCostOfRegistration(), e.getImageUrl(), e.getIsApproved(),
                Boolean.TRUE.equals(e.getIsCompleted()), registrations);
    }
}
