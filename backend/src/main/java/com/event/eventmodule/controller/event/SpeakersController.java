package com.event.eventmodule.controller.event;

import com.event.eventmodule.config.CheckAvailability;
import com.event.eventmodule.config.RequiresEventAccess;
import com.event.eventmodule.entity.event.EventSpeakers;
import com.event.eventmodule.service.events.EventSpeakersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class SpeakersController {

    private final EventSpeakersService eventSpeakersService;
    private final CheckAvailability checkAvailability;
    @RequiresEventAccess
    @PostMapping("organizer/events/{eventId}/speakers")
    public ResponseEntity<?> addSpeakers(@PathVariable Long eventId,
                                         @RequestBody EventSpeakers speaker) throws AccessDeniedException {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventSpeakersService.addSpeaker(eventId, speaker));
    }

    @GetMapping("/events/{eventId}/speakers")
    public ResponseEntity<List<EventSpeakers>> getFAQs(@PathVariable Long eventId) {

        return ResponseEntity.ok(
                eventSpeakersService.getFAQsByEvent(eventId)
        );
    }
}
