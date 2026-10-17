package com.event.eventmodule.filter;

import com.event.eventmodule.config.EventAccessService;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import com.event.eventmodule.Registration.RegisterRepository;
import com.event.eventmodule.Registration.UserRegistrationRepository;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.announcement.Announcement;
import com.event.eventmodule.repository.EventsRepository;
import com.event.eventmodule.service.events.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventsDtoController {

    private final EventService eventsDtoService;
    private final EventsRepository eventsRepository;
    private final RegisterRepository registerRepository;
    private final UserRegistrationRepository userRegistrationRepository;
    private final AnnouncementService announcementService;
    private final EventAccessService eventAccessService;

    @GetMapping("/{eventId}")
    public ResponseEntity<?> getEventById(
            @PathVariable Long eventId,
            @RequestAttribute(value = "userId", required = false) String userId,
            @RequestAttribute(value = "userRoles", required = false) String roles,
            @RequestAttribute(value = "username", required = false) String email) {

        Events event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        // Unapproved events are only visible to their organizer, collaborators and admins
        if (!Boolean.TRUE.equals(event.getIsApproved())
                && !eventAccessService.isAllowed(eventId, userId, roles, email)) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
        }

        Long registeredCount = registerRepository.countRegistrationsByEventId(eventId);
        boolean isRegistered = false;
        if (userId != null && !userId.isEmpty()) {
            isRegistered = userRegistrationRepository.existsByUserIdAndEventId(Long.valueOf(userId), eventId);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", event.getId());
        response.put("name", event.getName());
        response.put("description", event.getDescription());
        response.put("category", event.getCategory());
        response.put("eventField", event.getEventField());
        response.put("eventDate", event.getEventDate());
        response.put("venue", event.getVenue());
        response.put("maxPeople", event.getMaxPeople());
        response.put("costOfRegistration", event.getCostOfRegistration());
        response.put("imageUrl", event.getImageUrl());
        response.put("isFeatured", event.getIsFeatured());
        response.put("organizerId", event.getOrganizerId());
        response.put("registeredCount", registeredCount);
        response.put("isRegistered", isRegistered);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{eventId}/announcements")
    public ResponseEntity<?> getEventAnnouncements(
            @PathVariable Long eventId,
            @RequestAttribute(value = "userId") String userId) {
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        List<Announcement> announcements = announcementService.getAnnouncementsForEventAndUser(eventId, Long.valueOf(userId));
        return ResponseEntity.ok(announcements);
    }

    @PostMapping("/filter")
    public Page<?> filterEvents(
            @RequestBody EventFilterRequest request
    ) {

        // Jackson 3 binds through the all-args constructor, so omitted fields arrive as null, not their defaults
        int page = request.getPage() != null && request.getPage() >= 0 ? request.getPage() : 0;
        // Capped so one request can't pull the whole catalog (the events page asks for 6 at a time)
        int size = request.getSize() != null && request.getSize() > 0 ? Math.min(request.getSize(), MAX_PAGE_SIZE) : 6;
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("eventDate").ascending()
        );

        return eventsDtoService.filterEvents(
                merge(request.getCategory(), request.getCategories()),
                request.getSearch(),
                merge(request.getVenue(), request.getVenues()),
                request.getEventField(),
                request.getDateRange(),
                request.getCostType(),
                pageable
        );
    }
    static final int MAX_PAGE_SIZE = 100;

    private static List<String> merge(String one, List<String> many) {
        List<String> all = new java.util.ArrayList<>(many == null ? List.of() : many);
        if (one != null && !one.isBlank()) all.add(one);
        return all;
    }

    /** Categories (with counts), venues and departments of approved events, for the events page filters. */
    @GetMapping("/filter-options")
    public Map<String, Object> filterOptions() {
        List<Map<String, Object>> categories = eventsRepository.countApprovedByCategory().stream()
                .map(r -> Map.<String, Object>of("value", r[0], "count", r[1])).toList();
        return Map.of(
                "categories", categories,
                "venues", eventsRepository.findApprovedVenues(),
                "departments", eventsRepository.findApprovedEventFields());
    }
}
