package com.event.profilemodule.profile_controller;


import com.event.profilemodule.Repository.PreferencesRepository;
import com.event.profilemodule.Services.StudentService;
import com.event.profilemodule.entity.admin.Preferences;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
public class VolunteerCon {

    private final StudentService studentService;
    private final PreferencesRepository preferencesRepository;

    @PostMapping("/preferences")
    public Set<Preferences> updateVolunteerPreferences(
            @RequestAttribute("userId") String userId,
            @RequestBody Set<Long> preferenceIds
    ) {
        return studentService.updateVolunteerPreferences(Long.valueOf(userId), preferenceIds);
    }

    @GetMapping("/preferences")
    public Set<Preferences> getStudentPreferences(
            @RequestAttribute("userId") String userId
    ) {
        return studentService.getAllPreference(Long.valueOf(userId));
    }

    @GetMapping("/all-preferences")
    public List<Preferences> getAllAvailablePreferences() {
        return preferencesRepository.findAll();
    }
}
