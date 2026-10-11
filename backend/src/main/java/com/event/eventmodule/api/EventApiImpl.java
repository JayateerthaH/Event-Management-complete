package com.event.eventmodule.api;

import java.util.Date;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import com.event.eventmodule.repository.FeedBackRepository;
import com.event.eventmodule.filter.EventService;
import com.event.eventmodule.config.EventAccessService;
import com.event.eventmodule.Registration.RegisterRepository;
import com.event.eventmodule.Registration.RegistrationService;
import com.event.eventmodule.dtoPayload.EventsDto;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.analysis.MonthlyAttendanceCount;
import com.event.eventmodule.repository.CollaboratorsRepository;
import com.event.eventmodule.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Public facade of the event module for other modules (analytics, participation).
 * Split out of RegistrationService so that class only handles registrations.
 */
@Service
@RequiredArgsConstructor
public class EventApiImpl implements EventApi {

    private final RegistrationService registrationService;
    private final RegisterRepository registerRepository;
    private final EventsRepository eventsRepository;
    private final CollaboratorsRepository collaboratorsRepository;
    private final EventService eventService;
    private final EventAccessService eventAccessService;
    private final FeedBackRepository feedBackRepository;

    @Override
    public List<EventsDto> getPendingEvents(Long userId) {
        return registrationService.getUpcomingEventsForUser(userId).stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public List<EventsDto> getPastEvents(Long userId) {
        return registrationService.getPastEventsForUser(userId).stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public List<MonthlyAttendanceCount> getAttendanceCount(Long userId) {
        return registerRepository.getUserAttendanceLast6Months(userId);
    }

    @Override
    public List<MonthlyAttendanceCount> getRegisteredCount(Long userId) {
        return registerRepository.getOrganizerAttendanceLast6Months(userId);
    }

    @Override
    public void markAttendance(Long userId, Long eventId) {
        registerRepository.markAttended(userId, eventId);
    }

    @Override
    public boolean canScan(Long eventId, String email, Long userId) {
        if (collaboratorsRepository.existsByEventIdAndEmail(eventId, email)) {
            return true;
        }
        Events event = eventsRepository.findById(eventId).orElse(null);
        return event != null
                && event.getOrganizerId() != null
                && event.getOrganizerId().equals(userId);
    }

    @Override
    public long countAllEvents() {
        return eventsRepository.count();
    }

    @Override
    public List<EventCard> upcomingOpenEvents() {
        return eventsRepository.findByIsApprovedTrueAndEventDateAfterOrderByEventDateAsc(new Date()).stream()
                .filter(e -> !Boolean.TRUE.equals(e.getIsCompleted()))
                .map(this::toCard)
                .toList();
    }

    @Override
    public List<EventCard> registeredEvents(Long userId) {
        return java.util.stream.Stream.concat(
                        registrationService.getUpcomingEventsForUser(userId).stream(),
                        registrationService.getPastEventsForUser(userId).stream())
                .map(this::toCard)
                .toList();
    }

    @Override
    public List<EventCard> searchEvents(EventSearchCriteria c, int limit) {
        return eventService.filterEvents(c.category(), c.keywords(), c.venue(), c.department(), c.dateRange(),
                        c.costType(), PageRequest.of(0, Math.max(1, limit), Sort.by("eventDate").ascending()))
                .getContent().stream()
                .map(s -> new EventCard(s.getId(), s.getName(), s.getDescription(), s.getCategory(), s.getField(),
                        s.getEventDate(), s.getVenue(), s.getMaxPeople(), s.getCostOfRegistration(), s.getImageUrl(),
                        registerRepository.countRegistrationsByEventId(s.getId())))
                .toList();
    }

    @Override
    public List<String> eventCategories() {
        return eventsRepository.findApprovedCategories();
    }

    @Override
    public boolean canManageEvent(Long eventId, String userId, String roles, String email) {
        return eventAccessService.isAllowed(eventId, userId, roles, email);
    }

    @Override
    public List<FeedbackEntry> feedbackForEvent(Long eventId) {
        return feedBackRepository.findAllByEventId(eventId).stream()
                .map(f -> new FeedbackEntry(f.getNoOfStars() == null ? 0 : f.getNoOfStars(), f.getFeedBack()))
                .toList();
    }

    private EventCard toCard(Events e) {
        return new EventCard(e.getId(), e.getName(), e.getDescription(), e.getCategory(), e.getEventField(),
                e.getEventDate(), e.getVenue(), e.getMaxPeople(), e.getCostOfRegistration(), e.getImageUrl(),
                registerRepository.countRegistrationsByEventId(e.getId()));
    }

    private EventsDto convertToDto(Events e) {
        return new EventsDto(e.getId(), e.getName(), e.getEventDate(), e.getCategory(), e.getVenue());
    }
}
