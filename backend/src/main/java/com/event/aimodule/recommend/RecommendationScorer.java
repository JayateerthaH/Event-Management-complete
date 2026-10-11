package com.event.aimodule.recommend;

import com.event.eventmodule.api.EventCard;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Transparent content-based ranking: every point added comes with a reason shown to the student.
 *
 * <pre>
 * interest match   +3 per matched interest (max 2 counted)  "Matches your interest in AI"
 * past categories  +3 x share of your registrations in this category x confidence
 *                  (confidence = registrations / 3, capped at 1: one past event is weak evidence)
 * department       +1.5 when the event's field is your department
 * popularity       up to +1 (how full the event is)
 * soon             +1 within 7 days, +0.5 within 30 days
 * free             +0.25
 * </pre>
 * Already registered, full and past events are excluded.
 */
public class RecommendationScorer {

    static final double INTEREST = 3.0, HISTORY = 3.0, DEPARTMENT = 1.5, POPULARITY = 1.0;

    public List<RecommendedEvent> rank(List<EventCard> candidates, StudentSignals signals, Instant now, int limit) {
        Set<Long> registered = new HashSet<>();
        Map<String, Integer> categoryCounts = new HashMap<>();
        for (EventCard e : signals.history()) {
            registered.add(e.id());
            if (e.category() != null) categoryCounts.merge(e.category().toLowerCase(Locale.ROOT), 1, Integer::sum);
        }
        int historySize = signals.history().size();

        List<RecommendedEvent> scored = new ArrayList<>();
        for (EventCard e : candidates) {
            if (registered.contains(e.id()) || e.isFull()
                    || e.eventDate() == null || !e.eventDate().toInstant().isAfter(now)) continue;
            scored.add(score(e, signals, categoryCounts, historySize, now));
        }
        scored.sort(Comparator.comparingDouble(RecommendedEvent::score).reversed()
                .thenComparing(r -> r.event().eventDate()));
        return scored.subList(0, Math.min(limit, scored.size()));
    }

    private RecommendedEvent score(EventCard e, StudentSignals s, Map<String, Integer> categoryCounts,
                                   int historySize, Instant now) {
        double score = 0;
        List<String> reasons = new ArrayList<>();
        String text = String.join(" ", Objects.toString(e.name(), ""), Objects.toString(e.description(), ""),
                Objects.toString(e.category(), ""), Objects.toString(e.field(), "")).toLowerCase(Locale.ROOT);

        int matched = 0;
        for (String interest : s.interests()) {
            if (matched == 2) break;
            if (matches(text, e.category(), interest)) {
                score += INTEREST;
                matched++;
                reasons.add("Matches your interest in " + interest);
            }
        }

        if (historySize > 0 && e.category() != null) {
            int count = categoryCounts.getOrDefault(e.category().toLowerCase(Locale.ROOT), 0);
            if (count > 0) {
                double confidence = Math.min(1.0, historySize / 3.0);
                score += HISTORY * count / historySize * confidence;
                reasons.add("You've registered for " + count + " " + e.category() + " event" + (count > 1 ? "s" : ""));
            }
        }

        if (s.department() != null && e.field() != null && e.field().equalsIgnoreCase(s.department().trim())) {
            score += DEPARTMENT;
            reasons.add("For your department (" + e.field() + ")");
        }

        if (e.maxPeople() != null && e.maxPeople() > 0) {
            double fill = Math.min(1.0, (double) e.registeredCount() / e.maxPeople());
            score += POPULARITY * fill;
            if (fill >= 0.5) reasons.add("Popular: " + Math.round(fill * 100) + "% of seats taken");
        } else if (e.registeredCount() > 0) {
            score += POPULARITY * Math.min(1.0, e.registeredCount() / 50.0);
        }

        long days = Duration.between(now, e.eventDate().toInstant()).toDays();
        if (days <= 7) {
            score += 1.0;
            reasons.add("Happening this week");
        } else if (days <= 30) {
            score += 0.5;
        }

        if (isFree(e.costOfRegistration())) {
            score += 0.25;
            reasons.add("Free to attend");
        }

        if (reasons.isEmpty()) reasons.add("Upcoming on campus");
        return new RecommendedEvent(e, Math.round(score * 100) / 100.0, reasons);
    }

    // Exact category match, or the interest (or one of its words, 3+ letters) appears in the event text
    private static boolean matches(String eventText, String category, String interest) {
        if (interest == null || interest.isBlank()) return false;
        String i = interest.toLowerCase(Locale.ROOT).trim();
        if (category != null && category.equalsIgnoreCase(i)) return true;
        if (eventText.contains(i)) return true;
        for (String word : i.split("[^a-z0-9+#]+")) {
            if (word.length() >= 3 && eventText.matches("(?s).*\\b" + java.util.regex.Pattern.quote(word) + "\\b.*")) return true;
        }
        return false;
    }

    static boolean isFree(String cost) {
        if (cost == null || cost.isBlank()) return true;
        String c = cost.trim().toLowerCase(Locale.ROOT);
        return c.equals("free") || c.matches("0+(\\.0+)?");
    }
}
