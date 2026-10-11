package com.event.eventmodule.service.events;

import java.util.Optional;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.event.EventSchedule;
import com.event.eventmodule.repository.EventScheduleRepository;
import com.event.eventmodule.repository.EventsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class EventScheduleService {

    private final EventsRepository eventsRepository;
    private final EventScheduleRepository eventScheduleRepository;

    @Transactional
    public EventSchedule createSchedule(Long eventId, EventSchedule eventSchedule) {

        Events eventsDto = eventsRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Event not found with id: " + eventId));

        // set relationship (both sides)
        eventsDto.setEventSchedule(eventSchedule);
        eventSchedule.setEvents(eventsDto);

        // save ONLY owning side
        eventsRepository.save(eventsDto);

        return eventSchedule;
    }

    public Optional<EventSchedule> getEventSchedule(Long eventId) {
        // Unknown event -> 404; existing event without this section -> empty (controller answers 204)
        if (!eventsRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
        }
        return eventScheduleRepository.findByEvents_Id(eventId);
    }
}
