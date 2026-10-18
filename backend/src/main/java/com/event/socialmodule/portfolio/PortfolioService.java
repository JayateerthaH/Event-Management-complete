package com.event.socialmodule.portfolio;

import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventApi.AttendedEvent;
import com.event.profilemodule.communication.ProfileApi;
import com.event.socialmodule.achievement.AchievementService;
import com.event.socialmodule.achievement.AchievementService.AchievementView;
import com.event.socialmodule.repository.BuddySettingsRepository;
import com.event.sharedmodule.UserActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Student portfolio: QR-verified attendance, certificates and badges on one page a student can put on
 * a CV or LinkedIn. Only what the student chose to publish is shown; never email or phone.
 */
@Service
@RequiredArgsConstructor
public class PortfolioService {

    /** Most events a portfolio lists (newest first). */
    static final int MAX_EVENTS = 200;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PortfolioRepository portfolioRepository;
    private final EventApi eventApi;
    private final ProfileApi profileApi;
    private final AchievementService achievementService;
    private final BuddySettingsRepository buddySettingsRepository;
    private final ApplicationEventPublisher activityPublisher;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public record Badge(String key, String name, String description, LocalDateTime earnedAt) {}

    public record Entry(Long eventId, String name, String category, Date date, String venue,
                        String organizedBy, boolean certificate) {}

    public record Stats(int events, int certificates, int badges, int categories) {}

    public record View(String name, String department, String yearOfStudy, String bio, List<String> interests,
                       Stats stats, List<String> categories, List<Badge> badges, List<Entry> events) {}

    /** The owner's view: their settings, link and what the public page shows. */
    public record Mine(boolean published, String url, View portfolio) {}

    @Transactional
    public Mine mine(Long userId) {
        Portfolio p = portfolioRepository.findById(userId).orElseGet(() -> create(userId));
        achievementService.forStudent(userId);   // records newly earned badges, so their dates show
        return new Mine(p.isPublished(), url(p.getSlug()), build(userId));
    }

    @Transactional
    public Mine setPublished(Long userId, boolean published) {
        Portfolio p = portfolioRepository.findById(userId).orElseGet(() -> create(userId));
        if (published && !p.isPublished()) activityPublisher.publishEvent(new UserActivityEvent(userId, "portfolio_published", null));
        p.setPublished(published);
        p.setUpdatedAt(LocalDateTime.now());
        portfolioRepository.save(p);
        return new Mine(p.isPublished(), url(p.getSlug()), build(userId));
    }

    /** A new link; the old one stops working. */
    @Transactional
    public Mine resetLink(Long userId) {
        Portfolio p = portfolioRepository.findById(userId).orElseGet(() -> create(userId));
        p.setSlug(newSlug());
        p.setUpdatedAt(LocalDateTime.now());
        portfolioRepository.save(p);
        return new Mine(p.isPublished(), url(p.getSlug()), build(userId));
    }

    /** The public page, if the slug exists and its owner published it. */
    @Transactional(readOnly = true)
    public Optional<View> published(String slug) {
        if (slug == null || !slug.matches("[A-Za-z0-9_-]{8,40}")) return Optional.empty();
        return portfolioRepository.findBySlug(slug).filter(Portfolio::isPublished).map(p -> build(p.getUserId()));
    }

    View build(Long userId) {
        ProfileApi.PublicProfile profile = profileApi.publicProfiles(List.of(userId)).get(userId);
        String name = profile != null && profile.name() != null ? profile.name()
                : buddySettingsRepository.findById(userId).map(s -> s.getDisplayName())
                        .filter(n -> n != null && !n.isBlank()).orElse("CampusEvents student");

        List<AttendedEvent> attended = eventApi.attendedEvents(userId);
        List<Entry> events = attended.stream().limit(MAX_EVENTS)
                .map(a -> new Entry(a.card().id(), a.card().name(), a.card().category(), a.card().eventDate(),
                        a.card().venue(), a.organizedBy(), a.certificate()))
                .toList();
        List<String> categories = categories(attended);
        List<Badge> badges = achievementService.earned(userId).stream()
                .sorted(Comparator.comparing(AchievementView::earnedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(a -> new Badge(a.key(), a.name(), a.description(), a.earnedAt()))
                .toList();
        int certificates = (int) attended.stream().filter(AttendedEvent::certificate).count();
        return new View(name,
                profile == null ? null : profile.department(),
                profile == null ? null : profile.yearOfStudy(),
                profileApi.bio(userId).orElse(null),
                profile == null ? List.of() : profile.interests(),
                new Stats(attended.size(), certificates, badges.size(), categories.size()),
                categories, badges, events);
    }

    /** Distinct categories, most attended first. */
    static List<String> categories(List<AttendedEvent> attended) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> label = new HashMap<>();
        for (AttendedEvent a : attended) {
            String c = a.card().category();
            if (c == null || c.isBlank()) continue;
            String key = c.trim().toLowerCase(Locale.ROOT);
            label.putIfAbsent(key, c.trim());
            counts.merge(key, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> label.get(e.getKey())).toList();
    }

    private Portfolio create(Long userId) {
        return portfolioRepository.save(new Portfolio(userId, newSlug()));
    }

    private static String newSlug() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    String url(String slug) {
        return frontendUrl.replaceAll("/+$", "") + "/p/" + slug;
    }
}
