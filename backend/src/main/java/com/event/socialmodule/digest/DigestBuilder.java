package com.event.socialmodule.digest;

import com.event.eventmodule.api.EventCard;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Turns a student's week into a short digest. Pure, so it's easy to test. */
public final class DigestBuilder {

    static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH).withZone(IST);
    static final DateTimeFormatter LONG = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a", Locale.ENGLISH).withZone(IST);

    private DigestBuilder() {}

    /** going: registered this week; saved: saved this week; picks: suggestions this week; friendsGoing by event id. */
    public record Week(List<EventCard> going, List<EventCard> saved, List<EventCard> picks, Map<Long, Integer> friendsGoing) {
        public boolean isEmpty() {
            return going.isEmpty() && saved.isEmpty() && picks.isEmpty();
        }
    }

    /** The in-app message (max 255 characters), or empty when there's nothing worth sending. */
    public static Optional<String> message(Week w) {
        if (w.isEmpty()) return Optional.empty();
        List<String> parts = new ArrayList<>();
        if (!w.going().isEmpty()) parts.add("you're going to " + count(w.going().size(), "event"));
        if (!w.saved().isEmpty()) parts.add(count(w.saved().size(), "saved event") + " coming up");
        if (!w.picks().isEmpty()) parts.add(count(w.picks().size(), "pick") + " for you: " + names(w.picks(), 2));
        String text = "This week " + String.join(", ", parts) + ".";
        Optional<EventCard> social = w.picks().stream().filter(e -> w.friendsGoing().getOrDefault(e.id(), 0) > 0).findFirst();
        if (social.isPresent()) {
            int n = w.friendsGoing().get(social.get().id());
            text += " " + count(n, "friend") + (n == 1 ? " is" : " are") + " going to " + social.get().name() + ".";
        }
        return Optional.of(text.length() > 255 ? text.substring(0, 252) + "..." : text);
    }

    /** The email body (HTML), linking each event. */
    public static String emailHtml(Week w, String frontendUrl) {
        StringBuilder html = new StringBuilder("<div style=\"font-family:Arial,sans-serif;max-width:560px\">"
                + "<h2 style=\"color:#4c1d95\">Your week on campus</h2>");
        section(html, "You're going", w.going(), w, frontendUrl);
        section(html, "Saved events this week", w.saved(), w, frontendUrl);
        section(html, "Picked for you", w.picks(), w, frontendUrl);
        html.append("<p style=\"color:#64748b;font-size:12px\">You get this because the weekly digest email is on. "
                + "Turn it off in <a href=\"").append(frontendUrl).append("/settings\">Settings</a>.</p></div>");
        return html.toString();
    }

    private static void section(StringBuilder html, String title, List<EventCard> events, Week w, String url) {
        if (events.isEmpty()) return;
        html.append("<h3 style=\"margin:18px 0 6px\">").append(esc(title)).append("</h3><ul style=\"padding-left:18px\">");
        for (EventCard e : events) {
            html.append("<li style=\"margin:6px 0\"><a href=\"").append(url).append("/events/").append(e.id()).append("\">")
                    .append(esc(e.name())).append("</a>");
            if (e.eventDate() != null) html.append(" · ").append(LONG.format(e.eventDate().toInstant()));
            if (e.venue() != null && !e.venue().isBlank()) html.append(" · ").append(esc(e.venue()));
            int friends = w.friendsGoing().getOrDefault(e.id(), 0);
            if (friends > 0) html.append(" · ").append(count(friends, "friend")).append(" going");
            html.append("</li>");
        }
        html.append("</ul>");
    }

    private static String names(List<EventCard> events, int max) {
        List<String> out = new ArrayList<>();
        for (EventCard e : events.subList(0, Math.min(max, events.size()))) {
            out.add(e.name() + (e.eventDate() == null ? "" : " (" + DAY.format(e.eventDate().toInstant()) + ")"));
        }
        return String.join(", ", out);
    }

    private static String count(int n, String noun) {
        return n + " " + noun + (n == 1 ? "" : "s");
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
