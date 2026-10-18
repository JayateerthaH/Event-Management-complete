package com.event.socialmodule.leaderboard;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class DepartmentLeaderboardTest {

    @Test
    void ranksTopTenAndFindsMyDepartmentEvenBelowIt() {
        var counts = new LinkedHashMap<String, Long>();
        for (int i = 1; i <= 12; i++) counts.put("Dept " + i, 100L - i);
        var board = DepartmentLeaderboardController.build("Jul–Dec 2026", counts, DepartmentLeaderboardController.key("DEPT 12"));
        assertThat(board.top()).hasSize(10);
        assertThat(board.top().get(0).department()).isEqualTo("Dept 1");
        assertThat(board.mine().rank()).isEqualTo(12);
        assertThat(board.top()).noneMatch(DepartmentLeaderboardController.Row::mine);
    }

    @Test
    void noDepartmentMeansNoHighlight() {
        var board = DepartmentLeaderboardController.build("S", new LinkedHashMap<>(java.util.Map.of("CSE", 3L)), null);
        assertThat(board.mine()).isNull();
    }
}
