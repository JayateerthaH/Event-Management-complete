package com.event.socialmodule.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/social/notification-settings")
@RequiredArgsConstructor
public class NotificationPrefsController {

    private final NotificationPrefsService service;

    @GetMapping
    public NotificationPrefsService.PrefsView get(@RequestAttribute("userId") String userId) {
        return service.get(Long.valueOf(userId));
    }

    @PutMapping
    public NotificationPrefsService.PrefsView update(@RequestAttribute("userId") String userId,
                                                     @RequestBody NotificationPrefsService.PrefsUpdate body) {
        return service.update(Long.valueOf(userId), body);
    }
}
