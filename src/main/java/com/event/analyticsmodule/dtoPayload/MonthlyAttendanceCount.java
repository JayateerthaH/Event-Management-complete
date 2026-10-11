package com.event.analyticsmodule.dtoPayload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
@Setter
public class MonthlyAttendanceCount{
    private final String month;
    private final Long totalEvents;
}

