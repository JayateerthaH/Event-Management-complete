package com.event.eventmodule.service;


import com.event.eventmodule.Registration.RegisterRepository;
import com.event.eventmodule.entity.analysis.MonthlyAttendanceCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

public class AnalyticsService {

    private final RegisterRepository registerRepository;

    public List<MonthlyAttendanceCount> getUserAttendanceAnalytics(Long userId) {
        List<MonthlyAttendanceCount> result = registerRepository.getUserAttendanceLast6Months(userId);

        return result.stream()
                .map(r -> new MonthlyAttendanceCount(r.getMonth(), r.getTotalEvents()))
                .toList();
    }

    public List<MonthlyAttendanceCount> getOrganizerAttendanceAnalytics(Long userId) {
        List<MonthlyAttendanceCount> result = registerRepository.getOrganizerAttendanceLast6Months(userId);

        return result.stream()
                .map(r -> new MonthlyAttendanceCount(r.getMonth(), r.getTotalEvents()))
                .toList();
    }
}
