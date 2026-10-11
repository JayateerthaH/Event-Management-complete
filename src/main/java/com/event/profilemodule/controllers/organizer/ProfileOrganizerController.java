package com.event.profilemodule.controllers.organizer;


import com.event.profilemodule.Services.StudentService;
import com.event.sharedmodule.OverallCheckAvailability;
import com.event.profilemodule.entity.student.ProfileStudent;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
public class ProfileOrganizerController {

    private final StudentService studentService;

    @GetMapping("/filter/{preference}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public List<ProfileStudent> filterByPreference(@PathVariable String preference) throws AccessDeniedException {
        return studentService.getStudentsByPreference(preference);
    }



}
