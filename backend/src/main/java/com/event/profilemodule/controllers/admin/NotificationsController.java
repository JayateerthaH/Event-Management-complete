package com.event.profilemodule.controllers.admin;

import com.event.profilemodule.Services.NotificationsService;
import com.event.sharedmodule.OverallCheckAvailability;
import com.event.profilemodule.entity.admin.Notifications;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
public class NotificationsController {
    private final NotificationsService notificationsService;
    private final OverallCheckAvailability overallCheckAvailability;

    // CREATE Notification
    @PostMapping("/create/notification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Notifications> createNotification(@RequestBody Notifications notifications) throws AccessDeniedException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationsService.createNotification(notifications));
    }

    // DELETE Notification
    @GetMapping("/delete/notification")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteNotification(@RequestParam("id") Long id) throws ResourceNotFoundException, AccessDeniedException {
        notificationsService.deleteNotification(id);
    }

    // GET Single Notification
    @GetMapping("/notification")
    public ResponseEntity<Notifications> getNotification(@RequestAttribute("userId") String userId)
            throws ResourceNotFoundException {
        return ResponseEntity.status(HttpStatus.OK)
                .body(notificationsService.getNotification(Long.valueOf(userId)));
    }
}
