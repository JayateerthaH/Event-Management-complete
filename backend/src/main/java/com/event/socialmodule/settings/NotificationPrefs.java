package com.event.socialmodule.settings;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Which in-app notifications a user wants. Organizer announcements for registered events always arrive. */
@Entity
@Table(name = "notification_prefs", schema = "social")
@Getter
@Setter
@NoArgsConstructor
public class NotificationPrefs {

    @Id
    private Long userId;

    private boolean friendRequests = true;
    private boolean friendGoing = true;
    private boolean newEvents = true;
    private boolean badges = true;
    private boolean weeklyDigest = true;
    /** Also email the weekly digest (off by default; email is limited). */
    private boolean digestEmail = false;

    private LocalDateTime updatedAt;

    public NotificationPrefs(Long userId) {
        this.userId = userId;
    }
}
