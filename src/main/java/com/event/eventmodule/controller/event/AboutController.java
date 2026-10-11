package com.event.eventmodule.controller.event;


import com.event.eventmodule.config.CheckAvailability;
import com.event.eventmodule.config.RequiresEventAccess;
import com.event.eventmodule.entity.event.AboutEvent;
import com.event.eventmodule.service.events.EventAboutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequiredArgsConstructor
public class AboutController {

    private final EventAboutService eventAboutService;
    private final CheckAvailability checkAvailability;

    @PostMapping("/organizer/events/{eventId}/about")
    @RequiresEventAccess
    public ResponseEntity<?> addAboutEvent(@PathVariable Long eventId,
                                                    @RequestBody AboutEvent aboutEvent
                                                    ) throws AccessDeniedException {
        //have to update using security context holder//

        return ResponseEntity.status(HttpStatus.CREATED).body(eventAboutService.createAbout(eventId,aboutEvent));
    }

    @GetMapping("/events/{eventId}/about")
    public ResponseEntity<?> getAboutEvent(@PathVariable Long eventId){
        return eventAboutService.getAbout(eventId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }


}
