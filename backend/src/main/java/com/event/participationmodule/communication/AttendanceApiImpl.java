package com.event.participationmodule.communication;

import com.event.participationmodule.entity.UserRequest;
import com.event.participationmodule.service.ParticipationAuthService;
import org.springframework.stereotype.Service;

@Service
public class AttendanceApiImpl implements AttendanceApi {
    private final ParticipationAuthService participationAuthService;

    public AttendanceApiImpl(ParticipationAuthService participationAuthService) {
        this.participationAuthService = participationAuthService;
    }

    @Override
    public String register(UserRequest userRequest) throws Exception {
        participationAuthService.sendQr(
                userRequest.getEmail(),
                userRequest.getUserId(),
                userRequest.getEventId()
        );
        return "Registered Successfully";
    }
}