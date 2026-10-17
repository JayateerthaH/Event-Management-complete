package com.event.socialmodule;

import com.event.eventmodule.api.EventApi.Participation;
import com.event.eventmodule.api.EventCard;
import com.event.socialmodule.achievement.AchievementRules;
import com.event.socialmodule.achievement.AchievementRules.Achievement;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementRulesTest {

    private static EventCard attended(long id, String category) {
        return new EventCard(id, "E" + id, "", category, null, new Date(), "Hall", null, "0", null, 1);
    }

    private static Map<String, Achievement> byKey(List<Achievement> list) {
        return list.stream().collect(Collectors.toMap(Achievement::key, Function.identity()));
    }

    @Test
    void newStudentHasNothingEarnedButSeesProgressTargets() {
        var all = AchievementRules.evaluate(new Participation(List.of(), 0, 0, 0), 0);
        assertThat(all).noneMatch(Achievement::earned);
        assertThat(byKey(all).get("regular").target()).isEqualTo(5);
    }

    @Test
    void attendanceCategoriesFeedbackCertificatesAndFriendsEarnBadges() {
        var p = new Participation(List.of(attended(1, "Workshop"), attended(2, "workshop "), attended(3, "Talk"),
                attended(4, "Hackathon"), attended(5, null)), 6, 3, 1);
        var b = byKey(AchievementRules.evaluate(p, 2));
        assertThat(b.get("first_event").earned()).isTrue();
        assertThat(b.get("regular").earned()).isTrue();
        assertThat(b.get("explorer").earned()).isTrue();          // Workshop (case/space-insensitive), Talk, Hackathon
        assertThat(b.get("planner").earned()).isTrue();
        assertThat(b.get("voice").earned()).isTrue();
        assertThat(b.get("certified").earned()).isTrue();
        assertThat(b.get("connector").earned()).isFalse();
        assertThat(b.get("connector").progress()).isEqualTo(2);
        assertThat(b.get("veteran").progress()).isEqualTo(5);
    }

    @Test
    void progressIsCappedAtTheTarget() {
        var b = byKey(AchievementRules.evaluate(new Participation(List.of(), 40, 0, 0), 0));
        assertThat(b.get("planner").progress()).isEqualTo(3);
    }
}
