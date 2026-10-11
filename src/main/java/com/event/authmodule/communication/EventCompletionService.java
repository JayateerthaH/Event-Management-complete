package com.event.authmodule.communication;

import com.event.analyticsmodule.communication.DashboardApi;
import com.event.analyticsmodule.communication.DashboardUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventCompletionService {

    private final DashboardApi dashboardApi; // ← same pattern

    public void completeEvent(Long organizerId, List<Long> attendedStudentIds,
                              Long hours, Long registrations, Long costPerReg) {

        dashboardApi.updateStatsOnEventCompletion(
                new DashboardUpdateRequest(
                        attendedStudentIds,
                        organizerId,
                        hours,
                        registrations,
                        costPerReg
                )
        );
    }
}