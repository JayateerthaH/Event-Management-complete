package com.event.eventmodule.calendar;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * iCalendar (RFC 5545) feed, matching the single-event download in the app: an event stored at midnight UTC
 * (date only) is an all-day entry; otherwise it lasts its duration (default 1 hour).
 */
public final class IcsBuilder {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private IcsBuilder() {}

    public record Entry(Long eventId, String name, Instant start, Long durationHours, String venue, String description,
                        String url, String status) {}

    public static String feed(String calendarName, List<Entry> entries, Instant now) {
        List<String> lines = new ArrayList<>(List.of("BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//CampusEvents//My Events//EN",
                "CALSCALE:GREGORIAN", "METHOD:PUBLISH", "X-WR-CALNAME:" + text(calendarName),
                "REFRESH-INTERVAL;VALUE=DURATION:PT6H", "X-PUBLISHED-TTL:PT6H"));
        for (Entry e : entries) {
            if (e.start() == null) continue;
            lines.add("BEGIN:VEVENT");
            lines.add("UID:event-" + e.eventId() + "@campusevents");
            lines.add("DTSTAMP:" + STAMP.format(now));
            boolean allDay = e.start().atZone(ZoneOffset.UTC).toLocalTime().toSecondOfDay() == 0;
            if (allDay) {
                lines.add("DTSTART;VALUE=DATE:" + DATE.format(e.start()));
                lines.add("DTEND;VALUE=DATE:" + DATE.format(e.start().plus(Duration.ofDays(1))));
            } else {
                long hours = e.durationHours() != null && e.durationHours() > 0 ? e.durationHours() : 1;
                lines.add("DTSTART:" + STAMP.format(e.start()));
                lines.add("DTEND:" + STAMP.format(e.start().plus(Duration.ofHours(hours))));
            }
            lines.add("SUMMARY:" + text(e.name() == null ? "Campus event" : e.name()));
            if (e.venue() != null && !e.venue().isBlank()) lines.add("LOCATION:" + text(e.venue()));
            String desc = String.join("\n\n", nonBlank(e.status(), e.description(), e.url() == null ? null : "Event page: " + e.url()));
            if (!desc.isBlank()) lines.add("DESCRIPTION:" + text(desc));
            if (e.url() != null) lines.add("URL:" + e.url());
            lines.add("END:VEVENT");
        }
        lines.add("END:VCALENDAR");
        StringBuilder out = new StringBuilder();
        for (String line : lines) out.append(fold(line)).append("\r\n");
        return out.toString();
    }

    static String text(String s) {
        return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\\n").replace("\n", "\\n");
    }

    /** Lines over 75 octets are folded with CRLF + space, never splitting a UTF-8 character. */
    static String fold(String line) {
        StringBuilder out = new StringBuilder();
        int octets = 0;
        for (int i = 0; i < line.length(); ) {
            int cp = line.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            int len = ch.getBytes(StandardCharsets.UTF_8).length;
            if (octets + len > 75) {
                out.append("\r\n ");
                octets = 1;
            }
            out.append(ch);
            octets += len;
            i += Character.charCount(cp);
        }
        return out.toString();
    }

    private static List<String> nonBlank(String... values) {
        List<String> out = new ArrayList<>();
        for (String v : values) if (v != null && !v.isBlank()) out.add(v.trim());
        return out;
    }
}
