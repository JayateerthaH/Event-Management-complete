package com.event.eventmodule.api;

/** Seat changes other modules react to (buddy opt-ins, notifications). */
public final class SeatEvents {
    private SeatEvents() {}

    /** A student released their seat. */
    public record SeatReleased(Long userId, Long eventId) {}

    /** A seat opened and the first student on the waitlist was registered automatically. */
    public record PromotedFromWaitlist(Long userId, Long eventId) {}
}
