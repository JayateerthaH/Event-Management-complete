package com.event.profilemodule.profile_controller;

import com.event.profilemodule.Repository.InterestsRepository;
import com.event.profilemodule.Services.StudentService;
import com.event.profilemodule.entity.admin.Interests;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
public class InterestsCon {

    private final StudentService studentService;
    private final InterestsRepository interestsRepository;

    @PostMapping("/interests")
    public Set<Interests> updateStudentInterests(
            @RequestAttribute("userId") String userId,
            @RequestBody Set<Long> interestIds
    ) {
        return studentService.updateStudentInterests(Long.valueOf(userId), interestIds);
    }

    @GetMapping("/interests")
    public Set<Interests> updateStudentInterests(
            @RequestAttribute("userId") String userId
    ) {
        return studentService.getAllInterests(Long.valueOf(userId));
    }

    @GetMapping("/all-interests")
    public java.util.List<Interests> getAllAvailableInterests() {
        return interestsRepository.findAll();
    }

}
