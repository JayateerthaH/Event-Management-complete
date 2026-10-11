package com.event.eventmodule.controller.event;

import com.event.eventmodule.config.CheckAvailability;
import com.event.eventmodule.config.RequiresEventAccess;
import com.event.eventmodule.entity.event.highlight.KeyHighlights;
import com.event.eventmodule.service.events.KeyHighlightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequiredArgsConstructor
public class KeyHighlightsController {

    private final KeyHighlightsService keyHighlightsService;
    private final CheckAvailability checkAvailability;

    @RequiresEventAccess
    @PostMapping("organizer/events/{eventId}/highlights")
    public ResponseEntity<?> addKeyHighlights(@PathVariable Long eventId,
                                              @RequestBody KeyHighlights keyHighlights) throws AccessDeniedException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(keyHighlightsService.addHighlights(eventId, keyHighlights));
    }

    @GetMapping("/events/{eventId}/highlights")
    public ResponseEntity<KeyHighlights> getKeyHighlights(
            @PathVariable Long eventId) {

        return keyHighlightsService.getHighlights(eventId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

}