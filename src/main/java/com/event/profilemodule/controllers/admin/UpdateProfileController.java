package com.event.profilemodule.controllers.admin;

import com.event.profilemodule.Services.InterestsService;
import com.event.profilemodule.Services.NotificationsService;
import com.event.profilemodule.Services.PreferencesService;
import com.event.profilemodule.Services.SocialProfilesService;
import com.event.sharedmodule.OverallCheckAvailability;
import com.event.profilemodule.entity.admin.Interests;
import com.event.profilemodule.entity.admin.Notifications;
import com.event.profilemodule.entity.admin.Preferences;
import com.event.profilemodule.entity.admin.SocialProfiles;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/management")
public class UpdateProfileController {

    private final SocialProfilesService socialProfilesService;
    private final NotificationsService notificationsService;
    private final InterestsService interestsService;
    private final PreferencesService preferencesService;
    private final OverallCheckAvailability overallCheckAvailability;

    private static Long requireId(Long id) {
        if (id == null) throw new IllegalArgumentException("id is required");
        return id;
    }

    // UPDATE Notification
    @PostMapping("/update/notification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Notifications> updateNotification(
            @RequestAttribute("userId") String userId,
            @RequestParam(value = "id", required = false) Long id,
            @RequestBody Notifications notifications
    ) throws ResourceNotFoundException, AccessDeniedException {
        Long targetId = requireId(id != null ? id : notifications.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(notificationsService.updateNotification(targetId, notifications));
    }

    @PostMapping("/update/social-profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SocialProfiles> updateSocialProfile(
            @RequestAttribute("userId") String userId,
            @RequestParam(value = "id", required = false) Long id,
            @RequestBody SocialProfiles socialProfile
    ) throws ResourceNotFoundException{
        Long targetId = requireId(id != null ? id : socialProfile.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(socialProfilesService.updateSocialProfile(targetId, socialProfile));
    }

    // UPDATE Interest
    @PostMapping("/update/interest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Interests> updateInterest(
            @RequestAttribute("userId") String userId,
            @RequestParam(value = "id", required = false) Long id,
            @RequestBody Interests interest
    ) throws ResourceNotFoundException, AccessDeniedException {
        Long targetId = requireId(id != null ? id : interest.getInterestId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(interestsService.updateInterest(targetId, interest));
    }

    @PostMapping("/update/preference")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Preferences> updatePreferences(@RequestAttribute("userId") String userId, @RequestParam(value = "id", required = false) Long id, @RequestBody Preferences preferences) throws ResourceNotFoundException, AccessDeniedException {
        Long targetId = requireId(id != null ? id : preferences.getPreferenceId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(preferencesService.updatePreference(targetId,preferences));
    }

}
