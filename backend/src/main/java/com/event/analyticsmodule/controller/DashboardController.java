package com.event.analyticsmodule.controller;

import com.event.analyticsmodule.communication.DashboardApi;
import com.event.analyticsmodule.communication.DashboardUpdateRequest;
import com.event.analyticsmodule.dtoPayload.EventsDto;
import com.event.analyticsmodule.entity.organizer.BasicInformation;
import com.event.analyticsmodule.entity.student.DashboardBasicInfo;
import com.event.analyticsmodule.repository.DashboardBasicInfoRepository;
import com.event.analyticsmodule.repository.BasicInformationRepository;
import com.event.analyticsmodule.service.DashboardService;
import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.entity.analysis.MonthlyAttendanceCount;
import com.event.eventmodule.entity.config.BasicUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard")
public class DashboardController {

    private final BasicInformationRepository basicInformationRepository;
    private final DashboardBasicInfoRepository dashboardBasicInfoRepository;
    private final DashboardService dashboardService;
    private final EventApi eventApi;
    private final DashboardApi dashboardApi;

    @GetMapping("/events/pending")
    public List<EventsDto> getPendingEvents(@RequestAttribute("userId") String userId) {
        // using security context holder i have to fetch userId
        return dashboardService.getPendingEventsForUser(Long.valueOf(userId));
    }

    @GetMapping("/events/past")
    public List<com.event.analyticsmodule.dtoPayload.EventsDto> getPastEvents(@RequestAttribute("userId") String userId) {
        // using security context holder i have to fetch userId
        return dashboardService.getPastEventsForUser(Long.valueOf(userId));
    }


    @GetMapping("/user")
    public DashboardBasicInfo getBasicInfo(@RequestAttribute("userId") String userId){
//        System.out.println(dashboardBasicInfoRepository.findByUserId(Long.valueOf(userId)));

        return dashboardBasicInfoRepository.findByUserId(Long.valueOf(userId));
    }

    @GetMapping("/organizer")
    public BasicInformation getBasicOrgInfo(@RequestAttribute("userId") String userId){
        return basicInformationRepository.findByUserId(Long.valueOf(userId));
    }

    @GetMapping("user/analytics")
    public List<MonthlyAttendanceCount> getUserAttendance(
            @RequestAttribute("userId") String userId
    ) {
        return eventApi.getAttendanceCount(Long.valueOf(userId));
    }

    // Stats are updated internally by OrganizerController#complete; this raw endpoint is admin-only
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/organizer/complete")
    public void getAllStudents(@RequestBody DashboardUpdateRequest basicUpdateDto){
        dashboardService.updateBasicInfo(basicUpdateDto);
    }

    @GetMapping("organizer/analytics")
    public List<MonthlyAttendanceCount> getOrganizerRegistrations(
            @RequestAttribute("userId") String userId
    ) {
        return eventApi.getRegisteredCount(Long.valueOf(userId));
    }

    // Leftover endpoints from the microservice split; signup/onboarding now call DashboardApi directly.
    // Delegating keeps a single implementation and the admin check stops users wiping each other's data.
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create/user/{userId}")
    public void initBasicInfo(@PathVariable Long userId){
        dashboardApi.initializeStudentDashboard(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create/organizer/{userId}")
    public void initBasicOrganizerInfo(@PathVariable Long userId){
        dashboardApi.initializeOrganizerDashboard(userId);
    }
}
