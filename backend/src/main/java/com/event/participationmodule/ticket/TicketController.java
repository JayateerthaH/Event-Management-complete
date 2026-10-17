package com.event.participationmodule.ticket;

import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventCard;
import com.event.participationmodule.Repository.UserRepository;
import com.event.participationmodule.Repository.UserRequestRepository;
import com.event.participationmodule.entity.UserRequest;
import com.event.participationmodule.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * The student's QR tickets (the same codes that are emailed), so the app can keep them on the phone and
 * show them at the door without signal.
 */
@RestController
@RequiredArgsConstructor
public class TicketController {

    private final UserRequestRepository userRequestRepository;
    private final UserRepository userRepository;
    private final EventApi eventApi;

    public record Ticket(Long eventId, String eventName, Date eventDate, String venue, String code, boolean attended) {}

    @GetMapping("/attendance/my-tickets")
    public ResponseEntity<List<Ticket>> myTickets(@RequestAttribute("userId") String userId) {
        Instant cutoff = Instant.now().minus(Duration.ofDays(1));   // keep today's event even after it starts
        List<Ticket> tickets = new ArrayList<>();
        for (UserRequest r : userRequestRepository.findByUserId(Long.valueOf(userId))) {
            Optional<EventCard> card = eventApi.eventProfile(r.getEventId()).map(p -> p.card());
            if (card.isEmpty() || card.get().eventDate() == null || card.get().eventDate().toInstant().isBefore(cutoff)) continue;
            String code = userRepository.findFirstByEmailAndEventId(r.getEmail(), r.getEventId()).map(Users::getRandomId).orElse(null);
            if (code == null) continue;
            EventCard e = card.get();
            tickets.add(new Ticket(e.id(), e.name(), e.eventDate(), e.venue(), code, Boolean.TRUE.equals(r.getIsAttended())));
        }
        tickets.sort(Comparator.comparing(Ticket::eventDate));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tickets);
    }
}
