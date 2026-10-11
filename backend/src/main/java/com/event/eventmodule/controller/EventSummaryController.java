package com.event.eventmodule.controller;


import com.event.sharedmodule.exception.ResourceNotFoundException;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.home.EventSummaryDto;
import com.event.eventmodule.repository.EventsRepository;
import com.event.eventmodule.service.EventSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;        //✔ CORRECT
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("events")
public class EventSummaryController {

    private final EventSummaryService service;
    private final EventsRepository eventsRepository;

    // Get summary list of all events (admin — includes unapproved)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/summary/all")
    public Page<EventSummaryDto> getPaginatedEventSummaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {

        return service.getPaginatedEventSummaries(PageRequest.of(page, size));
    }

    // Approve an event (admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/approve/{eventId}")
    public ResponseEntity<?> approveEvent(
            @PathVariable Long eventId,
            @RequestAttribute("userRoles") String rolesHeader) {
        Events event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        event.setIsApproved(true);
        eventsRepository.save(event);
        return ResponseEntity.ok("Event approved successfully");
    }

    // Reject / un-approve an event (admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/reject/{eventId}")
    public ResponseEntity<?> rejectEvent(
            @PathVariable Long eventId,
            @RequestAttribute("userRoles") String rolesHeader) {
        service.deleteEventCompletely(eventId);
        return ResponseEntity.ok("Event rejected and removed");
    }
}
