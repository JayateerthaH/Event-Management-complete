package com.event.socialmodule.goal;

import com.event.eventmodule.api.EventCard;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SemesterTest {

    private static EventCard at(String isoUtc) {
        return new EventCard(1L, "E", "", "Talk", null, isoUtc == null ? null : Date.from(Instant.parse(isoUtc)), "Hall", 10L, "0", null, 0);
    }

    @Test
    void halvesOfTheYear() {
        Semester even = Semester.of(LocalDate.of(2026, 6, 30));
        assertThat(even.key()).isEqualTo("2026-1");
        assertThat(even.label()).isEqualTo("Jan–Jun 2026");
        Semester odd = Semester.of(LocalDate.of(2026, 7, 1));
        assertThat(odd.key()).isEqualTo("2026-2");
        assertThat(odd.end()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(odd.previous()).isEqualTo(even);
        assertThat(Semester.of(LocalDate.of(2027, 1, 2)).previous()).isEqualTo(odd);
    }

    @Test
    void countsEventsByIndianDate() {
        Semester odd = Semester.of(LocalDate.of(2026, 10, 18));
        var events = List.of(
                at("2026-06-30T18:45:00Z"),   // 1 Jul 00:15 IST: in
                at("2026-06-30T18:00:00Z"),   // 30 Jun 23:30 IST: out
                at("2026-12-31T18:00:00Z"),   // 31 Dec 23:30 IST: in
                at("2026-12-31T18:45:00Z"),   // 1 Jan 00:15 IST: out
                at(null));
        assertThat(GoalService.countIn(events, odd)).isEqualTo(2);
        assertThat(odd.contains(new java.sql.Date(Instant.parse("2026-08-01T00:00:00Z").toEpochMilli()))).isTrue();
    }
}
