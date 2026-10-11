package com.event.analyticsmodule.communication;

public interface DashboardApi {

    /**
     * Initializes a student dashboard when a new user signs up.
     * Called by: Auth module on successful signup.
     */
    void initializeStudentDashboard(Long userId);

    /**
     * Initializes an organizer dashboard when a user upgrades to organizer.
     * Removes existing student dashboard record automatically.
     * Called by: Auth module on role upgrade.
     */
    void initializeOrganizerDashboard(Long userId);

    /**
     * Updates both student and organizer stats after an event is completed.
     * Called by: Registration/Event module on event completion.
     */
    void updateStatsOnEventCompletion(DashboardUpdateRequest request);

    /** Sum of all organizers' recorded revenue (credited when events are completed). */
    long totalPlatformRevenue();
}