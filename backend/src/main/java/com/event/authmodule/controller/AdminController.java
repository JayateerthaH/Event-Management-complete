package com.event.authmodule.controller;

import com.event.authmodule.dtoPayload.*;
import com.event.authmodule.dtoPayload.OrganizerDto;
import com.event.authmodule.dtoPayload.StudentDto;
import com.event.authmodule.entitys.OnBoard;
import com.event.authmodule.repository.OnBoardDtoRepository;
import com.event.authmodule.repository.OrganizerRepository;
import com.event.authmodule.repository.StudentRepository;
import com.event.authmodule.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final StudentRepository studentRepository;
    private final OnBoardDtoRepository onBoardDtoRepository;
    private final OrganizerRepository organizerRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/onBoardNewOrganizer")
    public ResponseEntity<OrganizerResponseDto> onBoardOrganizer(@RequestBody OnBoardOrganizerRequestDto onBoardOrganizerRequestDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.onBoardOrganizer(onBoardOrganizerRequestDto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/onBoardNewAdmin")
    public ResponseEntity<AdminResponseDto> onBoardAdmin(@RequestBody OnBoardAdminRequestDto onBoardAdminRequestDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.onBoardAdmin(onBoardAdminRequestDto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/overview")
    public AdminOverviewDto getOverview(){
        return adminService.getOverview();
    }

    // Users whose email or profile name contains q, each once with all their roles
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public List<AdminUserDto> searchUsers(@RequestParam(defaultValue = "") String q) {
        return adminService.searchUsers(q);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/onboard")
    public List<OnBoard> getAllOnBoards(){
        return adminService.pendingRequests();
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/students")
    public List<StudentDto> getAllStudents(){
        return studentRepository.findAll()
                .stream()
                .map(student -> {
                    StudentDto dto = new StudentDto();
                    dto.setId(student.getId());
                    dto.setName(student.getName());
                    dto.setEmail(student.getEmail());
                    return dto;
                })
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/organizer")
    public List<OrganizerDto> getAllOrganizer(){
        return organizerRepository.findAll()
                .stream()
                .map(organizer -> {
                    OrganizerDto dto = new OrganizerDto();
                    dto.setId(organizer.getId());
                    dto.setName(organizer.getName());
                    dto.setEmail(
                            organizer.getUserInfo() != null
                                    ? organizer.getUserInfo().getUsername()
                                    : null
                    );
                    return dto;
                })
                .toList();
    }
}
