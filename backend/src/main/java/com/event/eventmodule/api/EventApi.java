package com.event.eventmodule.api;

import com.event.eventmodule.dtoPayload.EventsDto;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.analysis.MonthlyAttendanceCount;

import java.util.List;

public interface EventApi {
    List<EventsDto> getPendingEvents(Long userId);
    List<EventsDto> getPastEvents(Long userId);
    List<MonthlyAttendanceCount> getAttendanceCount(Long userId);
    List<MonthlyAttendanceCount> getRegisteredCount(Long userId);
    void markAttendance(Long userId, Long eventId);
    boolean canScan(Long eventId, String email, Long userId);
    long countAllEvents();
}