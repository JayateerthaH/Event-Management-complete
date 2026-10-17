package com.event.authmodule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On startup, tidies role records left by older versions or by promoting accounts outside the app: removes
 * student records of organizers/admins and requests for roles accounts already have. Idempotent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoleRecordCleanup implements ApplicationRunner {

    private final AdminService adminService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int students = adminService.removeStaleStudentRecords();
            int pending = adminService.pendingRequests().size();
            log.info("Role records checked: removed {} stale student record(s); {} request(s) pending", students, pending);
        } catch (RuntimeException e) {
            log.warn("Role record cleanup failed: {}", e.getMessage());
        }
    }
}
