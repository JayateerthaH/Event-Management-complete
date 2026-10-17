package com.event.eventmodule.api;

/** Published when an admin approves an event (it becomes visible to students). */
public record EventApprovedEvent(Long eventId, Long organizerId) {
}
