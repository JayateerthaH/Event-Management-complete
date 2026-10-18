package com.event.socialmodule.goal;

import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventCard;
import com.event.socialmodule.entity.BuddyNotification;
import com.event.socialmodule.repository.BuddyNotificationRepository;
import com.event.socialmodule.settings.NotificationPrefsService;
import com.event.sharedmodule.UserActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Semester goals: progress counts only QR-verified attendance at events dated in the semester. */
@Service
@RequiredArgsConstructor
public class GoalService {

    static final int MAX_TARGET = 50;

    private final SemesterGoalRepository goalRepository;
    private final EventApi eventApi;
    private final BuddyNotificationRepository notificationRepository;
    private final NotificationPrefsService prefs;
    private final ApplicationEventPublisher activityPublisher;

    public record GoalView(String semester, LocalDate start, LocalDate end, Integer target, int attended,
                           int registeredUpcoming, boolean reached, LocalDateTime reachedAt, int lastSemesterAttended) {}

    @Transactional
    public GoalView view(Long userId) {
        Semester s = Semester.current();
        Optional<SemesterGoal> goal = goalRepository.findByUserIdAndSemesterKey(userId, s.key());
        int attended = attended(userId, s);
        goal.ifPresent(g -> recordIfReached(g, attended));
        long now = System.currentTimeMillis();
        int upcoming = (int) eventApi.registeredEvents(userId).stream()
                .filter(e -> e.eventDate() != null && e.eventDate().getTime() > now && s.contains(e.eventDate()))
                .count();
        return new GoalView(s.label(), s.start(), s.end(), goal.map(SemesterGoal::getTarget).orElse(null), attended, upcoming,
                goal.map(g -> g.getReachedAt() != null).orElse(false), goal.map(SemesterGoal::getReachedAt).orElse(null),
                attended(userId, s.previous()));
    }

    @Transactional
    public GoalView set(Long userId, int target) {
        if (target < 1 || target > MAX_TARGET) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pick a goal between 1 and " + MAX_TARGET + " events");
        }
        Semester s = Semester.current();
        SemesterGoal goal = goalRepository.findByUserIdAndSemesterKey(userId, s.key())
                .orElseGet(() -> new SemesterGoal(userId, s.key(), target));
        if (goal.getId() == null) activityPublisher.publishEvent(new UserActivityEvent(userId, "goal_set", null));
        if (goal.getTarget() != target) goal.setReachedAt(null);   // a new target can be reached (and announced) again
        goal.setTarget(target);
        goalRepository.save(goal);
        return view(userId);
    }

    @Transactional
    public GoalView clear(Long userId) {
        goalRepository.findByUserIdAndSemesterKey(userId, Semester.current().key()).ifPresent(goalRepository::delete);
        return view(userId);
    }

    /** Called when badges are re-evaluated (dashboard, bell), so reaching the goal is announced promptly. */
    @Transactional
    public void check(Long userId) {
        Semester s = Semester.current();
        goalRepository.findByUserIdAndSemesterKey(userId, s.key())
                .filter(g -> g.getReachedAt() == null)
                .ifPresent(g -> recordIfReached(g, attended(userId, s)));
    }

    private void recordIfReached(SemesterGoal goal, int attended) {
        if (goal.getReachedAt() != null || attended < goal.getTarget()) return;
        goal.setReachedAt(LocalDateTime.now());
        goalRepository.save(goal);
        if (prefs.allows(goal.getUserId(), BuddyNotification.Type.ACHIEVEMENT)) {
            notificationRepository.save(new BuddyNotification(goal.getUserId(), BuddyNotification.Type.ACHIEVEMENT, null, null,
                    "Semester goal reached: you attended " + goal.getTarget() + (goal.getTarget() == 1 ? " event" : " events")
                            + " this semester."));
        }
    }

    private int attended(Long userId, Semester s) {
        return countIn(eventApi.attendedEvents(userId).stream().map(a -> a.card()).toList(), s);
    }

    static int countIn(List<EventCard> events, Semester s) {
        return (int) events.stream().map(EventCard::eventDate).filter(s::contains).count();
    }
}
