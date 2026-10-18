package com.event.sharedmodule;

import java.util.List;

/** An organizer marked an event completed; {@code attendeeIds} are the students who checked in. */
public record EventCompletedEvent(Long eventId, String eventName, List<Long> attendeeIds) {
}
