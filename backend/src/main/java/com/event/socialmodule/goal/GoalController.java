package com.event.socialmodule.goal;

import com.event.socialmodule.goal.GoalService.GoalView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/social/goal")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    public record GoalBody(Integer target) {}

    @GetMapping
    public GoalView goal(@RequestAttribute("userId") String userId) {
        return goalService.view(Long.valueOf(userId));
    }

    @PutMapping
    public GoalView set(@RequestAttribute("userId") String userId, @RequestBody GoalBody body) {
        if (body == null || body.target() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "target is required");
        return goalService.set(Long.valueOf(userId), body.target());
    }

    @DeleteMapping
    public GoalView clear(@RequestAttribute("userId") String userId) {
        return goalService.clear(Long.valueOf(userId));
    }
}
