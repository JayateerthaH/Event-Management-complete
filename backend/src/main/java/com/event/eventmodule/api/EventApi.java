package com.event.eventmodule.api;

import com.event.eventmodule.dtoPayload.EventsDto;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.analysis.MonthlyAttendanceCount;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
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

    /** Who created the event (the organizer students can follow). */
    Optional<Long> organizerOf(Long eventId);

    /** Approved upcoming events by this organizer, soonest first. */
    List<EventCard> upcomingByOrganizer(Long organizerId);

    /** The club/department named on the organizer's most recent event ("Organized by"), if any. */
    Optional<String> latestOrganizedBy(Long organizerId);

    /** What a student has done so far, for achievements. */
    record Participation(List<EventCard> attended, int registrations, long feedbackGiven, int certificates) {}

    Participation participation(Long userId);

    /** A QR-verified attendance for the student portfolio, with the host club and whether a certificate exists. */
    record AttendedEvent(EventCard card, String organizedBy, boolean certificate) {}

    /** Events the student attended (QR-verified), most recent first. */
    List<AttendedEvent> attendedEvents(Long userId);

    /** Approved events the user saved ("Interested"), most recently saved first. */
    List<EventCard> savedEvents(Long userId);

    /** Other students from this department registered for the event (a count only; no names). */
    long registeredFromDepartment(Long eventId, String department, Long excludeUserId);

    /** The department the student gave on their most recent registration form, if any. */
    Optional<String> lastRegistrationDepartment(Long userId);

    /** Students who attended (QR-verified) an earlier event by this event's organizer and haven't registered for it. */
    List<Long> pastAttendeesToInvite(Long eventId);

    /** Checked-in events per student for events dated in [from, to), for campus-wide comparisons (counts only). */
    Map<Long, Long> attendedCountsBetween(java.util.Date from, java.util.Date to);

    /** Students checked in (QR-scanned) at the event. */
    long checkedInCount(Long eventId);

    /** Whether the user has registered for the event. */
    boolean isRegistered(Long userId, Long eventId);

    /** Number of events per month (by event date) from {@code from} to {@code to}, inclusive; months without events are 0. */
    Map<YearMonth, Long> eventsPerMonth(YearMonth from, YearMonth to);
}