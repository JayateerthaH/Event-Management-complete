package com.event.aimodule;

import com.event.aimodule.recommend.RecommendedEvent;
import com.event.aimodule.recommend.SimilarEventsScorer;
import com.event.eventmodule.api.EventCard;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimilarEventsScorerTest {

    private static final Instant NOW = Instant.parse("2026-10-11T10:00:00Z");
    private final SimilarEventsScorer scorer = new SimilarEventsScorer();

    private static EventCard event(long id, String name, String description, String category, String field,
                                   int inDays, Long max, long registered) {
        return new EventCard(id, name, description, category, field,
                Date.from(NOW.plus(Duration.ofDays(inDays))), "Main Hall", max, "0", null, registered);
    }

    @Test
    void ranksSameCategoryAndSharedTopicsFirstWithReasons() {
        EventCard target = event(1, "Robotics Workshop", "Build an arduino robot", "Workshop", "ECE", 5, 50L, 0);
        EventCard arduino = event(2, "Arduino Basics", "Program arduino boards for robotics", "Workshop", "ECE", 20, 50L, 0);
        EventCard painting = event(3, "Painting Workshop", "Watercolours for beginners", "Workshop", null, 20, 50L, 0);
        EventCard music = event(4, "Music Night", "Live bands", "Cultural", null, 20, 50L, 0);

        List<RecommendedEvent> ranked = scorer.rank(target, List.of(music, painting, arduino, target), NOW, 10);

        assertThat(ranked).extracting(r -> r.event().id()).containsExactly(2L, 3L);
        assertThat(ranked.get(0).reasons())
                .contains("Also a Workshop event", "Similar topics: arduino, robotics", "Also for ECE", "Same venue");
    }

    @Test
    void leavesOutTheEventItselfFullAndPastEventsAndWeakMatches() {
        EventCard target = event(1, "Robotics Workshop", "robots", "Workshop", null, 5, null, 0);
        EventCard full = event(2, "Robotics Advanced", "robots", "Workshop", null, 10, 10L, 10);
        EventCard past = event(3, "Robotics Intro", "robots", "Workshop", null, -1, null, 0);
        EventCard unrelated = new EventCard(4L, "Chess Club", "Board games", "Sports", null,
                Date.from(NOW.plus(Duration.ofDays(60))), "Library", null, "0", null, 0);

        assertThat(scorer.rank(target, List.of(target, full, past, unrelated), NOW, 10)).isEmpty();
    }
}
