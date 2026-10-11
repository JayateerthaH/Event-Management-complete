package com.event.eventmodule.controller.event;


import com.event.eventmodule.config.CheckAvailability;
import com.event.eventmodule.config.RequiresEventAccess;
import com.event.eventmodule.entity.event.eventdetail.EventDetails;
import com.event.eventmodule.service.events.EventDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequiredArgsConstructor
public class EventDetailsController {

    private final EventDetailsService eventDetailsService;
    private final CheckAvailability checkAvailability;

    @RequiresEventAccess
    @PostMapping("organizer/events/{eventId}/details")
    public ResponseEntity<?> addEventDetails(@PathVariable Long eventId,
                                             @RequestBody EventDetails details
    ) throws AccessDeniedException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventDetailsService.addEventDetails(eventId, details));
    }

    @GetMapping("/events/{eventId}/details")
    public ResponseEntity<?> getEventDetails(@PathVariable Long eventId){
        return eventDetailsService.getEventDetails(eventId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

}
