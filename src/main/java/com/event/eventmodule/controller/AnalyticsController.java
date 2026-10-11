package com.event.eventmodule.controller;

import com.event.eventmodule.entity.analysis.MonthlyAttendanceCount;
import com.event.eventmodule.service.AnalyticsService;
import com.event.sharedmodule.SelfOrAdmin;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @SelfOrAdmin
    @GetMapping("/user/{userId}/attendance")
    public List<MonthlyAttendanceCount> getUserAttendance(
            @PathVariable Long userId
    ) {
        return analyticsService.getUserAttendanceAnalytics(userId);
    }

    @SelfOrAdmin
    @GetMapping("/organizer/{userId}/registrations")
    public List<MonthlyAttendanceCount> getOrganizerRegistrations(
            @PathVariable Long userId
    ) {
        return analyticsService.getOrganizerAttendanceAnalytics(userId);
    }
}
