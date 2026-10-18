package com.event.socialmodule.wrapped;

import com.event.eventmodule.api.EventApi.AttendedEvent;
import com.event.eventmodule.api.EventCard;
import com.event.socialmodule.goal.Semester;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WrappedServiceTest {

    private static AttendedEvent event(String category, String club) {
        return new AttendedEvent(new EventCard(1L, "E", "", category, null, new Date(), "Hall", 10L, "0", null, 0), club, false);
    }

    @Test
    void topPercentOnlyWhenMeaningfulAndFlattering() {
        assertThat(WrappedService.topPercent(3, List.of(3L, 1L, 1L, 1L))).isNull();              // too few students
        assertThat(WrappedService.topPercent(9, List.of(9L, 1L, 1L, 2L, 3L, 1L, 1L, 1L, 1L, 1L))).isEqualTo(10);
        assertThat(WrappedService.topPercent(1, List.of(9L, 1L, 1L, 2L, 3L, 1L))).isNull();      // everyone did as well
    }

    @Test
    void topGroupsCaseInsensitivelyAndSkipsBlanks() {
        var top = WrappedService.top(List.of(event("Talk", "IEEE"), event("talk", null), event("Workshop", " ")), AttendedEvent::organizedBy);
        assertThat(top).containsExactly(new WrappedService.Count("IEEE", 1));
        assertThat(WrappedService.top(List.of(event("Talk", null), event("talk ", null), event("Quiz", null)), a -> a.card().category()))
                .containsExactly(new WrappedService.Count("Talk", 2), new WrappedService.Count("Quiz", 1));
    }

    @Test
    void semesterKeys() {
        assertThat(Semester.fromKey("2026-1")).contains(Semester.of(LocalDate.of(2026, 3, 1)));
        assertThat(Semester.fromKey("2026-3")).isEmpty();
        assertThat(Semester.fromKey("x")).isEmpty();
    }
}
