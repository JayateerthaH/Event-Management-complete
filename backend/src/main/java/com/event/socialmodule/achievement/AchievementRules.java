package com.event.socialmodule.achievement;

import com.event.eventmodule.api.EventApi.Participation;
import com.event.eventmodule.api.EventCard;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Badges students earn automatically. Attendance counts only QR-verified check-ins, so badges reward
 * showing up, not just signing up.
 */
public final class AchievementRules {

    private AchievementRules() {}

    public record Achievement(String key, String name, String description, int progress, int target) {
        public boolean earned() {
            return progress >= target;
        }
    }

    public static List<Achievement> evaluate(Participation p, int friends) {
        int attended = p.attended().size();
        int categories = (int) p.attended().stream().map(EventCard::category).filter(Objects::nonNull)
                .map(c -> c.trim().toLowerCase(Locale.ROOT)).filter(c -> !c.isEmpty()).distinct().count();
        return List.of(
                badge("first_event", "First step", "Attend your first event", attended, 1),
                badge("planner", "Planner", "Register for 3 events", p.registrations(), 3),
                badge("regular", "Regular", "Attend 5 events", attended, 5),
                badge("explorer", "Explorer", "Attend events in 3 different categories", categories, 3),
                badge("voice", "Voice", "Give feedback on 3 events", (int) Math.min(Integer.MAX_VALUE, p.feedbackGiven()), 3),
                badge("certified", "Certified", "Earn your first certificate", p.certificates(), 1),
                badge("connector", "Connector", "Make 3 friends through Event buddies", friends, 3),
                badge("veteran", "Campus veteran", "Attend 15 events", attended, 15));
    }

    private static Achievement badge(String key, String name, String description, int progress, int target) {
        return new Achievement(key, name, description, Math.min(progress, target), target);
    }
}
