package com.event.eventmodule.service.events;

import com.event.sharedmodule.exception.ResourceNotFoundException;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.event.WhatToBring;
import com.event.eventmodule.repository.EventsRepository;
import com.event.eventmodule.repository.WhatToBringRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WhatToBringService {

    private final EventsRepository eventsRepository;
    private final WhatToBringRepository whatToBringRepository;

    @Transactional
    public WhatToBring addWhatToBring(Long eventId, WhatToBring whatToBring) {

        Events event = eventsRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Event not found"));

        // owning side (FK written here)
        whatToBring.setEvents(event);

        // optional: keep object graph consistent
        event.getWhatToBring().add(whatToBring);

        // save ONLY owning side
        return whatToBringRepository.save(whatToBring);
    }


    public @Nullable List<WhatToBring> getWhatToBring(Long eventId) {
        return whatToBringRepository.findByEvents_Id(eventId);
    }
}
