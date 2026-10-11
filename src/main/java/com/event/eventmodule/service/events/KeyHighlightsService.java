package com.event.eventmodule.service.events;


import java.util.Optional;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.event.highlight.KeyHighlights;
import com.event.eventmodule.repository.EventsRepository;
import com.event.eventmodule.repository.KeyHighlightsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KeyHighlightsService {

    private final EventsRepository eventsRepository;
    private final KeyHighlightsRepository keyHighlightsRepository;

    @Transactional
    public KeyHighlights addHighlights(Long eventId, KeyHighlights keyHighlights) {

        Events event = eventsRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Event not found with id: " + eventId));

        // set relationship (both sides)
        event.setKeyHighlights(keyHighlights);
        keyHighlights.setEvents(event);

        // save ONLY owning side
        eventsRepository.save(event);

        return keyHighlights;
    }

    public Optional<KeyHighlights> getHighlights(Long eventId) {
        // Unknown event -> 404; existing event without this section -> empty (controller answers 204)
        if (!eventsRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
        }
        return keyHighlightsRepository.findByEvents_Id(eventId);
    }

}
