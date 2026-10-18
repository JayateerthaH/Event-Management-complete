package com.event.socialmodule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** In-app notification shown in the navbar bell. */
@Entity
@Table(name = "buddy_notification", schema = "social", indexes = @Index(columnList = "userId"))
@EntityListeners(com.event.socialmodule.push.BellNotificationEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class BuddyNotification {

    public enum Type { BUDDY_REQUEST, BUDDY_ACCEPTED, FRIEND_GOING, ACHIEVEMENT, NEW_EVENT, DIGEST, SEAT_OPENED, EVENT_RECAP }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Who sees it. */
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Type type;

    /** Who it's about. */
    private Long actorUserId;
    private Long eventId;

    @Column(length = 255)
    private String message;

    private boolean read = false;
    private LocalDateTime createdAt;

    public BuddyNotification(Long userId, Type type, Long actorUserId, Long eventId, String message) {
        this.userId = userId;
        this.type = type;
        this.actorUserId = actorUserId;
        this.eventId = eventId;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }
}
