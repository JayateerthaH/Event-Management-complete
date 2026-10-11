package com.event.eventmodule.config;

public interface EventAccessService {
    boolean isAllowed(Long eventId, String userId, String rolesHeader, String userEmail);
}