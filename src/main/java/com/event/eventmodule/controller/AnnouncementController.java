package com.event.eventmodule.controller;


import com.event.eventmodule.Registration.RegisterRepository;
import com.event.eventmodule.config.CheckAvailability;
import com.event.eventmodule.config.RequiresEventAccess;
import com.event.eventmodule.entity.announcement.Announcement;
import com.event.eventmodule.entity.announcement.AnnouncementRequest;
import com.event.eventmodule.service.events.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
//@RequestMapping
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    private final RegisterRepository registerRepository;
    private final CheckAvailability checkAvailability;

    @PostMapping("/organizer/create/{eventId}")
    @RequiresEventAccess
    public ResponseEntity<?> create(@RequestBody AnnouncementRequest dto, @PathVariable Long eventId) throws AccessDeniedException {

        Set<Long> userIds = new HashSet<>(registerRepository.findUserIdsByEventId(eventId));

        return ResponseEntity.ok(announcementService.createAnnouncement(dto.getTitle(), dto.getMessage(), userIds, eventId));

    }

    @GetMapping("/user/announcement")
    public List<Announcement> getForUser(@RequestAttribute("userId") String userId) {
        return announcementService.getAnnouncementsForUser(Long.valueOf(userId));
    }


}
