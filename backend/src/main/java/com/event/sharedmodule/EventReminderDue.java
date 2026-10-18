package com.event.sharedmodule;

/** A registered student's event is coming up ("tomorrow" or "in about an hour"); published by the event module. */
public record EventReminderDue(Long userId, Long eventId, String eventName, String when) {
}
