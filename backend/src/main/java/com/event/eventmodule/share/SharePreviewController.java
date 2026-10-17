package com.event.eventmodule.share;

import com.event.eventmodule.entity.Events;
import com.event.eventmodule.repository.EventsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.event.sharedmodule.UserActivityEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Link previews for WhatsApp, Telegram, Facebook, LinkedIn, Slack, X...: those crawlers don't run
 * JavaScript, so vercel.json sends their requests for /events/{id} here, and this page carries the
 * event's Open Graph tags. People who open it are sent on to the app.
 */
@RestController
public class SharePreviewController {

    static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE d MMM yyyy, h:mm a", Locale.ENGLISH)
            .withZone(ZoneId.of("Asia/Kolkata"));

    private final EventsRepository eventsRepository;
    private final String frontendUrl;
    private ApplicationEventPublisher activityPublisher;

    public SharePreviewController(EventsRepository eventsRepository, @Value("${app.frontend-url}") String frontendUrl) {
        this.eventsRepository = eventsRepository;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @org.springframework.beans.factory.annotation.Autowired
    void setActivityPublisher(ApplicationEventPublisher activityPublisher) {
        this.activityPublisher = activityPublisher;
    }

    /** Counts a share (for engagement analytics); the share itself happens on the device. */
    @PostMapping("/events/{eventId:\\d+}/shared")
    public ResponseEntity<Void> shared(@PathVariable Long eventId, @RequestAttribute("userId") String userId) {
        if (activityPublisher != null) activityPublisher.publishEvent(new UserActivityEvent(Long.valueOf(userId), "event_shared", eventId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/public/share/events/{eventId:\\d+}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> eventPreview(@PathVariable Long eventId) {
        String url = frontendUrl + "/events/" + eventId;
        Events e = eventsRepository.findById(eventId).filter(ev -> Boolean.TRUE.equals(ev.getIsApproved())).orElse(null);
        String title = e == null ? "CampusEvents" : e.getName();
        String description = e == null ? "Discover and register for campus events." : describe(e);
        String image = e == null ? null : previewImage(e.getImageUrl());
        if (image == null) image = frontendUrl + "/og-default.jpg";

        String html = """
                <!doctype html>
                <html lang="en"><head>
                <meta charset="utf-8">
                <title>%1$s · CampusEvents</title>
                <meta name="description" content="%2$s">
                <meta property="og:type" content="website">
                <meta property="og:site_name" content="CampusEvents">
                <meta property="og:title" content="%1$s">
                <meta property="og:description" content="%2$s">
                <meta property="og:url" content="%3$s">
                <meta property="og:image" content="%4$s">
                <meta property="og:image:width" content="1200">
                <meta property="og:image:height" content="630">
                <meta name="twitter:card" content="summary_large_image">
                <link rel="canonical" href="%3$s">
                <meta http-equiv="refresh" content="0; url=%3$s">
                </head><body><p><a href="%3$s">%1$s</a></p><p>%2$s</p></body></html>
                """.formatted(esc(title), esc(description), esc(url), esc(image));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    static String describe(Events e) {
        StringBuilder s = new StringBuilder();
        if (e.getEventDate() != null) s.append(WHEN.format(e.getEventDate().toInstant()));
        if (e.getVenue() != null && !e.getVenue().isBlank()) s.append(s.isEmpty() ? "" : " · ").append(e.getVenue().trim());
        String cost = e.getCostOfRegistration();
        if (cost != null && (cost.isBlank() || cost.trim().equals("0") || cost.trim().equalsIgnoreCase("free"))) {
            s.append(s.isEmpty() ? "" : " · ").append("Free");
        }
        String about = e.getDescription() == null ? "" : e.getDescription().replaceAll("\\s+", " ").trim();
        if (!about.isEmpty()) {
            if (about.length() > 160) about = about.substring(0, 157).trim() + "...";
            s.append(s.isEmpty() ? "" : ". ").append(about);
        }
        return s.isEmpty() ? "A campus event on CampusEvents." : s.toString();
    }

    /** Cloudinary covers are cropped to 1200x630 JPEG (what previews expect); other absolute URLs as-is. */
    static String previewImage(String url) {
        if (url == null || !url.startsWith("https://")) return null;
        if (url.contains("res.cloudinary.com/") && url.contains("/upload/")) {
            return url.replaceFirst("/upload/", "/upload/f_jpg,q_auto,c_fill,g_auto,w_1200,h_630/");
        }
        return url;
    }

    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s, "UTF-8");
    }
}
