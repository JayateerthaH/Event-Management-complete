package com.event.authmodule.dtoPayload;

import java.util.List;

/** Platform-wide numbers for the admin dashboard: overview cards and the Analytics tab. */
public record AdminOverviewDto(long totalUsers, long totalEvents, long activeOrganizers, long platformRevenue,
                               List<RoleCount> roleDistribution, List<MonthStat> systemAnalytics) {

    /** Accounts that have a role (an organizer is usually a student too, so counts can overlap). */
    public record RoleCount(String role, long count) {}

    /** One month, e.g. "May 2026": accounts created that month, and events taking place that month. */
    public record MonthStat(String month, long users, long events) {}
}
