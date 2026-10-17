package com.event.socialmodule.digest;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mondays 9:00 IST; admins can also send this week's digest now (it never goes twice in a week). */
@RestController
@RequiredArgsConstructor
public class DigestScheduler {

    private final DigestService digestService;

    @Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Kolkata")
    public void weekly() {
        digestService.runWeekly();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/digest/run")
    public DigestService.RunResult runNow() {
        return digestService.runWeekly();
    }
}
