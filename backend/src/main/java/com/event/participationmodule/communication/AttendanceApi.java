package com.event.participationmodule.communication;

import com.event.participationmodule.entity.UserRequest;

public interface AttendanceApi {
    String register(UserRequest userRequest) throws Exception;
}