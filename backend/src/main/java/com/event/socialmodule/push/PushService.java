package com.event.socialmodule.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends Web Push messages to a user's subscribed devices. Off until VAPID keys are configured
 * (VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY, VAPID_SUBJECT). Guardrails from the retention plan: non-urgent
 * pushes are capped per week and never sent during quiet hours; the bell still has everything.
 */
@Service
@Slf4j
public class PushService {

    static final int WEEKLY_NON_URGENT_CAP = 3;
    static final LocalTime QUIET_FROM = LocalTime.of(22, 0), QUIET_UNTIL = LocalTime.of(8, 0);
    static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final PushSubscriptionRepository subscriptions;
    private final PushSentRepository sentRepository;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ExecutorService sender = Executors.newVirtualThreadPerTaskExecutor();
    private final String publicKey, privateKey, subject;
    private boolean enabled;

    public PushService(PushSubscriptionRepository subscriptions, PushSentRepository sentRepository,
                       @Value("${app.push.vapid-public-key:}") String publicKey,
                       @Value("${app.push.vapid-private-key:}") String privateKey,
                       @Value("${app.push.subject:mailto:admin@campusevents.app}") String subject) {
        this.subscriptions = subscriptions;
        this.sentRepository = sentRepository;
        this.publicKey = publicKey == null ? "" : publicKey.trim();
        this.privateKey = privateKey == null ? "" : privateKey.trim();
        this.subject = subject == null || subject.isBlank() ? "mailto:admin@campusevents.app" : subject.trim();
    }

    @PostConstruct
    void checkKeys() {
        if (publicKey.isEmpty() || privateKey.isEmpty()) {
            log.info("Web push is off: set VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY to turn it on");
            return;
        }
        try {
            WebPushCrypto.validateKeys(publicKey, privateKey);
            enabled = true;
            log.info("Web push is on");
        } catch (Exception e) {
            log.error("Web push is off: the VAPID keys are invalid ({})", e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String publicKey() {
        return enabled ? publicKey : null;
    }

    /**
     * Sends to all of the user's devices in the background. Returns whether it was sent (false when push is
     * off, the user has no devices, or a guardrail held a non-urgent message back).
     */
    public boolean send(Long userId, PushMessage message, boolean urgent) {
        if (!enabled || userId == null) return false;
        List<PushSubscription> devices = subscriptions.findByUserId(userId);
        if (devices.isEmpty()) return false;
        if (!urgent) {
            if (isQuietHours(ZonedDateTime.now(ZONE).toLocalTime())) return false;
            if (sentRepository.countByUserIdAndUrgentFalseAndSentAtAfter(userId, LocalDateTime.now().minusDays(7)) >= WEEKLY_NON_URGENT_CAP) return false;
        }
        sentRepository.save(new PushSent(userId, urgent));
        byte[] payload;
        try {
            payload = json.writeValueAsBytes(message);
        } catch (Exception e) {
            return false;
        }
        for (PushSubscription device : devices) sender.submit(() -> deliver(device, payload, urgent));
        return true;
    }

    static boolean isQuietHours(LocalTime t) {
        return !t.isBefore(QUIET_FROM) || t.isBefore(QUIET_UNTIL);
    }

    private void deliver(PushSubscription device, byte[] payload, boolean urgent) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(device.getEndpoint()))
                    .timeout(Duration.ofSeconds(15))
                    .header("TTL", urgent ? "3600" : "86400")
                    .header("Urgency", urgent ? "high" : "normal")
                    .header("Content-Encoding", "aes128gcm")
                    .header("Content-Type", "application/octet-stream")
                    .header("Authorization", WebPushCrypto.vapidAuthorization(device.getEndpoint(), subject, publicKey, privateKey, Instant.now()))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(WebPushCrypto.encrypt(payload, device.getP256dh(), device.getAuth())))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status == 404 || status == 410) {
                subscriptions.deleteByEndpoint(device.getEndpoint());   // the browser unsubscribed or expired
            } else if (status >= 200 && status < 300) {
                device.setLastSuccessAt(LocalDateTime.now());
                subscriptions.save(device);
            } else {
                log.warn("Push to device {} failed: HTTP {} {}", device.getId(), status, response.body().length() > 200 ? response.body().substring(0, 200) : response.body());
            }
        } catch (Exception e) {
            log.warn("Push to device {} failed: {}", device.getId(), e.toString());
        }
    }
}
