package com.event.socialmodule.recap;

import com.event.eventmodule.api.EventApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class EventRecapController {

    private final EventRecapService recapService;
    private final EventApi eventApi;

    @GetMapping("/social/events/{eventId}/recap")
    public EventRecapService.Recap recap(@PathVariable Long eventId, @RequestAttribute("userId") String userId,
                                         @RequestAttribute("userRoles") String roles, @RequestAttribute("username") String email) {
        if (!eventApi.canManageEvent(eventId, userId, roles, email)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return recapService.recap(eventId);
    }
}
