package com.event.socialmodule;

import com.event.eventmodule.api.EventCard;
import com.event.socialmodule.digest.DigestBuilder;
import com.event.socialmodule.digest.DigestBuilder.Week;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DigestBuilderTest {

    private static EventCard event(long id, String name, String isoUtc) {
        return new EventCard(id, name, "", "Talk", null, Date.from(Instant.parse(isoUtc)), "Hall <B>", 100L, "0", null, 3);
    }

    private final EventCard robotics = event(1, "Robotics", "2026-10-20T05:30:00Z");   // Tue 11:00 IST
    private final EventCard music = event(2, "Music Night", "2026-10-22T13:30:00Z");   // Thu 19:00 IST
    private final EventCard quiz = event(3, "Quiz", "2026-10-23T05:30:00Z");

    @Test
    void nothingThisWeekMeansNoDigest() {
        assertThat(DigestBuilder.message(new Week(List.of(), List.of(), List.of(), Map.of()))).isEmpty();
    }

    @Test
    void summarisesGoingSavedPicksAndFriends() {
        var msg = DigestBuilder.message(new Week(List.of(quiz), List.of(music), List.of(robotics, music), Map.of(1L, 2)));
        assertThat(msg).contains("This week you're going to 1 event, 1 saved event coming up, "
                + "2 picks for you: Robotics (Tue), Music Night (Thu). 2 friends are going to Robotics.");
    }

    @Test
    void emailLinksEventsAndEscapesText() {
        String html = DigestBuilder.emailHtml(new Week(List.of(), List.of(), List.of(robotics), Map.of(1L, 1)), "https://campus.example");
        assertThat(html).contains("<a href=\"https://campus.example/events/1\">Robotics</a>")
                .contains("Tue 20 Oct, 11:00 AM").contains("Hall &lt;B&gt;").contains("1 friend going")
                .contains("https://campus.example/settings");
    }
}
