package com.event.eventmodule.duplicate;

import com.event.eventmodule.config.EventAccessService;
import com.event.eventmodule.entity.Events;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class EventDuplicationController {

    private final EventDuplicationService duplicationService;
    private final EventAccessService eventAccessService;

    public record DuplicateRequest(Date eventDate, String name) {}

    /** POST /organizer/events/{eventId}/duplicate {eventDate, name?}: the new event awaits admin approval. */
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    @PostMapping("/organizer/events/{eventId}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> duplicate(@PathVariable Long eventId, @RequestBody DuplicateRequest body,
                                         @RequestAttribute("userId") String userId,
                                         @RequestAttribute(value = "userRoles", required = false) String roles,
                                         @RequestAttribute(value = "username", required = false) String email) {
        if (!eventAccessService.isAllowed(eventId, userId, roles, email)) {
            throw new AccessDeniedException("Only the event's organizer or a collaborator can duplicate it");
        }
        Events copy = duplicationService.duplicate(eventId, Long.valueOf(userId),
                body == null ? null : body.eventDate(), body == null ? null : body.name());
        return Map.of("id", copy.getId(), "name", copy.getName(),
                "message", "Duplicated. The new event is waiting for admin approval; add a cover image for it.");
    }
}
