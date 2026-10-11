package com.event.eventmodule.api;

import com.event.eventmodule.dtoPayload.EventsDto;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.analysis.MonthlyAttendanceCount;

import java.util.List;
import java.util.Optional;

public interface EventApi {
    List<EventsDto> getPendingEvents(Long userId);
    List<EventsDto> getPastEvents(Long userId);
    List<MonthlyAttendanceCount> getAttendanceCount(Long userId);
    List<MonthlyAttendanceCount> getRegisteredCount(Long userId);
    void markAttendance(Long userId, Long eventId);
    boolean canScan(Long eventId, String email, Long userId);
    long countAllEvents();

    // ── Read model for the AI module (approved events only, no attendee data) ──

    /** Approved events that have not started yet and are not completed. */
    List<EventCard> upcomingOpenEvents();

    /** Events the user has registered for (past and upcoming). */
    List<EventCard> registeredEvents(Long userId);

    /** Approved events matching the criteria, soonest first, at most {@code limit}. */
    List<EventCard> searchEvents(EventSearchCriteria criteria, int limit);

    /** Distinct categories of approved events. */
    List<String> eventCategories();

    /** Same rule as event management endpoints: creator, collaborator or admin. */
    boolean canManageEvent(Long eventId, String userId, String roles, String email);

    List<FeedbackEntry> feedbackForEvent(Long eventId);

    /** Public page content of any event (approved or not; callers check {@link EventProfile#approved()}). */
    Optional<EventProfile> eventProfile(Long eventId);
}