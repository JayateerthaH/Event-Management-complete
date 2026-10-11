package com.event.notificationmodule;

public interface NotificationApi {

    void sendQRMail(String to, String subject, String body, String base64Qr);

    void sendEmail(String to, String subject, String body);

    // Low-level method for custom payloads if needed
    void sendRawEmail(java.util.Map<String, Object> payload);
}