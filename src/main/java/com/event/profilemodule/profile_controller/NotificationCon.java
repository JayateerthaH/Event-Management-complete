package com.event.profilemodule.profile_controller;

import com.event.profilemodule.Repository.NotificationsRepository;
import com.event.profilemodule.Services.StudentService;
import com.event.profilemodule.entity.admin.Notifications;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
public class NotificationCon {

    private final StudentService studentService;
    private final NotificationsRepository notificationsRepository;

    @PostMapping("/notifications")
    public Set<Notifications> updateStudentNotifications(
            @RequestAttribute("userId") String userId,
            @RequestBody Set<Long> notificationIds
    ) {
        return studentService.updateNotificationType(Long.valueOf(userId), notificationIds);
    }

    @GetMapping("/notifications")
    public Set<Notifications> getStudentNotifications(
            @RequestAttribute("userId") String userId
    ) {
        return studentService.getNotificationType(Long.valueOf(userId));
    }

    @GetMapping("/all-notifications")
    public List<Notifications> getAllAvailableNotifications() {
        return notificationsRepository.findAll();
    }
}
