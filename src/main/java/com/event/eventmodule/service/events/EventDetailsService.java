package com.event.eventmodule.service.events;


import java.util.Optional;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.event.eventdetail.EventDetails;
import com.event.eventmodule.repository.EventDetailsRepository;
import com.event.eventmodule.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventDetailsService {

    private final EventsRepository eventsRepository;
    private final EventDetailsRepository eventDetailsRepository;

    public EventDetails addEventDetails(Long eventId, EventDetails details) {

        Events event = eventsRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Event not found with id: " + eventId));

        // set relationship (both sides)
        event.setEventDetails(details);
        details.setEvents(event);

        // save ONLY owning side
        eventsRepository.save(event);

        return details;
    }

    public Optional<EventDetails> getEventDetails(Long eventId) {
        // Unknown event -> 404; existing event without this section -> empty (controller answers 204)
        if (!eventsRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
        }
        return eventDetailsRepository.findByEvents_Id(eventId);
    }
}
