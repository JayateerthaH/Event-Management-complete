package com.event.socialmodule.wrapped;

import com.event.sharedmodule.UserActivityEvent;
import com.event.socialmodule.goal.Semester;
import com.event.socialmodule.wrapped.WrappedService.Wrapped;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/social/wrapped")
@RequiredArgsConstructor
public class WrappedController {

    private final WrappedService wrappedService;
    private final ApplicationEventPublisher activityPublisher;

    /** The current semester's recap, or another one with ?semester=2026-1. */
    @GetMapping
    public Wrapped wrapped(@RequestAttribute("userId") String userId, @RequestParam(required = false) String semester) {
        Semester s = semester == null ? Semester.current()
                : Semester.fromKey(semester).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown semester"));
        Long me = Long.valueOf(userId);
        activityPublisher.publishEvent(new UserActivityEvent(me, "wrapped_viewed", null));
        return wrappedService.forStudent(me, s);
    }

    /** Counts a share for engagement analytics (the share itself happens on the device). */
    @PostMapping("/shared")
    public void shared(@RequestAttribute("userId") String userId) {
        activityPublisher.publishEvent(new UserActivityEvent(Long.valueOf(userId), "wrapped_shared", null));
    }
}
