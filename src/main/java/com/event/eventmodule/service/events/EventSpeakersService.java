package com.event.eventmodule.service.events;

import com.event.sharedmodule.exception.ResourceNotFoundException;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.event.EventSpeakers;
import com.event.eventmodule.repository.EventSpeakersRepository;
import com.event.eventmodule.repository.EventsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventSpeakersService {

    private final EventsRepository eventsRepository;
    private final EventSpeakersRepository speakersRepository;

    @Transactional
    public EventSpeakers addSpeaker(Long eventId, EventSpeakers speaker) {

        Events event = eventsRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Event not found with id: " + eventId));

        // owning side (FK written here)
        speaker.setEvents(event);

        // optional: keep object graph consistent
        event.getSpeakers().add(speaker);

        // save ONLY owning side
        return speakersRepository.save(speaker);
    }


    public @Nullable List<EventSpeakers> getFAQsByEvent(Long eventId) {
        return speakersRepository.findByEvents_Id(eventId);
    }
}

