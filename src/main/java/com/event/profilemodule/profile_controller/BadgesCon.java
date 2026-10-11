package com.event.profilemodule.profile_controller;

import com.event.profilemodule.Services.StudentService;
import com.event.profilemodule.entity.admin.Badges;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
public class BadgesCon {
    private final StudentService studentService;

    @PostMapping("/badges")
    public Set<Badges> updateBadges(
            @RequestAttribute("userId") String userId,
            @RequestBody Set<Long> badgesIds
    ) {
        return studentService.updateBadges(Long.valueOf(userId), badgesIds);
    }

    @GetMapping("/badges")
    public Set<Badges> getStudentBadges(
            @RequestAttribute("userId") String userId
    ) {
        return studentService.getAllBadges(Long.valueOf(userId));
    }

}
