package com.event.analyticsmodule.engagement;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EngagementController {

    private final EngagementService engagementService;
    private final HealthMetricsService healthMetricsService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/engagement")
    public EngagementService.Engagement engagement() {
        return engagementService.engagement();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/engagement/health")
    public HealthMetricsService.Health health() {
        return healthMetricsService.health();
    }
}
