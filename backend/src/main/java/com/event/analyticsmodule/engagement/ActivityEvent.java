package com.event.analyticsmodule.engagement;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** One counted action (signup, login, active, event_registered, attended, event_saved, buddy_request...). */
@Entity
@Table(name = "activity_event", schema = "analytics",
        indexes = {@Index(columnList = "createdAt"), @Index(columnList = "userId"), @Index(columnList = "type")})
@Getter
@NoArgsConstructor
public class ActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(length = 40, nullable = false)
    private String type;

    private Long eventId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ActivityEvent(Long userId, String type, Long eventId) {
        this.userId = userId;
        this.type = type;
        this.eventId = eventId;
        this.createdAt = LocalDateTime.now();
    }
}
