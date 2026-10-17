package com.event.eventmodule.Registration;

import com.event.eventmodule.api.SeatEvents;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.repository.EventsRepository;
import com.event.participationmodule.communication.AttendanceApi;
import com.event.sharedmodule.UserActivityEvent;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * Full events take a waitlist. When a registered student releases their seat, the first student on the
 * waitlist is registered automatically (same QR email as a normal registration) and told in the app.
 * Callers hold RegistrationLock so seat counts can't race.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistService {

    private final WaitlistEntryRepository waitlistRepository;
    private final EventsRepository eventsRepository;
    private final RegisterRepository registerRepository;
    private final UserRegistrationRepository userRegistrationRepository;
    private final RegistrationService registrationService;
    private final AttendanceApi attendanceApi;
    private final ApplicationEventPublisher events;

    public record Status(boolean registered, boolean full, boolean onWaitlist, Integer position, long waitlistSize,
                         boolean canRelease) {}

    public Status status(Long userId, Long eventId) {
        Events e = event(eventId);
        boolean registered = userRegistrationRepository.existsByUserIdAndEventId(userId, eventId);
        List<WaitlistEntry> line = waitlistRepository.findByEventIdOrderByCreatedAtAscIdAsc(eventId);
        Integer position = null;
        for (int i = 0; i < line.size(); i++) if (line.get(i).getUserId().equals(userId)) position = i + 1;
        boolean canRelease = registered && !started(e) && !attendanceApi.hasAttended(userId, eventId);
        return new Status(registered, isFull(e), position != null, position, line.size(), canRelease);
    }

    @Transactional
    public Status join(Long userId, Long eventId, Register details) {
        Events e = event(eventId);
        if (!Boolean.TRUE.equals(e.getIsApproved())) throw bad("This event is not open for registration yet");
        if (started(e)) throw bad("This event has already started");
        if (userRegistrationRepository.existsByUserIdAndEventId(userId, eventId)) throw bad("You're already registered for this event");
        if (!isFull(e)) throw bad("Seats are available. Register instead.");
        if (waitlistRepository.findByEventIdAndUserId(eventId, userId).isEmpty()) {
            if (details == null || blank(details.getName()) || blank(details.getEmail())) {
                throw bad("Enter your name and email so we can register you when a seat opens");
            }
            WaitlistEntry w = new WaitlistEntry();
            w.setEventId(eventId);
            w.setUserId(userId);
            w.setName(details.getName().trim());
            w.setEmail(details.getEmail().trim());
            w.setPhone(details.getPhone());
            w.setDepartment(details.getDepartment());
            w.setYearOfStudy(details.getYearOfStudy());
            w.setCreatedAt(LocalDateTime.now());
            waitlistRepository.save(w);
            events.publishEvent(new UserActivityEvent(userId, "waitlist_joined", eventId));
        }
        return status(userId, eventId);
    }

    @Transactional
    public Status leave(Long userId, Long eventId) {
        waitlistRepository.findByEventIdAndUserId(eventId, userId).ifPresent(waitlistRepository::delete);
        return status(userId, eventId);
    }

    /** "Can't make it": frees the seat and gives it to the first student on the waitlist. */
    @Transactional
    public Status release(Long userId, Long eventId) throws Exception {
        Events e = event(eventId);
        if (started(e)) throw bad("The event has already started, so the seat can't be released");
        if (attendanceApi.hasAttended(userId, eventId)) throw bad("You've already checked in to this event");
        List<UserRegistration> links = userRegistrationRepository.findByUserId(userId).stream()
                .filter(u -> eventId.equals(u.getEventId())).toList();
        if (links.isEmpty()) throw bad("You're not registered for this event");
        for (UserRegistration link : links) {
            registerRepository.findById(link.getRegistrationId()).ifPresent(r -> {
                if (e.getRegisters() != null) e.getRegisters().remove(r);
                registerRepository.delete(r);
            });
            userRegistrationRepository.delete(link);
        }
        attendanceApi.cancel(userId, eventId);
        registerRepository.flush();
        events.publishEvent(new SeatEvents.SeatReleased(userId, eventId));
        events.publishEvent(new UserActivityEvent(userId, "seat_released", eventId));
        promoteNext(e);
        return status(userId, eventId);
    }

    private void promoteNext(Events e) throws Exception {
        for (WaitlistEntry next : waitlistRepository.findByEventIdOrderByCreatedAtAscIdAsc(e.getId())) {
            if (isFull(e)) return;
            waitlistRepository.delete(next);
            if (userRegistrationRepository.existsByUserIdAndEventId(next.getUserId(), e.getId())) continue;
            Register r = new Register();
            r.setName(next.getName());
            r.setEmail(next.getEmail());
            r.setPhone(next.getPhone());
            r.setDepartment(next.getDepartment());
            r.setYearOfStudy(next.getYearOfStudy());
            registrationService.save(next.getUserId(), e.getId(), r);
            events.publishEvent(new SeatEvents.PromotedFromWaitlist(next.getUserId(), e.getId()));
            log.info("Waitlist: user {} got a seat at event {}", next.getUserId(), e.getId());
            return;
        }
    }

    private boolean isFull(Events e) {
        return e.getMaxPeople() != null && registerRepository.countRegistrationsByEventId(e.getId()) >= e.getMaxPeople();
    }

    private static boolean started(Events e) {
        return Boolean.TRUE.equals(e.getIsCompleted()) || (e.getEventDate() != null && !e.getEventDate().after(new Date()));
    }

    private Events event(Long eventId) {
        return eventsRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
