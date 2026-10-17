package com.event.eventmodule.Registration;

import org.springframework.web.server.ResponseStatusException;
import com.event.eventmodule.api.AsyncService;
import com.event.eventmodule.entity.config.UserRequest;
import com.event.eventmodule.repository.EventsRepository;
import com.event.eventmodule.entity.Events;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegisterRepository registerRepository;
    private final EventsRepository eventsRepository;
    private final UserRegistrationRepository userRegistrationRepository;
    private final AsyncService asyncService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public ResponseEntity<?> save(Long userId,Long eventId, Register register) throws Exception {

        boolean exists = userRegistrationRepository.existsByUserIdAndEventId(userId, eventId);

        if (exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already Registered for the Event");
        }
        Events events = eventsRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        if (!Boolean.TRUE.equals(events.getIsApproved())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This event is not open for registration yet");
        }
        long registeredCount = registerRepository.countRegistrationsByEventId(eventId);
        // maxPeople == null means the organizer set no limit (previously an NPE)
        if(events.getMaxPeople() == null || registeredCount < events.getMaxPeople())
        {
            // Always create a new registration row; ID is DB-generated.
            register.setId(null);
            // Never trust attendance from the request body — it gates certificates and stats.
            register.setIsAttended(false);
            register.setEventsDto(events);
            register.setUserId(userId);
            register = registerRepository.save(register);
            
            UserRegistration userRegistration = new UserRegistration(userId, register.getId(), eventId);
            userRegistrationRepository.save(userRegistration);

            asyncService.sendNotificationAsync(new UserRequest(register.getEmail(), userId, eventId));
            eventPublisher.publishEvent(new com.event.eventmodule.api.EventRegisteredEvent(userId, eventId));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Registration successful!");
            response.put("registeredCount", registeredCount + 1);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        throw new ResponseStatusException(HttpStatus.GONE, "Sorry The Seats Are Full");
    }

    @Transactional
    public List<Events> getUpcomingEventsForUser(Long userId) {
        Date today = new Date();
        return findRegisteredEvents(userId, event -> event.getEventDate().after(today));
    }

    @Transactional
    public List<Events> getPastEventsForUser(Long userId) {
        Date today = new Date();
        return findRegisteredEvents(userId, event -> event.getEventDate().before(today));
    }

    // One batched lookup instead of a findById per registration (N+1)
    private List<Events> findRegisteredEvents(Long userId, Predicate<Events> filter) {
        List<Long> registrationIds = userRegistrationRepository.findByUserId(userId).stream()
                .map(UserRegistration::getRegistrationId)
                .toList();

        return registerRepository.findAllById(registrationIds).stream()
                .map(Register::getEventsDto)
                .filter(Objects::nonNull)
                .filter(event -> event.getEventDate() != null)
                .filter(filter)
                .toList();
    }

}
