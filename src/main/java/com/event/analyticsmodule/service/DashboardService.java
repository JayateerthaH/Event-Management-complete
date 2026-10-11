package com.event.analyticsmodule.service;

import com.event.analyticsmodule.communication.DashboardUpdateRequest;
import com.event.analyticsmodule.dtoPayload.EventsDto;
import com.event.analyticsmodule.entity.organizer.BasicInformation;
import com.event.analyticsmodule.entity.student.DashboardBasicInfo;
import com.event.analyticsmodule.dtoPayload.MonthlyAttendanceCount;
import com.event.analyticsmodule.repository.DashboardBasicInfoRepository;
import com.event.analyticsmodule.repository.BasicInformationRepository;
import com.event.eventmodule.api.EventApi;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BasicInformationRepository basicInformationRepository;
    private final DashboardBasicInfoRepository dashboardBasicInfoRepository;
    private final EventApi eventApi; // Spring injects EventApiImpl here
    private final ModelMapper modelMapper;

    public DashboardBasicInfo getStudentBasicInfo(Long userId) {
        return dashboardBasicInfoRepository.findByUserId(userId);
    }

    public BasicInformation getOrganizerBasicInfo(Long userId) {
        return basicInformationRepository.findByUserId(userId);
    }

    // Method 1: Upcoming Events
    public List<EventsDto> getPendingEventsForUser(Long userId) {
        return eventApi.getPendingEvents(userId).stream()
                .map(record -> {
                    EventsDto dto = modelMapper.map(record, EventsDto.class);
                    dto.setUserId(userId); // Adding the analytics-specific ID
                    return dto;
                })
                .toList();
    }

    // Method 2: Past Events
    public List<EventsDto> getPastEventsForUser(Long userId) {
        return eventApi.getPastEvents(userId).stream()
                .map(record -> {
                    EventsDto dto = modelMapper.map(record, EventsDto.class);
                    dto.setUserId(userId);
                    return dto;
                })
                .toList();
    }

    // Method 3: Student Attendance Analytics
    public List<MonthlyAttendanceCount> getUserAttendanceAnalytics(Long userId) {
        return eventApi.getAttendanceCount(userId).stream()
                .map(record -> modelMapper.map(record, MonthlyAttendanceCount.class))
                .toList();
    }

    // Method 4: Organizer Registration Analytics
    public List<MonthlyAttendanceCount> getOrganizerRegistrationAnalytics(Long userId) {
        return eventApi.getRegisteredCount(userId).stream()
                .map(record -> modelMapper.map(record, MonthlyAttendanceCount.class))
                .toList();
    }

    @Transactional
    public void updateBasicInfo(DashboardUpdateRequest request) {
        List<Long> studentIds = request.studentIds();
        Long attended = 0L;
        long registrations = request.totalRegistrations() != null ? request.totalRegistrations() : 0L;
        long costPerRegistration = request.costPerRegistration() != null ? request.costPerRegistration() : 0L;
        long hours = request.hours() != null ? request.hours() : 0L;

        if (studentIds != null && !studentIds.isEmpty()) {
            dashboardBasicInfoRepository.updateAttendanceStats(studentIds, hours);
            attended = (long) studentIds.size();
        }

        basicInformationRepository.updateOrganizerStats(
                request.organizerId(),
                registrations,
                attended,
                registrations * costPerRegistration
        );
    }
}