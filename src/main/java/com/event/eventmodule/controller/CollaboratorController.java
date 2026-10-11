package com.event.eventmodule.controller;


import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.config.RequiresEventAccess;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.repository.CollaboratorsRepository;
import com.event.eventmodule.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/organizer")
@RequiredArgsConstructor
public class CollaboratorController {
    public final CollaboratorsRepository collaboratorsRepository;
    private final EventsRepository eventsRepository;
    private final EventApi eventApi;

    // Collaborator emails are only visible to the event's organizer, its collaborators and admins
    @RequiresEventAccess
    @GetMapping("/{eventId}")
    public List<String> getCollabortors(@PathVariable Long eventId){
        return collaboratorsRepository.findEmailByEventId(eventId);
    }

    @GetMapping("/collaboratorEvents")
    public List<Events> getCollaboratorEvents(@RequestAttribute("username") String email) {
        List<Long> eventIds = collaboratorsRepository.findEventIdsByEmail(email);
        if (eventIds.isEmpty()) return Collections.emptyList();
        return eventsRepository.findAllById(eventIds);
    }

    /**
     * Tells the caller whether *they* may scan attendance QR codes for this event.
     * The identity comes from the JWT; the old email/userId query params are accepted but ignored,
     * because trusting them let anyone probe who organizes or collaborates on any event.
     */
    @GetMapping("/can-scan/{eventId}")
    public boolean canScan(@PathVariable Long eventId,
                           @RequestAttribute("username") String email,
                           @RequestAttribute("userId") String userId) {
        return eventApi.canScan(eventId, email, Long.valueOf(userId));
    }

}
