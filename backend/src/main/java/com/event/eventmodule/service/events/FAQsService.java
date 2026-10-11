package com.event.eventmodule.service.events;

import com.event.sharedmodule.exception.ResourceNotFoundException;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.event.FAQs;
import com.event.eventmodule.repository.EventsRepository;
import com.event.eventmodule.repository.FAQsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FAQsService {

    private final EventsRepository eventsRepository;
    private final FAQsRepository faqRepository;

    @Transactional
    public FAQs addFAQ(Long eventId, FAQs faq) {

        Events event = eventsRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Event not found: " + eventId));

        // set relationship (both sides)
        faq.setEvents(event);          // owning side (DB FK)
        event.getFaQs().add(faq);         // keep object graph consistent

        // save ONLY owning side
        return faqRepository.save(faq);
    }


    public @Nullable List<FAQs> getFAQsByEvent(Long eventId) {
        return faqRepository.findByEvents_Id(eventId);
    }
}
