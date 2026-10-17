package com.event.eventmodule.Registration;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Seat status, waitlist and "release my seat" for the logged-in student. */
@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;

    @GetMapping("/{eventId}/seat")
    public WaitlistService.Status status(@PathVariable Long eventId, @RequestAttribute("userId") String userId) {
        return waitlistService.status(Long.valueOf(userId), eventId);
    }

    @PostMapping("/{eventId}/waitlist")
    public WaitlistService.Status join(@PathVariable Long eventId, @RequestAttribute("userId") String userId,
                                       @RequestBody Register details) {
        synchronized (RegistrationLock.LOCK) {
            return waitlistService.join(Long.valueOf(userId), eventId, details);
        }
    }

    @DeleteMapping("/{eventId}/waitlist")
    public WaitlistService.Status leave(@PathVariable Long eventId, @RequestAttribute("userId") String userId) {
        return waitlistService.leave(Long.valueOf(userId), eventId);
    }

    @DeleteMapping("/{eventId}/register")
    public WaitlistService.Status release(@PathVariable Long eventId, @RequestAttribute("userId") String userId) throws Exception {
        synchronized (RegistrationLock.LOCK) {
            return waitlistService.release(Long.valueOf(userId), eventId);
        }
    }
}
