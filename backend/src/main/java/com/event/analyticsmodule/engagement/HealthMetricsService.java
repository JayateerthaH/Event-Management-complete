package com.event.analyticsmodule.engagement;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The retention plan's "what to measure" list, for admins: day-1/7/30 retention, time to first registration,
 * show-up and feedback rates, organizer repeat rate, feature adoption and notification health. Read-only SQL
 * over existing tables; each number is null until there's enough data for it.
 */
@Service
@RequiredArgsConstructor
public class HealthMetricsService {

    private final JdbcTemplate jdbc;

    public record Rate(Double rate, long cohort) {}

    public record Health(Rate day1, Rate day7, Rate day30, Double medianMinutesToFirstRegistration,
                         Double firstHourRegistrationShare, Rate showUp, Double feedbackRate, Rate organizerRepeat,
                         Double followsPerStudent, Double sharesPerEvent, long portfoliosPublished, long goalsSet,
                         long pushDevices, long pushTurnedOn30, long pushTurnedOff30, Map<String, Long> notificationsTurnedOff) {}

    public Health health() {
        LocalDateTime now = LocalDateTime.now();
        return new Health(
                retention(1, now.minusDays(60), now.minusDays(2)),
                retention(7, now.minusDays(60), now.minusDays(8)),
                retention(30, now.minusDays(120), now.minusDays(31)),
                jdbc.queryForObject("""
                        select percentile_cont(0.5) within group (order by extract(epoch from (r.first_at - s.created_at)) / 60)
                        from analytics.activity_event s
                        join (select user_id, min(created_at) first_at from analytics.activity_event where type = 'event_registered' group by user_id) r
                          on r.user_id = s.user_id and r.first_at >= s.created_at
                        where s.type = 'signup' and s.created_at >= ?""", Double.class, ts(now.minusDays(90))),
                ratio(jdbc.queryForMap("""
                        select count(*) filter (where exists (select 1 from analytics.activity_event r where r.user_id = s.user_id
                                 and r.type = 'event_registered' and r.created_at >= s.created_at and r.created_at < s.created_at + interval '1 hour')) as hit,
                               count(*) as total
                        from analytics.activity_event s where s.type = 'signup' and s.created_at >= ?""", ts(now.minusDays(90)))),
                showUp(now),
                feedbackRate(now),
                organizerRepeat(now),
                perUnit("select count(*) from social.organizer_follow",
                        "select count(distinct user_id) from analytics.activity_event where created_at >= now() - interval '30 days'"),
                perUnit("select count(*) from analytics.activity_event where type = 'event_shared' and created_at >= now() - interval '30 days'",
                        "select count(*) from events.events where is_approved = true and event_date >= now() - interval '30 days'"),
                count("select count(*) from social.portfolio where published = true"),
                count("select count(*) from social.semester_goal"),
                count("select count(*) from social.push_subscription"),
                count("select count(*) from analytics.activity_event where type = 'push_enabled' and created_at >= now() - interval '30 days'"),
                count("select count(*) from analytics.activity_event where type = 'push_disabled' and created_at >= now() - interval '30 days'"),
                optOuts());
    }

    /** Of students who signed up in the window, the share active on day N after signup. */
    private Rate retention(int day, LocalDateTime from, LocalDateTime to) {
        Map<String, Object> row = jdbc.queryForMap("""
                select count(*) filter (where exists (select 1 from analytics.activity_event b where b.user_id = s.user_id
                         and b.created_at >= s.created_at + make_interval(days => ?) and b.created_at < s.created_at + make_interval(days => ?))) as hit,
                       count(*) as total
                from analytics.activity_event s where s.type = 'signup' and s.created_at >= ? and s.created_at < ?""",
                day, day + 1, ts(from), ts(to));
        return new Rate(ratio(row), ((Number) row.get("total")).longValue());
    }

    /** Check-ins per registration, for past events (90 days) where the organizer scanned anyone at all. */
    private Rate showUp(LocalDateTime now) {
        Map<String, Object> row = jdbc.queryForMap("""
                select count(*) filter (where r.is_attended) as hit, count(*) as total, count(distinct e.id) as events
                from events.register r join events.events e on e.id = r.event_id
                where e.event_date >= ? and e.event_date < now()
                  and exists (select 1 from events.register x where x.event_id = e.id and x.is_attended)""", ts(now.minusDays(90)));
        return new Rate(ratio(row), ((Number) row.get("events")).longValue());
    }

    private Double feedbackRate(LocalDateTime now) {
        return ratio(jdbc.queryForMap("""
                select count(distinct (f.user_id, f.event_id)) as hit,
                       (select count(*) from events.register r join events.events e on e.id = r.event_id
                         where r.is_attended and e.event_date >= ? and e.event_date < now()) as total
                from events.feed_back f join events.events e on e.id = f.event_id
                join events.register r on r.event_id = f.event_id and r.user_id = f.user_id and r.is_attended
                where e.event_date >= ? and e.event_date < now()""", ts(now.minusDays(90)), ts(now.minusDays(90))));
    }

    /** Organizers with an event in the last 180 days who ran another within 30 days of one. */
    private Rate organizerRepeat(LocalDateTime now) {
        Map<String, Object> row = jdbc.queryForMap("""
                select count(*) filter (where repeat) as hit, count(*) as total from (
                  select e.organizer_id, bool_or(exists (select 1 from events.events o where o.organizer_id = e.organizer_id and o.id <> e.id
                           and o.event_date > e.event_date and o.event_date <= e.event_date + interval '30 days')) as repeat
                  from events.events e where e.organizer_id is not null and e.event_date >= ? group by e.organizer_id) t""",
                ts(now.minusDays(180)));
        return new Rate(ratio(row), ((Number) row.get("total")).longValue());
    }

    private Map<String, Long> optOuts() {
        Map<String, Object> row = jdbc.queryForMap("""
                select count(*) filter (where not friend_requests) as friend_requests, count(*) filter (where not friend_going) as friend_going,
                       count(*) filter (where not new_events) as new_events, count(*) filter (where not badges) as badges,
                       count(*) filter (where not weekly_digest) as weekly_digest
                from social.notification_prefs""");
        Map<String, Long> out = new LinkedHashMap<>();
        row.forEach((k, v) -> out.put(k, ((Number) v).longValue()));
        return out;
    }

    private Double perUnit(String countSql, String unitsSql) {
        long n = count(countSql), units = count(unitsSql);
        return units == 0 ? null : (double) n / units;
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }

    private static Double ratio(Map<String, Object> row) {
        long total = ((Number) row.get("total")).longValue();
        return total == 0 ? null : ((Number) row.get("hit")).doubleValue() / total;
    }

    private static Timestamp ts(LocalDateTime t) {
        return Timestamp.valueOf(t);
    }
}
