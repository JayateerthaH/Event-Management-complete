package com.event.sharedmodule;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Keep-alive for cron-job.org: public, no database access, tiny response, never cached.
 * Point the cron job at https://<render-service>/public/ping every 5-10 minutes.
 */
@RestController
public class PingController {

    @GetMapping("/public/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("status", "ok", "time", Instant.now().toString()));
    }
}
