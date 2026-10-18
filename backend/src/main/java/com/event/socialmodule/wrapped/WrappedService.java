package com.event.socialmodule.wrapped;

import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventApi.AttendedEvent;
import com.event.socialmodule.achievement.AchievementEarnedRepository;
import com.event.socialmodule.entity.BuddyRequest;
import com.event.socialmodule.goal.Semester;
import com.event.socialmodule.repository.BuddyRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Campus Wrapped: a student's semester in numbers, from QR-verified attendance only. The campus comparison
 * uses counts across all students, never anyone else's details.
 */
@Service
@RequiredArgsConstructor
public class WrappedService {

    /** The "top X%" line needs at least this many active students, so it means something. */
    static final int MIN_STUDENTS_FOR_RANK = 5;

    private final EventApi eventApi;
    private final BuddyRequestRepository requestRepository;
    private final AchievementEarnedRepository earnedRepository;

    public record Count(String name, int count) {}

    public record Wrapped(String key, String semester, int events, long hours, int certificates,
                          List<Count> topCategories, List<Count> topClubs, String busiestMonth, int busiestMonthEvents,
                          int badges, int friends, String firstEvent, String lastEvent, Integer topPercent,
                          List<String> semesters) {}

    @Transactional(readOnly = true)
    public Wrapped forStudent(Long userId, Semester s) {
        List<AttendedEvent> attended = eventApi.attendedEvents(userId).stream()
                .filter(a -> s.contains(a.card().eventDate()))
                .sorted(Comparator.comparing(a -> a.card().eventDate().getTime()))
                .toList();

        long hours = attended.stream().limit(60)
                .map(a -> eventApi.eventProfile(a.card().id()).map(p -> p.durationInHours()).orElse(null))
                .filter(Objects::nonNull).mapToLong(Long::longValue).sum();

        Map<Month, Integer> perMonth = new EnumMap<>(Month.class);
        attended.forEach(a -> perMonth.merge(Instant.ofEpochMilli(a.card().eventDate().getTime()).atZone(Semester.ZONE).getMonth(), 1, Integer::sum));
        Map.Entry<Month, Integer> busiest = perMonth.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);

        int friends = (int) requestRepository.involving(userId, List.of(BuddyRequest.Status.ACCEPTED)).stream()
                .filter(r -> within(r.getRespondedAt(), s)).map(r -> r.otherThan(userId)).distinct().count();
        int badges = (int) earnedRepository.findByUserId(userId).stream().filter(e -> within(e.getEarnedAt(), s)).count();

        return new Wrapped(s.key(), s.label(), attended.size(), hours,
                (int) attended.stream().filter(AttendedEvent::certificate).count(),
                top(attended, a -> a.card().category()), top(attended, AttendedEvent::organizedBy),
                busiest == null ? null : busiest.getKey().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                busiest == null ? 0 : busiest.getValue(), badges, friends,
                attended.isEmpty() ? null : attended.get(0).card().name(),
                attended.isEmpty() ? null : attended.get(attended.size() - 1).card().name(),
                attended.isEmpty() ? null : topPercent(attended.size(), eventApi.attendedCountsBetween(s.startDate(), s.endDate()).values()),
                recentSemesters());
    }

    /** The current semester and the two before it, for switching. */
    static List<String> recentSemesters() {
        Semester now = Semester.current();
        return List.of(now.key(), now.previous().key(), now.previous().previous().key());
    }

    /** "Top X%": the share of active students who attended at least as many events (rounded up). */
    static Integer topPercent(int mine, Collection<Long> everyone) {
        if (everyone.size() < MIN_STUDENTS_FOR_RANK) return null;
        long atLeast = everyone.stream().filter(c -> c >= mine).count();
        int percent = (int) Math.ceil(100.0 * Math.max(1, atLeast) / everyone.size());
        return percent <= 50 ? Math.max(1, percent) : null;   // only flattering when it's true
    }

    static List<Count> top(List<AttendedEvent> attended, Function<AttendedEvent, String> key) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> label = new HashMap<>();
        for (AttendedEvent a : attended) {
            String v = key.apply(a);
            if (v == null || v.isBlank()) continue;
            String k = v.trim().toLowerCase(Locale.ROOT);
            label.putIfAbsent(k, v.trim());
            counts.merge(k, 1, Integer::sum);
        }
        return counts.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(3)
                .map(e -> new Count(label.get(e.getKey()), e.getValue())).collect(Collectors.toList());
    }

    private static boolean within(LocalDateTime t, Semester s) {
        return t != null && !t.toLocalDate().isBefore(s.start()) && t.toLocalDate().isBefore(s.endExclusive());
    }
}
