package com.event.eventmodule.service;

import com.event.eventmodule.Registration.UserRegistrationRepository;
import com.event.eventmodule.entity.home.EventSummaryDto;
import com.event.eventmodule.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;    // ✔ CORRECT
import org.springframework.data.domain.Page;       // ✔ CORRECT

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventSummaryService {

    private final EventsRepository repository;
    private final CertificateTemplateRepository certificateTemplateRepository;
    private final CollaboratorsRepository collaboratorsRepository;
    private final EventImageRepository eventImageRepository;
    private final EventLinkRepository eventLinkRepository;
    private final FeedBackRepository feedBackRepository;
    private final FeedBackTableRepository feedBackTableRepository;
    private final UserRegistrationRepository userRegistrationRepository;

    public Page<EventSummaryDto> getPaginatedEventSummaries(Pageable pageable) {
        return repository.fetchEventSummaries(pageable);
    }

    @Transactional
    public void deleteEventCompletely(Long eventId) {
        // Delete all externally linked entities using custom queries
        certificateTemplateRepository.deleteByEventId(eventId);
        collaboratorsRepository.deleteAllByEventId(eventId);
        eventImageRepository.deleteAllByEventId(eventId);
        eventLinkRepository.deleteAllByEventId(eventId);
        feedBackRepository.deleteAllByEventId(eventId);
        feedBackTableRepository.deleteAllByEventId(eventId);
        userRegistrationRepository.deleteAllByEventId(eventId);   // otherwise "already registered" lingers

        // Finally, delete the event (this cascades to registrations, announcements, schedules, etc.)
        repository.deleteById(eventId);
    }
}
