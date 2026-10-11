package com.event.eventmodule.controller.event;

import java.util.List;
import com.event.eventmodule.entity.home.AttendeeDto;
import com.event.eventmodule.config.RequiresEventAccess;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.service.EventsDtoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/event/organizer")
@Slf4j
@RequiredArgsConstructor
public class EventController {

    private final EventsDtoService eventsDtoService;

    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Events> createEvent(
            @RequestBody Events events,
            HttpServletRequest request,
            @RequestAttribute("userId") String userId
    ) throws IOException {

        log.info("Logged in user : - " + userId);
        events.setOrganizerId(Long.valueOf(userId));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(eventsDtoService.createEvent(events, request));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllEvents(@RequestAttribute("userId") String userId){
        log.info("userId header value: '{}'", userId);
        return ResponseEntity.ok(eventsDtoService.getEvents(Long.valueOf(userId)));
    }

    @GetMapping("/attendees")
    public List<AttendeeDto> getAttendees(@RequestAttribute("userId") String userId) {
        return eventsDtoService.getAttendees(Long.valueOf(userId));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId,
                                            @RequestAttribute("userId") String userId,
                                            @RequestAttribute("userRoles") String roles) {
        boolean isAdmin = java.util.Arrays.asList(roles.split(",")).contains("ADMIN");
        eventsDtoService.deleteEvent(eventId, Long.valueOf(userId), isAdmin);
        return ResponseEntity.noContent().build();
    }
}
