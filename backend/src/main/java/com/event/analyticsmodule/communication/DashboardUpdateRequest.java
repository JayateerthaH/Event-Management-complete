package com.event.analyticsmodule.communication;

// dashboard/api/DashboardUpdateRequest.java

import java.util.List;

/**
 * Public DTO for dashboard stats update.
 * Replaces BasicUpdateDto for cross-module communication.
 */
public record DashboardUpdateRequest(
        List<Long> studentIds,
        Long organizerId,
        Long hours,
        Long totalRegistrations,
        Long costPerRegistration
) {}