
package com.event.participationmodule.service;

import org.springframework.security.access.AccessDeniedException;
import com.event.eventmodule.api.EventApi;
import com.event.participationmodule.Repository.AttendanceRepository;
import com.event.participationmodule.Repository.UserRepository;
import com.event.participationmodule.Repository.UserRequestRepository;
import com.event.participationmodule.entity.Attendance;
import com.event.participationmodule.entity.UserRequest;
import com.event.participationmodule.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final org.springframework.context.ApplicationEventPublisher activityPublisher;
    private final UserRepository userRepository;
    private final UserRequestRepository userRequestRepository;
    private final AttendanceNotificationAsyncService attendanceNotificationAsyncService;
    private final EventApi eventApi;

    @Transactional
    public String markAttendance(String randomId, Long scannerUserId, String scannerEmail) {

        // 1️⃣ Validate randomId and fetch user
        Users user = userRepository.findByRandomId(randomId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR Code! User not found for id: " + randomId));

        String email = user.getEmail();
        Long eventId = user.getEventId();

        // 2️⃣ Verify the scanner is a collaborator or organizer for this event
        if (scannerUserId == null || scannerEmail == null) {
            throw new IllegalArgumentException("Scanner identity is required to mark attendance!");
        }
        if (!eventApi.canScan(eventId, scannerEmail, scannerUserId)) {
            throw new AccessDeniedException("You are not authorized to scan attendance for this event!");
        }

        // 3️⃣ Fetch user request using email + eventId
        UserRequest userRequest = userRequestRepository.findByEmailAndEventId(email, eventId)
                .orElseThrow(() -> new IllegalArgumentException("User not registered for this event!"));

        // 4️⃣ Check if already attended
        if (Boolean.TRUE.equals(userRequest.getIsAttended())) {
            throw new IllegalArgumentException("Attendance already marked!");
        }

        LocalDate today = LocalDate.now();

        // 5️⃣ Check attendance exists for today same people do not show again
        if (attendanceRepository.existsByEmailAndEventIdAndDate(email, eventId, today)) {
            throw new IllegalArgumentException("Attendance already marked today!");
        }

        // 6️⃣ Create attendance entry
        Attendance attendance = new Attendance(
                email,
                eventId,
                today,
                LocalDateTime.now()
        );

        attendanceRepository.save(attendance);

        // 7️⃣ Update UserRequest as attended
        userRequest.setIsAttended(true);
        userRequestRepository.save(userRequest);

        attendanceNotificationAsyncService.sendNotificationAsync(userRequest);
        activityPublisher.publishEvent(new com.event.sharedmodule.UserActivityEvent(userRequest.getUserId(), "attended", eventId));

        return "Attendance marked successfully for " + email + " for event " + eventId;
    }

}
