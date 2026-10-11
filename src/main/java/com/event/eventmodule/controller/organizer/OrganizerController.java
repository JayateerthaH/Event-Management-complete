package com.event.eventmodule.controller.organizer;

import com.event.eventmodule.config.RequiresEventAccess;
import com.event.eventmodule.entity.event.Collaborators;
import com.event.eventmodule.repository.CollaboratorsRepository;
import com.event.eventmodule.service.EventCompleteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/organizer")
@RequiredArgsConstructor
@Slf4j
public class OrganizerController {

    private final EventCompleteService eventCompleteService;
    private final CollaboratorsRepository collaboratorsRepository;


    @GetMapping("/complete/{eventId}")
    @RequiresEventAccess
    public ResponseEntity<?> getAllUserIds(@PathVariable Long eventId) {
        eventCompleteService.completeEvent(eventId);
        return ResponseEntity.ok("Successfully completed");
    }

    @PostMapping("/addCollaborator")
    @RequiresEventAccess
    public ResponseEntity<?> addCollaborator(
                                             @RequestBody Collaborators collaborators
                                             )
    {

        return ResponseEntity.ok(collaboratorsRepository.save(new Collaborators(collaborators.getEventId(),
                collaborators.getEmail())));

    }
}
