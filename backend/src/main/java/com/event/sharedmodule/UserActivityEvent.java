package com.event.sharedmodule;

/**
 * Something a user did that's worth counting for engagement analytics (e.g. "event_saved").
 * Published by any module; recorded by the analytics module after the transaction commits.
 */
public record UserActivityEvent(Long userId, String type, Long eventId) {
}
