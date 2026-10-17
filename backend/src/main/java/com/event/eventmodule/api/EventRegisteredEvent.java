package com.event.eventmodule.api;

/**
 * Published after a user registers for an event, for other modules to react to (e.g. telling the user's
 * friends). Listeners should use @TransactionalEventListener so they only see committed registrations.
 */
public record EventRegisteredEvent(Long userId, Long eventId) {
}
