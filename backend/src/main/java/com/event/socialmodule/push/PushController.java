package com.event.socialmodule.push;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

/** Phone notifications: the server's public key, and subscribing/unsubscribing this device. */
@RestController
@RequiredArgsConstructor
public class PushController {

    private final PushService pushService;
    private final PushSubscriptionRepository subscriptions;
    private final org.springframework.context.ApplicationEventPublisher activity;

    public record Keys(String p256dh, String auth) {}

    public record SubscriptionBody(String endpoint, Keys keys) {}

    public record EndpointBody(String endpoint) {}

    @GetMapping("/public/push/key")
    public Map<String, Object> key() {
        return pushService.isEnabled() ? Map.of("enabled", true, "publicKey", pushService.publicKey()) : Map.of("enabled", false);
    }

    /** Saves (or moves to this user) the device's subscription. */
    @PostMapping("/social/push/subscription")
    @Transactional
    public ResponseEntity<Void> subscribe(@RequestAttribute("userId") String userId, @RequestBody SubscriptionBody body) {
        if (!pushService.isEnabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Phone notifications aren't set up on this server yet");
        if (body == null || body.endpoint() == null || body.keys() == null || body.keys().p256dh() == null || body.keys().auth() == null
                || !body.endpoint().startsWith("https://") || body.endpoint().length() > 1000
                || body.keys().p256dh().length() > 200 || body.keys().auth().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid push subscription");
        }
        try {
            WebPushCrypto.publicKey(WebPushCrypto.decode(body.keys().p256dh()));
            if (WebPushCrypto.decode(body.keys().auth()).length != 16) throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid push subscription keys");
        }
        PushSubscription s = subscriptions.findByEndpoint(body.endpoint()).orElseGet(PushSubscription::new);
        s.setUserId(Long.valueOf(userId));   // a shared device follows whoever subscribed last
        s.setEndpoint(body.endpoint());
        s.setP256dh(body.keys().p256dh());
        s.setAuth(body.keys().auth());
        if (s.getCreatedAt() == null) {
            s.setCreatedAt(LocalDateTime.now());
            activity.publishEvent(new com.event.sharedmodule.UserActivityEvent(Long.valueOf(userId), "push_enabled", null));
        }
        subscriptions.save(s);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/social/push/subscription")
    @Transactional
    public ResponseEntity<Void> unsubscribe(@RequestAttribute("userId") String userId, @RequestBody EndpointBody body) {
        if (body != null && body.endpoint() != null) {
            subscriptions.findByEndpoint(body.endpoint()).filter(s -> s.getUserId().equals(Long.valueOf(userId))).ifPresent(s -> {
                subscriptions.delete(s);
                activity.publishEvent(new com.event.sharedmodule.UserActivityEvent(Long.valueOf(userId), "push_disabled", null));
            });
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/social/push/test")
    public Map<String, Object> test(@RequestAttribute("userId") String userId) {
        boolean sent = pushService.send(Long.valueOf(userId),
                new PushMessage("Notifications are on", "You'll hear about friend requests, seats and event reminders here.", "/settings", "test"), true);
        return Map.of("sent", sent);
    }
}
