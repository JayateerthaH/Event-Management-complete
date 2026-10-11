package com.event.eventmodule.controller.event;

import com.event.eventmodule.config.CheckAvailability;
import com.event.eventmodule.config.RequiresEventAccess;
import com.event.eventmodule.entity.event.WhatToBring;
import com.event.eventmodule.service.events.WhatToBringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class WhatToBringController {

    private final WhatToBringService whatToBringService;
    private final CheckAvailability checkAvailability;
    @PostMapping("organizer/events/{eventId}/whatToBring")
    @RequiresEventAccess
    public ResponseEntity<?> addWhatToBring(@PathVariable Long eventId,
                                            @RequestBody WhatToBring whatToBring) throws AccessDeniedException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(whatToBringService.addWhatToBring(eventId, whatToBring));
    }

    @GetMapping("/events/{eventId}/whatToBring")
    public ResponseEntity<List<WhatToBring>> getFAQs(@PathVariable Long eventId) {

        return ResponseEntity.ok(
                whatToBringService.getWhatToBring(eventId)
        );
    }

}
