package com.event.profilemodule.profile_controller;

import com.event.profilemodule.Repository.StudentProfileRepository;
import com.event.profilemodule.Services.StudentService;
import com.event.profilemodule.entity.student.ProfileStudent;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
public class PersonalInfoCon {

    private final StudentService studentService;
    private final StudentProfileRepository studentProfileRepository;
    private final ModelMapper modelMapper;

    @GetMapping("/students")
    public ResponseEntity<ProfileStudent> getStudentByUserId(@RequestAttribute("userId") String userId) {

        // Convert String ID to Long
        Long id = Long.valueOf(userId);

        // Call Service
        ProfileStudent student = studentService.getStudent(id);

        return ResponseEntity.ok(student);
    }

    @PostMapping("/students")
    public ResponseEntity<ProfileStudent> updateStudent(
            @RequestAttribute("userId") String userId,
            @RequestBody ProfileStudent studentRequest
    ) {
        System.out.println("RECEIVED BIO: " + studentRequest.getBio());
        ProfileStudent updated = studentService.updateStudent(Long.valueOf(userId), studentRequest);
        return ResponseEntity.ok(updated);
    }

}
