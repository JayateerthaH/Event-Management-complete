package com.event.socialmodule.settings;

import com.event.socialmodule.entity.BuddyNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationPrefsService {

    private final NotificationPrefsRepository repository;

    public record PrefsView(boolean friendRequests, boolean friendGoing, boolean newEvents, boolean badges,
                            boolean weeklyDigest, boolean digestEmail) {}

    /** Partial update: null leaves a setting unchanged. */
    public record PrefsUpdate(Boolean friendRequests, Boolean friendGoing, Boolean newEvents, Boolean badges,
                              Boolean weeklyDigest, Boolean digestEmail) {}

    @Transactional(readOnly = true)
    public PrefsView get(Long userId) {
        return view(repository.findById(userId).orElseGet(() -> new NotificationPrefs(userId)));
    }

    @Transactional
    public PrefsView update(Long userId, PrefsUpdate u) {
        NotificationPrefs p = repository.findById(userId).orElseGet(() -> new NotificationPrefs(userId));
        if (u.friendRequests() != null) p.setFriendRequests(u.friendRequests());
        if (u.friendGoing() != null) p.setFriendGoing(u.friendGoing());
        if (u.newEvents() != null) p.setNewEvents(u.newEvents());
        if (u.badges() != null) p.setBadges(u.badges());
        if (u.weeklyDigest() != null) p.setWeeklyDigest(u.weeklyDigest());
        if (u.digestEmail() != null) p.setDigestEmail(u.digestEmail());
        p.setUpdatedAt(LocalDateTime.now());
        return view(repository.save(p));
    }

    /** Whether this user wants in-app notifications of this type. */
    @Transactional(readOnly = true)
    public boolean allows(Long userId, BuddyNotification.Type type) {
        NotificationPrefs p = repository.findById(userId).orElse(null);
        if (p == null) return true;
        return switch (type) {
            case BUDDY_REQUEST, BUDDY_ACCEPTED -> p.isFriendRequests();
            case FRIEND_GOING -> p.isFriendGoing();
            case NEW_EVENT -> p.isNewEvents();
            case ACHIEVEMENT -> p.isBadges();
            case DIGEST -> p.isWeeklyDigest();
            case SEAT_OPENED -> true;   // they asked for the seat; always tell them
        };
    }

    private static PrefsView view(NotificationPrefs p) {
        return new PrefsView(p.isFriendRequests(), p.isFriendGoing(), p.isNewEvents(), p.isBadges(),
                p.isWeeklyDigest(), p.isDigestEmail());
    }
}
