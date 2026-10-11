package com.event.profilemodule.controllers.admin;

import com.event.profilemodule.Services.SocialProfilesService;
import com.event.sharedmodule.OverallCheckAvailability;
import com.event.profilemodule.entity.admin.SocialProfiles;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
public class SocialProfileController {

    private final SocialProfilesService socialProfileService;
    private final OverallCheckAvailability overallCheckAvailability;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add-social")
    public ResponseEntity<SocialProfiles> addSocialProfile(
            @RequestBody SocialProfiles socialProfile) {

        return ResponseEntity.ok(
                socialProfileService.addSocialProfile( socialProfile)
        );
    }

    @GetMapping("/social")
    public ResponseEntity<List<SocialProfiles>> getSocialProfiles() {

        return ResponseEntity.ok(
                socialProfileService.getSocialProfiles()
        );
    }


    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/social-profile")
    public ResponseEntity<Void> deleteSocialProfile(
            @RequestParam("id") Long id
    )
            throws ResourceNotFoundException {

        socialProfileService.deleteSocialProfile(id);
        return ResponseEntity.noContent().build();
    }

}