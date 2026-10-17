package com.event.socialmodule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A "Say hi" request. ACCEPTED means the two are friends (in either direction). */
@Entity
@Table(name = "buddy_request", schema = "social",
        indexes = {@Index(columnList = "fromUserId"), @Index(columnList = "toUserId")})
@Getter
@Setter
@NoArgsConstructor
public class BuddyRequest {

    public enum Status { PENDING, ACCEPTED, IGNORED, CANCELLED, REMOVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long fromUserId;
    private Long toUserId;

    /** The event where they met, if any. */
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Status status = Status.PENDING;

    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public BuddyRequest(Long fromUserId, Long toUserId, Long eventId) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.eventId = eventId;
        this.createdAt = LocalDateTime.now();
    }

    public boolean involves(Long userId) {
        return userId.equals(fromUserId) || userId.equals(toUserId);
    }

    public Long otherThan(Long userId) {
        return userId.equals(fromUserId) ? toUserId : fromUserId;
    }
}
