package com.event.participationmodule.service;

import com.event.eventmodule.api.EventApi;
import com.event.participationmodule.entity.UserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttendanceNotificationAsyncService {

    // Talks to the event module directly instead of going back through AttendanceService,
    // which removes the AttendanceService <-> AttendanceNotificationAsyncService cycle (and its @Lazy).
    private final EventApi eventApi;

    @Async
    public void sendNotificationAsync(UserRequest request) {
        eventApi.markAttendance(request.getUserId(), request.getEventId());
    }
}
