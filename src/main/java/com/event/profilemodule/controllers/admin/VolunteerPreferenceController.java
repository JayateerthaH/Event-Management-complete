package com.event.profilemodule.controllers.admin;

import com.event.profilemodule.Services.PreferencesService;
import com.event.profilemodule.entity.admin.Preferences;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
public class VolunteerPreferenceController {

    private final PreferencesService preferencesService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create/preference")
    public ResponseEntity<Preferences> createPreferences(@RequestBody Preferences preferences){
        return ResponseEntity.status(HttpStatus.CREATED).body(preferencesService.createPreference(preferences));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/delete/preference")
    public void deletePreferences(@RequestParam("id") Long id) throws ResourceNotFoundException {
        preferencesService.deletePreference(id);
    }

    @GetMapping("/preference")
    public ResponseEntity<Preferences> getPreferences(@RequestAttribute("userId") String userId) throws ResourceNotFoundException {
        return ResponseEntity.status(HttpStatus.OK).body(preferencesService.getPreference(Long.valueOf(userId)));
    }
}
