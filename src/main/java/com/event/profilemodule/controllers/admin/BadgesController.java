package com.event.profilemodule.controllers.admin;

import com.event.profilemodule.Services.BadgesService;
import com.event.sharedmodule.OverallCheckAvailability;
import com.event.profilemodule.entity.admin.Badges;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/management")
@RequiredArgsConstructor

public class BadgesController {

    private final BadgesService badgesService;
    private final OverallCheckAvailability overallCheckAvailability;

    // CREATE Badge
    @PostMapping("/create/badge")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Badges> createBadge(@RequestBody Badges badges) throws AccessDeniedException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(badgesService.createBadge(badges));
    }

    // DELETE Badge
    @GetMapping("/delete/badge")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteBadge(@RequestParam("id") Long id) throws ResourceNotFoundException, AccessDeniedException {
        badgesService.deleteBadge(id);
    }

    // UPDATE Badge
    @PostMapping("/update/badge")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Badges> updateBadge(
            @RequestParam(value = "id", required = false) Long id,
            @RequestBody Badges badges
    ) throws ResourceNotFoundException, AccessDeniedException {
        // Previously used the admin's userId as the badge id, updating an unrelated badge
        Long targetId = id != null ? id : badges.getId();
        if (targetId == null) throw new IllegalArgumentException("Badge id is required");
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(badgesService.updateBadge(targetId, badges));
    }

    // GET Single Badge
    @GetMapping("/badge")
    public ResponseEntity<Badges> getBadge(@RequestAttribute("userId") String userId)
            throws ResourceNotFoundException {
        return ResponseEntity.status(HttpStatus.OK)
                .body(badgesService.getBadge(Long.valueOf(userId)));
    }

    @GetMapping("/all-badges")
    public ResponseEntity<List<Badges>> getAllBadges() {
        return ResponseEntity.ok(badgesService.getAllBadges());
    }
}
