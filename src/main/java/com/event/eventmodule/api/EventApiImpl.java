package com.event.eventmodule.api;

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

    private EventsDto convertToDto(Events e) {
        return new EventsDto(e.getId(), e.getName(), e.getEventDate(), e.getCategory(), e.getVenue());
    }
}
