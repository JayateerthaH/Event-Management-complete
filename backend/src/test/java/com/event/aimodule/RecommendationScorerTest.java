package com.event.aimodule;

import com.event.aimodule.recommend.RecommendationScorer;
import com.event.aimodule.recommend.RecommendedEvent;
import com.event.aimodule.recommend.StudentSignals;
import com.event.eventmodule.api.EventCard;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationScorerTest {

    private static final Instant NOW = Instant.parse("2026-10-11T10:00:00Z");
    private final RecommendationScorer scorer = new RecommendationScorer();

    private static EventCard event(long id, String name, String category, String field, int inDays,
                                   Long max, long registered, String cost) {
        return new EventCard(id, name, name + " description", category, field,
                Date.from(NOW.plus(Duration.ofDays(inDays))), "Hall", max, cost, null, registered);
    }

    @Test
    void ranksByInterestsHistoryAndDepartmentWithReasons() {
        EventCard ai = event(1, "Intro to Machine Learning", "Workshop", "CSE", 20, 100L, 10, "0");
        EventCard music = event(2, "Music Night", "Cultural", null, 20, 100L, 10, "200");
        EventCard hack = event(3, "Hackathon", "Hackathon", "CSE", 20, 100L, 10, "0");
        EventCard pastHackathon = event(9, "Old Hackathon", "Hackathon", "CSE", -40, 100L, 10, "0");

        List<RecommendedEvent> ranked = scorer.rank(List.of(music, hack, ai),
                new StudentSignals(List.of("Machine Learning"), "CSE", List.of(pastHackathon)), NOW, 10);

        assertThat(ranked).extracting(r -> r.event().id()).containsExactly(1L, 3L, 2L);
        assertThat(ranked.get(0).reasons()).contains("Matches your interest in Machine Learning", "For your department (CSE)");
        assertThat(ranked.get(1).reasons()).contains("You've registered for 1 Hackathon event");
    }

    @Test
    void historyWeighsMoreWithMoreRegistrations() {
        EventCard hack = event(3, "Hackathon", "Hackathon", null, 20, null, 0, "100");
        List<EventCard> oneHackathon = List.of(event(10, "Old 1", "Hackathon", null, -40, null, 0, "0"));
        List<EventCard> threeHackathons = List.of(event(10, "Old 1", "Hackathon", null, -40, null, 0, "0"),
                event(11, "Old 2", "Hackathon", null, -30, null, 0, "0"), event(12, "Old 3", "Hackathon", null, -20, null, 0, "0"));

        double weak = scorer.rank(List.of(hack), new StudentSignals(List.of(), null, oneHackathon), NOW, 1).get(0).score();
        double strong = scorer.rank(List.of(hack), new StudentSignals(List.of(), null, threeHackathons), NOW, 1).get(0).score();

        assertThat(strong).isGreaterThan(weak);
    }

    @Test
    void excludesRegisteredFullAndPastEvents() {
        EventCard registered = event(1, "Registered", "Workshop", null, 5, 10L, 1, "0");
        EventCard full = event(2, "Full", "Workshop", null, 5, 10L, 10, "0");
        EventCard past = event(3, "Past", "Workshop", null, -1, 10L, 1, "0");
        EventCard open = event(4, "Open", "Workshop", null, 5, 10L, 1, "0");

        List<RecommendedEvent> ranked = scorer.rank(List.of(registered, full, past, open),
                new StudentSignals(List.of(), null, List.of(registered)), NOW, 10);

        assertThat(ranked).extracting(r -> r.event().id()).containsExactly(4L);
    }

    @Test
    void coldStartStillRanksBySoonnessAndPopularity() {
        EventCard soonPopular = event(1, "Soon", "Seminar", null, 3, 10L, 8, "0");
        EventCard later = event(2, "Later", "Seminar", null, 60, 10L, 1, "100");

        List<RecommendedEvent> ranked = scorer.rank(List.of(later, soonPopular),
                new StudentSignals(List.of(), null, List.of()), NOW, 10);

        assertThat(ranked).extracting(r -> r.event().id()).containsExactly(1L, 2L);
        assertThat(ranked.get(0).reasons()).contains("Happening this week", "Popular: 80% of seats taken", "Free to attend");
        assertThat(ranked.get(1).reasons()).containsExactly("Upcoming on campus");
    }

    @Test
    void respectsLimit() {
        List<EventCard> many = java.util.stream.LongStream.rangeClosed(1, 10)
                .mapToObj(i -> event(i, "E" + i, "Seminar", null, (int) i, null, 0, "0")).toList();
        assertThat(scorer.rank(many, new StudentSignals(List.of(), null, List.of()), NOW, 3)).hasSize(3);
    }

    @Test
    void savedEventsBoostTheirCategoryAndAreNotSuggestedAgain() {
        EventCard savedTalk = event(1, "Cloud Talk", "Talk", null, 10, 100L, 5, "0");
        EventCard otherTalk = event(2, "Security Talk", "Talk", null, 20, 100L, 5, "0");
        EventCard workshop = event(3, "Pottery", "Workshop", null, 20, 100L, 5, "0");

        List<RecommendedEvent> ranked = scorer.rank(List.of(savedTalk, otherTalk, workshop),
                new StudentSignals(List.of(), null, List.of(), List.of(savedTalk)), NOW, 10);

        assertThat(ranked).extracting(r -> r.event().id()).containsExactly(2L, 3L);
        assertThat(ranked.get(0).reasons()).contains("Like events you saved");
    }
}
