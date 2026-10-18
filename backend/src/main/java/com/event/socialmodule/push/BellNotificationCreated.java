package com.event.socialmodule.push;

import com.event.socialmodule.entity.BuddyNotification;

/** A bell notification was saved; pushed to the user's devices once the transaction commits. */
public record BellNotificationCreated(Long userId, BuddyNotification.Type type, Long eventId, String message) {
}
