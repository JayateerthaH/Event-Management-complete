package com.event.eventmodule;

import com.event.eventmodule.calendar.IcsBuilder;
import com.event.eventmodule.calendar.IcsBuilder.Entry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IcsBuilderTest {

    private static final Instant NOW = Instant.parse("2026-10-18T00:00:00Z");

    @Test
    void timedAndAllDayEventsWithEscapingAndRefreshHint() {
        String ics = IcsBuilder.feed("CampusEvents", List.of(
                new Entry(7L, "Robotics; Build, Race", Instant.parse("2026-12-15T05:30:00Z"), 3L, "Hall B", "Line 1\nLine 2",
                        "https://campus.example/events/7", "You're registered."),
                new Entry(8L, "Fest", Instant.parse("2026-12-20T00:00:00Z"), null, null, null, null, null)), NOW);
        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n")
                .contains("REFRESH-INTERVAL;VALUE=DURATION:PT6H")
                .contains("UID:event-7@campusevents\r\n")
                .contains("DTSTART:20261215T053000Z\r\nDTEND:20261215T083000Z")
                .contains("SUMMARY:Robotics\\; Build\\, Race")
                .contains("DTSTART;VALUE=DATE:20261220\r\nDTEND;VALUE=DATE:20261221")
                .contains("LOCATION:Hall B");
        assertThat(ics.replace("\r\n ", "")).contains("DESCRIPTION:You're registered.\\n\\nLine 1\\nLine 2\\n\\nEvent page: https://campus.example/events/7");
    }

    @Test
    void longLinesAreFoldedAt75OctetsWithoutBreakingCharacters() {
        String name = "कार्यशाला ".repeat(20);
        String ics = IcsBuilder.feed("CampusEvents", List.of(new Entry(1L, name, Instant.parse("2026-12-15T05:30:00Z"),
                1L, null, null, null, null)), NOW);
        for (String line : ics.split("\r\n")) {
            assertThat(line.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75);
        }
        assertThat(ics.replace("\r\n ", "")).contains("SUMMARY:" + name.trim());
    }
}
