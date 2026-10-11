package com.event.notificationmodule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
class BrevoEmailService implements NotificationApi {

    @Value("${brevo.sender-email}")
    private String senderEmail;

    @Value("${brevo.sender-name}")
    private String senderName;

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @Value("${brevo.url}") // https://api.brevo.com/v3/smtp/email
    private String brevoUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendQRMail(String to, String subject, String body, String base64Qr) {
        Map<String, Object> attachment = Map.of(
                "name", "qr.png",
                "content", base64Qr
        );

        Map<String, Object> payload = Map.of(
                "sender", Map.of("email", senderEmail, "name", senderName),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", "<html><body><p>" + body + "</p></body></html>",
                "attachment", List.of(attachment)
        );

        this.sendRawEmail(payload);
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        Map<String, Object> payload = Map.of(
                "sender", Map.of("email", senderEmail, "name", senderName),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", "<html><body><p>" + body + "</p></body></html>"
        );
        this.sendRawEmail(payload);
    }

    @Override
    public void sendRawEmail(Map<String, Object> payload) {
        log.info("Executing Brevo SMTP call to: {}", payload.get("to"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForEntity(brevoUrl, request, String.class);
        } catch (RestClientException e) {
            // Surface the failure instead of swallowing it, so callers can retry or report it
            throw new EmailDeliveryException("Failed to send email via Brevo to " + payload.get("to"), e);
        }
    }
}