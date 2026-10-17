package com.event.socialmodule.follow;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @GetMapping("/organizers/by-event/{eventId}")
    public FollowService.OrganizerView forEvent(@RequestAttribute("userId") String userId, @PathVariable Long eventId) {
        return followService.forEvent(Long.valueOf(userId), eventId);
    }

    @PostMapping("/follow/{organizerId}")
    public FollowService.OrganizerView follow(@RequestAttribute("userId") String userId, @PathVariable Long organizerId) {
        return followService.follow(Long.valueOf(userId), organizerId);
    }

    @DeleteMapping("/follow/{organizerId}")
    public FollowService.OrganizerView unfollow(@RequestAttribute("userId") String userId, @PathVariable Long organizerId) {
        return followService.unfollow(Long.valueOf(userId), organizerId);
    }

    @GetMapping("/following")
    public List<FollowService.OrganizerView> following(@RequestAttribute("userId") String userId) {
        return followService.following(Long.valueOf(userId));
    }
}
