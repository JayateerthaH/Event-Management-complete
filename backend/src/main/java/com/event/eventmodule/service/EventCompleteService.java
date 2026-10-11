package com.event.eventmodule.service;

import com.event.eventmodule.Registration.RegisterRepository;
import com.event.eventmodule.api.AsyncService;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.config.BasicUpdateDto;
import com.event.eventmodule.repository.EventsRepository;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventCompleteService {
    private final RegisterRepository registerRepository;
    private final EventsRepository eventsRepository;
    private final AsyncService asyncService;

    public List<Long> findAllStudents(Long id) {
        return registerRepository.findUserIdsByEventIdAndIsAttendedTrue(id);
    }

    /**
     * Marks the event completed and pushes its stats to the dashboards exactly once.
     * Stats are credited to the event's organizer, not to whoever (e.g. a collaborator) triggers it.
     */
    @Transactional
    public void completeEvent(Long eventId) {
        Events event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (Boolean.TRUE.equals(event.getIsCompleted())) {
            throw new IllegalArgumentException("Event " + eventId + " is already completed");
        }

        BasicUpdateDto basicUpdateDto = new BasicUpdateDto(
                findAllStudents(eventId),
                durationInHours(event),
                registerRepository.countRegistrationsByEventId(eventId),
                event.getOrganizerId(),
                costPerRegistration(event)
        );

        event.setIsCompleted(true);
        asyncService.sendUpdateAsync(basicUpdateDto);
    }

    private long durationInHours(Events event) {
        return event.getEventDetails() != null && event.getEventDetails().getDurationInHours() != null
                ? event.getEventDetails().getDurationInHours() : 1L;
    }

    private long costPerRegistration(Events event) {
        String cost = event.getCostOfRegistration();
        try {
            return cost != null && !cost.isBlank() ? Long.parseLong(cost.trim()) : 0L;
        } catch (NumberFormatException ex) {
            return 0L;   // e.g. "Free"
        }
    }
}
