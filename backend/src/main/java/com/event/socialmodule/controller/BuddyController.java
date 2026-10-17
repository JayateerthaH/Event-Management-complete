package com.event.socialmodule.controller;

import com.event.socialmodule.service.BuddyService;
import com.event.socialmodule.service.BuddyService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Event buddies, friends, people search by interests, blocking/reporting and friend notifications. */
@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
public class BuddyController {

    private final BuddyService buddyService;

    public record SayHi(Long userId, Long eventId) {}
    public record ReportBody(Long userId, String reason) {}

    @GetMapping("/settings")
    public SettingsView settings(@RequestAttribute("userId") String userId) {
        return buddyService.settings(id(userId));
    }

    @PutMapping("/settings")
    public SettingsView updateSettings(@RequestAttribute("userId") String userId, @RequestBody SettingsUpdate body) {
        return buddyService.updateSettings(id(userId), body);
    }

    @GetMapping("/interests")
    public List<String> interests() {
        return buddyService.allInterests();
    }

    @GetMapping("/events/{eventId}/buddies")
    public EventBuddies eventBuddies(@RequestAttribute("userId") String userId, @PathVariable Long eventId) {
        return buddyService.eventBuddies(id(userId), eventId);
    }

    @PostMapping("/events/{eventId}/opt-in")
    public EventBuddies optIn(@RequestAttribute("userId") String userId, @PathVariable Long eventId) {
        return buddyService.optIn(id(userId), eventId);
    }

    @DeleteMapping("/events/{eventId}/opt-in")
    public EventBuddies optOut(@RequestAttribute("userId") String userId, @PathVariable Long eventId) {
        return buddyService.optOut(id(userId), eventId);
    }

    @GetMapping("/search")
    public List<Person> search(@RequestAttribute("userId") String userId,
                               @RequestParam(required = false) String q,
                               @RequestParam(required = false) List<String> interest) {
        return buddyService.search(id(userId), q, interest);
    }

    @GetMapping("/requests")
    public Requests requests(@RequestAttribute("userId") String userId) {
        return buddyService.requests(id(userId));
    }

    @PostMapping("/requests")
    public Person sayHi(@RequestAttribute("userId") String userId, @RequestBody SayHi body) {
        return buddyService.sendRequest(id(userId), body.userId(), body.eventId());
    }

    @PostMapping("/requests/{requestId}/accept")
    public Person accept(@RequestAttribute("userId") String userId, @PathVariable Long requestId) {
        return buddyService.accept(id(userId), requestId);
    }

    @PostMapping("/requests/{requestId}/ignore")
    public ResponseEntity<Void> ignore(@RequestAttribute("userId") String userId, @PathVariable Long requestId) {
        buddyService.ignore(id(userId), requestId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<Void> cancel(@RequestAttribute("userId") String userId, @PathVariable Long requestId) {
        buddyService.cancel(id(userId), requestId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/friends")
    public List<FriendView> friends(@RequestAttribute("userId") String userId) {
        return buddyService.friends(id(userId));
    }

    @DeleteMapping("/friends/{otherId}")
    public ResponseEntity<Void> removeFriend(@RequestAttribute("userId") String userId, @PathVariable Long otherId) {
        buddyService.removeFriend(id(userId), otherId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blocks")
    public List<Person> blocked(@RequestAttribute("userId") String userId) {
        return buddyService.blocked(id(userId));
    }

    @PostMapping("/blocks/{otherId}")
    public ResponseEntity<Void> block(@RequestAttribute("userId") String userId, @PathVariable Long otherId) {
        buddyService.block(id(userId), otherId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/blocks/{otherId}")
    public ResponseEntity<Void> unblock(@RequestAttribute("userId") String userId, @PathVariable Long otherId) {
        buddyService.unblock(id(userId), otherId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reports")
    public Map<String, String> report(@RequestAttribute("userId") String userId, @RequestBody ReportBody body) {
        buddyService.report(id(userId), body.userId(), body.reason());
        return Map.of("message", "Thanks. An admin will review it, and this student is now blocked.");
    }

    @GetMapping("/notifications")
    public List<NotificationView> notifications(@RequestAttribute("userId") String userId) {
        return buddyService.notifications(id(userId));
    }

    @PostMapping("/notifications/read")
    public ResponseEntity<Void> markRead(@RequestAttribute("userId") String userId) {
        buddyService.markNotificationsRead(id(userId));
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/reports")
    public List<ReportView> reports() {
        return buddyService.reports();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/reports/{reportId}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable Long reportId) {
        buddyService.resolveReport(reportId);
        return ResponseEntity.noContent().build();
    }

    private static Long id(String userId) {
        return Long.valueOf(userId);
    }
}
