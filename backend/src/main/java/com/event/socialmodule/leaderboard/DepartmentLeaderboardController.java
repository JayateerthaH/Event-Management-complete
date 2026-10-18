package com.event.socialmodule.leaderboard;

import com.event.eventmodule.api.EventApi;
import com.event.profilemodule.communication.ProfileApi;
import com.event.socialmodule.goal.Semester;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Departments ranked by check-ins this semester. Totals only, never individual students (the plan's
 * alternative to personal leaderboards); the viewer's own department is highlighted with its rank.
 */
@RestController
@RequiredArgsConstructor
public class DepartmentLeaderboardController {

    static final int TOP = 10;

    private final EventApi eventApi;
    private final ProfileApi profileApi;

    public record Row(int rank, String department, long checkIns, boolean mine) {}

    public record Leaderboard(String semester, List<Row> top, Row mine) {}

    @GetMapping("/social/leaderboard/departments")
    public Leaderboard departments(@RequestAttribute("userId") String userId) {
        Long me = Long.valueOf(userId);
        Semester s = Semester.current();
        String myDept = profileApi.department(me).or(() -> eventApi.lastRegistrationDepartment(me))
                .map(DepartmentLeaderboardController::key).orElse(null);
        return build(s.label(), eventApi.checkInsByDepartment(s.startDate(), s.endDate()), myDept);
    }

    static Leaderboard build(String semester, Map<String, Long> counts, String myDept) {
        List<Row> top = new ArrayList<>();
        Row mine = null;
        int rank = 0;
        for (var e : counts.entrySet()) {
            rank++;
            boolean isMine = myDept != null && key(e.getKey()).equals(myDept);
            Row row = new Row(rank, e.getKey(), e.getValue(), isMine);
            if (rank <= TOP) top.add(row);
            if (isMine) mine = row;
        }
        return new Leaderboard(semester, top, mine);
    }

    /** Same grouping as the query: case and spaces don't matter. */
    static String key(String department) {
        return department.replace(" ", "").toLowerCase(Locale.ROOT);
    }
}
