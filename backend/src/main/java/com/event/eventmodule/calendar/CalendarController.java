package com.event.eventmodule.calendar;

import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventCard;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Calendar subscription: a private .ics URL with the student's registered and saved events, which
 * Google/Apple/Outlook calendars refresh on their own. Anyone with the link can read it, so it can be reset.
 */
@RestController
@RequiredArgsConstructor
public class CalendarController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CalendarFeedRepository feedRepository;
    private final EventApi eventApi;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public record FeedLink(String url) {}

    @GetMapping("/events/calendar-feed")
    @Transactional
    public FeedLink link(@RequestAttribute("userId") String userId) {
        Long me = Long.valueOf(userId);
        CalendarFeed feed = feedRepository.findById(me).orElseGet(() -> create(me));
        return new FeedLink(url(feed.getToken()));
    }

    @PostMapping("/events/calendar-feed/reset")
    @Transactional
    public FeedLink reset(@RequestAttribute("userId") String userId) {
        Long me = Long.valueOf(userId);
        CalendarFeed feed = feedRepository.findById(me).orElseGet(() -> create(me));
        feed.setToken(newToken());
        feed.setCreatedAt(LocalDateTime.now());
        feedRepository.save(feed);
        return new FeedLink(url(feed.getToken()));
    }

    @GetMapping(value = "/public/calendar/{token}.ics")
    public ResponseEntity<String> feed(@PathVariable String token) {
        if (!token.matches("[A-Za-z0-9_-]{20,64}")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        CalendarFeed feed = feedRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This calendar link was reset or doesn't exist"));
        Instant now = Instant.now(), since = now.minus(Duration.ofDays(30));
        Map<Long, IcsBuilder.Entry> entries = new LinkedHashMap<>();
        for (EventCard e : eventApi.registeredEvents(feed.getUserId())) add(entries, e, "You're registered.", since);
        for (EventCard e : eventApi.savedEvents(feed.getUserId())) {
            if (!entries.containsKey(e.id())) add(entries, e, "Saved (not registered yet).", since);
        }
        String body = IcsBuilder.feed("CampusEvents", new ArrayList<>(entries.values()), now);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(30)))
                .header("Content-Disposition", "inline; filename=\"campusevents.ics\"")
                .body(body);
    }

    private void add(Map<Long, IcsBuilder.Entry> entries, EventCard e, String status, Instant since) {
        if (e.eventDate() == null || e.eventDate().toInstant().isBefore(since)) return;
        Long hours = eventApi.eventProfile(e.id()).map(p -> p.durationInHours()).orElse(null);
        String about = e.description() == null ? null
                : (e.description().length() > 500 ? e.description().substring(0, 497) + "..." : e.description());
        entries.put(e.id(), new IcsBuilder.Entry(e.id(), e.name(), e.eventDate().toInstant(), hours, e.venue(), about,
                base() + "/events/" + e.id(), status));
    }

    private CalendarFeed create(Long userId) {
        CalendarFeed f = new CalendarFeed();
        f.setUserId(userId);
        f.setToken(newToken());
        f.setCreatedAt(LocalDateTime.now());
        return feedRepository.save(f);
    }

    private static String newToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String base() {
        return frontendUrl.replaceAll("/+$", "");
    }

    /** Served through the frontend's /api proxy, so the link uses the app's own domain. */
    private String url(String token) {
        return base() + "/api/public/calendar/" + token + ".ics";
    }
}
