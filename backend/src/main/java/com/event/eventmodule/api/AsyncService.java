package com.event.eventmodule.api;

import com.event.analyticsmodule.communication.DashboardApi;
import com.event.analyticsmodule.communication.DashboardUpdateRequest;
import com.event.eventmodule.entity.config.BasicUpdateDto;
import com.event.eventmodule.entity.config.UserRequest;
import com.event.participationmodule.communication.AttendanceApi;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncService {

    private final DashboardApi dashboardApi;   // depend on the module's interface, not its implementation
    private final ModelMapper modelMapper;
    private final AttendanceApi attendanceApi;

    public AsyncService(@Lazy DashboardApi dashboardApi, ModelMapper modelMapper, AttendanceApi attendanceApi) {
        this.dashboardApi = dashboardApi;
        this.modelMapper = modelMapper;
        this.attendanceApi = attendanceApi;
    }

    @Async
    public void sendNotificationAsync(UserRequest request) throws Exception {
        attendanceApi.register(modelMapper.map(request,com.event.participationmodule.entity.UserRequest.class));
    }

    public void sendUpdateAsync(BasicUpdateDto basicUpdateDto){
        dashboardApi.updateStatsOnEventCompletion(new DashboardUpdateRequest(
                basicUpdateDto.getStudentIds(),
                basicUpdateDto.getOrganizerId(),
                basicUpdateDto.getHours(),
                basicUpdateDto.getTotalRegistrations(),
                basicUpdateDto.getCostPerRegistration()
        ));
    }
}
