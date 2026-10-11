package com.event.authmodule.dtoPayload;

/** Platform-wide numbers for the admin dashboard overview cards. */
public record AdminOverviewDto(long totalUsers, long totalEvents, long activeOrganizers, long platformRevenue) {
}
