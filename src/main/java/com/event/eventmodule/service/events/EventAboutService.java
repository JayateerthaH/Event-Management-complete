package com.event.eventmodule.service.events;

import java.util.Optional;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.event.AboutEvent;
import com.event.eventmodule.repository.AboutEventRepository;
import com.event.eventmodule.repository.EventsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventAboutService {

    private final AboutEventRepository aboutEventRepository;
    private final EventsRepository eventsRepository;

    @Transactional
    public AboutEvent createAbout(Long eventId, AboutEvent aboutEvent) {

        Events event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // set relationship (both sides)
        event.setAboutEvent(aboutEvent);
        aboutEvent.setEvents(event);

        // save ONLY owning side
        eventsRepository.save(event);

        return aboutEvent;
    }

    public Optional<AboutEvent> getAbout(Long eventId) {
        // Unknown event -> 404; existing event without this section -> empty (controller answers 204)
        if (!eventsRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
        }
        return aboutEventRepository.findByEvents_Id(eventId);
    }

}
