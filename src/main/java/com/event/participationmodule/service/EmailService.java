package com.event.participationmodule.service;

import com.event.notificationmodule.NotificationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final NotificationApi notificationApi;

    public void sendQRMail(String to, String subject, String body, byte[] qrPng) {
        String base64Qr = Base64.getEncoder().encodeToString(qrPng);
        notificationApi.sendQRMail(to, subject, body, base64Qr);
    }
}
