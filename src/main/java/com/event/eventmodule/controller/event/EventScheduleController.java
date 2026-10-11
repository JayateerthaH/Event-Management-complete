package com.event.eventmodule.controller.event;


import com.event.eventmodule.config.CheckAvailability;
import com.event.eventmodule.config.RequiresEventAccess;
import com.event.eventmodule.entity.event.EventSchedule;
import com.event.eventmodule.service.events.EventScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequiredArgsConstructor
public class EventScheduleController {

    private final EventScheduleService eventScheduleService;

    @RequiresEventAccess
    @PostMapping("organizer/events/{eventId}/schedule")
    public ResponseEntity<?> addEventSchedule(@PathVariable Long eventId,
                                              @RequestBody EventSchedule eventSchedule) throws AccessDeniedException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventScheduleService.createSchedule(eventId, eventSchedule));
    }

    @GetMapping("/events/{eventId}/schedule")
    public ResponseEntity<?> getEventDetails(@PathVariable Long eventId) {
        return eventScheduleService.getEventSchedule(eventId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
