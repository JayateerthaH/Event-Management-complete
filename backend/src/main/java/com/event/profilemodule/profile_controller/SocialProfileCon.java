package com.event.profilemodule.profile_controller;

import com.event.profilemodule.Services.SocialProfilesService;
import com.event.profilemodule.Services.StudentService;
import com.event.profilemodule.entity.student.SocialProfileUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
public class SocialProfileCon {

    private final StudentService studentService;
    private final SocialProfilesService socialProfilesService;


    @PostMapping("/profile")
    public Set<SocialProfileUpdate> updateStudentSocialProfiles(@RequestAttribute("userId") String userId,
                                                                @RequestBody Map<Long,String> map){
        return studentService.updateSocialProfiles(Long.valueOf(userId),map);
    }

    @GetMapping("/profile")
    public Set<SocialProfileUpdate> getUserProfiles(@RequestAttribute("userId") String userId) {
        return studentService.getAllSocialProfiles(Long.valueOf(userId));
    }

}

