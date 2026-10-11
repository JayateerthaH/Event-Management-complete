package com.event.notificationmodule;


import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Sends arbitrary mail from the app's sender account, so it must not be open to every user.
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/internal/email")
@RequiredArgsConstructor
public class BrevoEmailController {

    private final NotificationApi notificationApi; // Use the Interface!

    @PostMapping("/send-qr")
    public void sendQRMail(@RequestBody EmailRequestDTO request) {
        notificationApi.sendQRMail(
                request.getTo(),
                request.getSubject(),
                request.getBody(),
                request.getBase64Qr()
        );
    }

    @PostMapping("/send-email")
    public void sendEmail(@RequestBody EmailRequestDTO request) {
        notificationApi.sendEmail(
                request.getTo(),
                request.getSubject(),
                request.getBody()
        );
    }
}