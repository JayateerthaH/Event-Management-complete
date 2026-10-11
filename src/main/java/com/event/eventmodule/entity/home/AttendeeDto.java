package com.event.eventmodule.entity.home;

import com.event.eventmodule.Registration.Register;

import java.util.Date;

/** One registration for an organizer's event, as listed on the organizer dashboard. */
public record AttendeeDto(Long id, String name, String email, Long eventId, String eventTitle,
                          Date date, boolean attended) {
    public static AttendeeDto from(Register r) {
        return new AttendeeDto(r.getId(), r.getName(), r.getEmail(), r.getEventsDto().getId(),
                r.getEventsDto().getName(), r.getEventsDto().getEventDate(), Boolean.TRUE.equals(r.getIsAttended()));
    }
}
