package com.event.aimodule.recommend;

import com.event.eventmodule.api.EventCard;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * "You might also like" for an event page: content similarity with a reason for every point.
 *
 * <pre>
 * same category    +3     "Also a Workshop event"
 * shared topics    +0.75 per distinctive word in both names/descriptions (max +2.25)   "Similar topics: robotics, arduino"
 * same department  +1.5   "Also for ECE"
 * same venue       +0.5   "Same venue"
 * soon             +0.5 within 14 days
 * </pre>
 * Candidates below {@link #MIN_SCORE} are not similar enough and are left out, so an empty list is a valid answer.
 */
public class SimilarEventsScorer {

    static final double CATEGORY = 3.0, TOPIC = 0.75, MAX_TOPICS = 3, DEPARTMENT = 1.5, VENUE = 0.5, SOON = 0.5;
    static final double MIN_SCORE = 1.5;

    private static final Set<String> STOP_WORDS = Set.of(
            "event", "events", "college", "campus", "students", "student", "will", "with", "from", "this", "that",
            "your", "their", "there", "about", "have", "join", "come", "into", "more", "also", "where", "when",
            "what", "which", "they", "them", "been", "being", "were", "would", "could", "should", "other", "some",
            "time", "day", "days", "date", "venue", "free", "open", "learn", "fun", "great", "best", "part",
            "level", "national", "international", "state", "annual", "edition", "session", "sessions", "program",
            "showcasing", "featuring", "organized", "organised", "hosted", "welcome", "everyone", "participants");

    public List<RecommendedEvent> rank(EventCard target, List<EventCard> candidates, Instant now, int limit) {
        Set<String> targetWords = words(target);
        List<RecommendedEvent> scored = new ArrayList<>();
        for (EventCard e : candidates) {
            if (Objects.equals(e.id(), target.id()) || e.isFull()
                    || e.eventDate() == null || !e.eventDate().toInstant().isAfter(now)) continue;
            RecommendedEvent r = score(target, targetWords, e, now);
            if (r.score() >= MIN_SCORE) scored.add(r);
        }
        scored.sort(Comparator.comparingDouble(RecommendedEvent::score).reversed()
                .thenComparing(r -> r.event().eventDate()));
        return scored.subList(0, Math.min(limit, scored.size()));
    }

    private RecommendedEvent score(EventCard target, Set<String> targetWords, EventCard e, Instant now) {
        double score = 0;
        List<String> reasons = new ArrayList<>();

        if (target.category() != null && target.category().equalsIgnoreCase(Objects.toString(e.category(), ""))) {
            score += CATEGORY;
            reasons.add("Also a " + e.category() + " event");
        }

        // Category names ("workshop") are already scored above, so they don't count as topics
        String categories = (Objects.toString(target.category(), "") + " " + Objects.toString(e.category(), "")).toLowerCase(Locale.ROOT);
        List<String> shared = words(e).stream().filter(targetWords::contains)
                .filter(w -> !categories.contains(w)).sorted().limit((long) MAX_TOPICS).toList();
        if (!shared.isEmpty()) {
            score += TOPIC * shared.size();
            reasons.add("Similar topics: " + String.join(", ", shared));
        }

        if (target.field() != null && !target.field().isBlank() && target.field().equalsIgnoreCase(Objects.toString(e.field(), ""))) {
            score += DEPARTMENT;
            reasons.add("Also for " + e.field());
        }

        if (target.venue() != null && !target.venue().isBlank() && target.venue().equalsIgnoreCase(Objects.toString(e.venue(), ""))) {
            score += VENUE;
            reasons.add("Same venue");
        }

        if (Duration.between(now, e.eventDate().toInstant()).toDays() <= 14) {
            score += SOON;
        }
        return new RecommendedEvent(e, Math.round(score * 100) / 100.0, reasons);
    }

    /** Distinctive words (4+ letters, not stop words) from the name and description. */
    static Set<String> words(EventCard e) {
        String text = (Objects.toString(e.name(), "") + " " + Objects.toString(e.description(), "")).toLowerCase(Locale.ROOT);
        Set<String> out = new TreeSet<>();
        for (String w : text.split("[^a-z0-9+#]+")) {
            if (w.length() >= 4 && !STOP_WORDS.contains(w) && !w.chars().allMatch(Character::isDigit)) out.add(w);
        }
        return out;
    }
}
