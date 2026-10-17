package com.event.participationmodule.communication;

import com.event.participationmodule.entity.UserRequest;

public interface AttendanceApi {
    String register(UserRequest userRequest) throws Exception;

    /** Whether the user has been checked in (QR scanned) at the event. */
    boolean hasAttended(Long userId, Long eventId);

    /** Removes the user's QR ticket for the event (when they release their seat). */
    void cancel(Long userId, Long eventId);
}