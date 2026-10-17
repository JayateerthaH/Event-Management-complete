package com.event.analyticsmodule.engagement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Engagement numbers for admins, from the activity table. */
@Service
@RequiredArgsConstructor
public class EngagementService {

    private final ActivityEventRepository repository;

    public record DayCount(LocalDate day, long users) {}

    /**
     * week1Retention: of students who signed up 14-60 days ago, the share active again 7-13 days after signup
     * (null until there are such signups). countsLast30Days: actions by type.
     */
    public record Engagement(long dailyActive, long weeklyActive, long monthlyActive, Double weeklyOverMonthly,
                             Double week1Retention, int retentionCohort, Map<String, Long> countsLast30Days,
                             List<DayCount> dailyActiveLast30Days, LocalDateTime trackingSince) {}

    @Transactional(readOnly = true)
    public Engagement engagement() {
        LocalDateTime now = LocalDateTime.now();
        long dau = repository.activeUsersSince(now.minusDays(1));
        long wau = repository.activeUsersSince(now.minusDays(7));
        long mau = repository.activeUsersSince(now.minusDays(30));

        Map<String, Long> counts = new TreeMap<>();
        for (Object[] row : repository.countsByTypeSince(now.minusDays(30))) {
            counts.put((String) row[0], ((Number) row[1]).longValue());
        }

        Map<LocalDate, Long> byDay = new HashMap<>();
        for (Object[] row : repository.dailyActiveSince(now.toLocalDate().minusDays(29).atStartOfDay())) {
            LocalDate day = row[0] instanceof Date d ? d.toLocalDate() : LocalDate.parse(row[0].toString());
            byDay.put(day, ((Number) row[1]).longValue());
        }
        List<DayCount> series = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate day = now.toLocalDate().minusDays(i);
            series.add(new DayCount(day, byDay.getOrDefault(day, 0L)));
        }

        List<Object[]> cohort = repository.signupsBetween(now.minusDays(60), now.minusDays(14));
        int retained = 0;
        for (Object[] row : cohort) {
            LocalDateTime signedUp = (LocalDateTime) row[1];
            if (repository.activeBetween((Long) row[0], signedUp.plusDays(7), signedUp.plusDays(14))) retained++;
        }

        LocalDateTime since = repository.findAll(org.springframework.data.domain.PageRequest.of(0, 1,
                org.springframework.data.domain.Sort.by("createdAt"))).stream().findFirst()
                .map(ActivityEvent::getCreatedAt).orElse(null);
        return new Engagement(dau, wau, mau, mau == 0 ? null : (double) wau / mau,
                cohort.isEmpty() ? null : (double) retained / cohort.size(), cohort.size(), counts, series, since);
    }
}
