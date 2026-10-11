package com.event.eventmodule.controller.home;

import com.event.eventmodule.entity.home.BasicInfo;
import com.event.eventmodule.service.home.BasicInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/home/basic-info")
@RequiredArgsConstructor
public class BasicInfoController {

    private final BasicInfoService basicInfoService;

    // 🏠 HOME PAGE API (VERY FAST)
    @GetMapping
    public ResponseEntity<BasicInfo> getBasicInfo() {
        return ResponseEntity.ok(basicInfoService.getBasicInfo());
    }

    // ➕ Increment when event is created
    @PostMapping("admin/event")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> incrementEvents() {
        basicInfoService.incrementEvents();
        return ResponseEntity.ok("Event count incremented");
    }

    // ➕ Increment when student registers
    @PostMapping("admin/student")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> incrementStudents() {
        basicInfoService.incrementStudents();
        return ResponseEntity.ok("Student count incremented");
    }

    // ➕ Increment when college is added
    @PostMapping("admin/college")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> incrementColleges() {
        basicInfoService.incrementCollegeConnected();
        return ResponseEntity.ok("College count incremented");
    }
}
