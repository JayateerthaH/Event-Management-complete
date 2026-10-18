package com.event.aimodule.plan;

import com.event.eventmodule.api.EventCard;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanWeekServiceTest {

    private static EventCard at(long id, String iso) {
        return new EventCard(id, "E" + id, "", "Talk", null, Date.from(Instant.parse(iso)), "Hall", 10L, "0", null, 0);
    }

    @Test
    void clashesWithinTwoHoursOfSomethingYoureGoingTo() {
        var going = List.of(at(1, "2026-10-20T10:00:00Z"));
        assertThat(PlanWeekService.clashes(at(2, "2026-10-20T11:30:00Z"), going)).isTrue();
        assertThat(PlanWeekService.clashes(at(3, "2026-10-20T12:00:00Z"), going)).isFalse();
    }

    @Test
    void fallbackSummaryReadsNaturally() {
        assertThat(PlanWeekService.fallbackSummary(0, 0)).startsWith("Nothing on this week yet");
        assertThat(PlanWeekService.fallbackSummary(1, 2)).isEqualTo("You're going to 1 event this week, and here are 2 events worth a look.");
        assertThat(PlanWeekService.fallbackSummary(0, 1)).isEqualTo("You're not registered for anything this week, and here is 1 event worth a look.");
    }
}
