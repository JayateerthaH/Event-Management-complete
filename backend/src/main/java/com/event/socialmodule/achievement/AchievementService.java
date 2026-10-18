package com.event.socialmodule.achievement;

import com.event.eventmodule.api.EventApi;
import com.event.socialmodule.achievement.AchievementRules.Achievement;
import com.event.socialmodule.entity.BuddyNotification;
import com.event.socialmodule.entity.BuddyRequest;
import com.event.socialmodule.repository.BuddyNotificationRepository;
import com.event.socialmodule.repository.BuddyRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Works out a student's badges, records newly earned ones and tells the student once. */
@Service
@RequiredArgsConstructor
public class AchievementService {

    /** How often a background check (from the notifications bell) may re-evaluate one student. */
    static final Duration RECHECK = Duration.ofMinutes(5);

    private final EventApi eventApi;
    private final BuddyRequestRepository requestRepository;
    private final AchievementEarnedRepository earnedRepository;
    private final BuddyNotificationRepository notificationRepository;
    private final com.event.socialmodule.settings.NotificationPrefsService prefs;
    private final com.event.socialmodule.goal.GoalService goalService;
    private final Map<Long, Instant> lastChecked = new ConcurrentHashMap<>();

    public record AchievementView(String key, String name, String description, int progress, int target,
                                  boolean earned, LocalDateTime earnedAt) {}

    public record Summary(int earned, int total, List<AchievementView> achievements) {}

    @Transactional
    public Summary forStudent(Long userId) {
        lastChecked.put(userId, Instant.now());
        int friends = (int) requestRepository.involving(userId, List.of(BuddyRequest.Status.ACCEPTED)).stream()
                .map(r -> r.otherThan(userId)).distinct().count();
        List<Achievement> all = AchievementRules.evaluate(eventApi.participation(userId), friends);

        Map<String, AchievementEarned> recorded = earnedRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(AchievementEarned::getAchievementKey, Function.identity(), (a, b) -> a));
        for (Achievement a : all) {
            if (a.earned() && !recorded.containsKey(a.key())) {
                AchievementEarned e = earnedRepository.save(new AchievementEarned(userId, a.key()));
                recorded.put(a.key(), e);
                if (prefs.allows(userId, BuddyNotification.Type.ACHIEVEMENT)) notificationRepository.save(new BuddyNotification(userId, BuddyNotification.Type.ACHIEVEMENT, null, null,
                        "You earned the \"" + a.name() + "\" badge: " + a.description().toLowerCase() + "."));
            }
        }
        List<AchievementView> views = all.stream().map(a -> new AchievementView(a.key(), a.name(), a.description(),
                a.progress(), a.target(), a.earned(),
                a.earned() && recorded.containsKey(a.key()) ? recorded.get(a.key()).getEarnedAt() : null)).toList();
        goalService.check(userId);   // a check-in that earns a badge may also complete the semester goal
        return new Summary((int) views.stream().filter(AchievementView::earned).count(), views.size(), views);
    }

    /** Badges the student has earned, without recording or notifying anything (for public pages). */
    @Transactional(readOnly = true)
    public List<AchievementView> earned(Long userId) {
        int friends = (int) requestRepository.involving(userId, List.of(BuddyRequest.Status.ACCEPTED)).stream()
                .map(r -> r.otherThan(userId)).distinct().count();
        Map<String, LocalDateTime> recorded = earnedRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(AchievementEarned::getAchievementKey, AchievementEarned::getEarnedAt, (a, b) -> a));
        return AchievementRules.evaluate(eventApi.participation(userId), friends).stream()
                .filter(Achievement::earned)
                .map(a -> new AchievementView(a.key(), a.name(), a.description(), a.progress(), a.target(), true,
                        recorded.get(a.key())))
                .toList();
    }

    /** Cheap re-check used when the bell loads, at most every few minutes per student. */
    @Transactional
    public void recheckIfDue(Long userId) {
        Instant last = lastChecked.get(userId);
        if (last == null || last.plus(RECHECK).isBefore(Instant.now())) forStudent(userId);
    }
}
