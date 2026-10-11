package com.event.participationmodule.controller;


import com.event.participationmodule.service.ParticipationAuthService;
import com.event.participationmodule.dto.QRRequest;
import com.event.participationmodule.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final ParticipationAuthService participationAuthService;

    // Student re-requests their own QR (e.g. the registration email never arrived).
    // Identity comes from the JWT, so a user can only trigger a resend for their own registration.
    @PostMapping("/resend-qr/{eventId}")
    public ResponseEntity<String> resendQr(@PathVariable Long eventId,
                                           @RequestAttribute("userId") String userId) throws Exception {
        return ResponseEntity.ok(participationAuthService.resendQr(Long.valueOf(userId), eventId));
    }

    @PostMapping("/mark")
    public ResponseEntity<?> markAttendance(
            @RequestBody QRRequest qrRequest,
            @RequestAttribute(value = "userId", required = false) String userId,
            @RequestAttribute(value = "username", required = false) String username) {

        // Errors are mapped by GlobalExceptionHandler (400 invalid QR / already marked, 403 not allowed to scan).
        // The old catch-all turned every failure, including database errors, into a 400.
        if (qrRequest.getScannedData() == null || qrRequest.getScannedData().trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid QR Data");
        }

        String randomId = qrRequest.getScannedData().trim();
        Long scannerUserId = (userId != null && !userId.isEmpty()) ? Long.valueOf(userId) : null;
        String scannerEmail = (username != null && !username.isEmpty()) ? username : null;

        String result = attendanceService.markAttendance(randomId, scannerUserId, scannerEmail);

        return ResponseEntity.ok(result);
    }
}
