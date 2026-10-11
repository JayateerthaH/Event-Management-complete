package com.event.aimodule.assistant;

import com.event.eventmodule.api.EventCard;
import com.event.eventmodule.api.EventProfile;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Renders an event's public page content as plain text for an LLM prompt (empty sections are left out). */
final class EventFacts {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final int MAX_FIELD = 600;

    private EventFacts() {
    }

    static String describe(EventProfile p) {
        EventCard c = p.card();
        StringBuilder sb = new StringBuilder();
        line(sb, "Event", c.name());
        line(sb, "Category", c.category());
        line(sb, "Department / field", c.field());
        line(sb, "Date", date(c));
        line(sb, "Venue", c.venue());
        line(sb, "Registration cost", cost(c.costOfRegistration()));
        if (c.maxPeople() != null && c.maxPeople() > 0) {
            line(sb, "Seats", c.registeredCount() + " of " + c.maxPeople() + " taken");
        }
        line(sb, "Description", c.description());
        line(sb, "About", p.about());
        line(sb, "Organized by", p.organizedBy());
        line(sb, "Level", p.level());
        line(sb, "Language", p.language());
        if (p.durationInHours() != null && p.durationInHours() > 0) line(sb, "Duration", p.durationInHours() + " hours");
        if (!p.departments().isEmpty()) line(sb, "Departments", String.join(", ", p.departments()));
        list(sb, "Schedule", p.schedule(), s -> join(" - ", s.time(), s.title(), s.description()));
        list(sb, "Speakers", p.speakers(), s -> join(", ", s.name(), s.profession(), s.experience()));
        list(sb, "Highlights", p.highlights(), h -> h);
        list(sb, "What to bring", p.whatToBring(), w -> w);
        list(sb, "FAQs", p.faqs(), f -> join(" A: ", f.question() == null ? null : "Q: " + f.question(), f.answer()));
        return sb.toString().trim();
    }

    /**
     * "Friday, 16 October 2026" plus the time when one was set. The create form sends a bare date, which is
     * stored as UTC midnight; formatting that in the server's zone would invent a time (05:30 in IST).
     */
    static String date(EventCard c) {
        if (c.eventDate() == null) return null;
        ZonedDateTime utc = c.eventDate().toInstant().atZone(ZoneOffset.UTC);
        if (utc.toLocalTime().equals(LocalTime.MIDNIGHT)) return DATE.format(utc);
        ZonedDateTime local = c.eventDate().toInstant().atZone(ZoneId.systemDefault());
        return DATE.format(local) + ", " + TIME.format(local);
    }

    /** "Free", or the amount in rupees as the app shows it ("₹1200"); free text is passed through. */
    static String cost(String cost) {
        if (isFree(cost)) return "Free";
        String c = cost.trim();
        return c.matches("\\d+(\\.\\d+)?") ? "₹" + c : c;
    }

    static boolean isFree(String cost) {
        if (cost == null || cost.isBlank()) return true;
        String s = cost.trim().toLowerCase(Locale.ROOT);
        return s.equals("free") || s.matches("0+(\\.0+)?");
    }

    private static void line(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) sb.append(label).append(": ").append(clip(value)).append('\n');
    }

    private static <T> void list(StringBuilder sb, String label, List<T> items, Function<T, String> render) {
        List<String> lines = items.stream().map(render).filter(s -> s != null && !s.isBlank()).limit(25).toList();
        if (lines.isEmpty()) return;
        sb.append(label).append(":\n");
        lines.forEach(l -> sb.append("- ").append(clip(l)).append('\n'));
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(sep);
            sb.append(p.trim());
        }
        return sb.toString();
    }

    private static String clip(String s) {
        String t = s.trim().replaceAll("\\s+", " ");
        return t.length() > MAX_FIELD ? t.substring(0, MAX_FIELD) + "…" : t;
    }
}
