package com.event.socialmodule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** "I'm open to meeting others at this event": visible only to other opted-in attendees. */
@Entity
@Table(name = "buddy_opt_in", schema = "social",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "eventId"}))
@Getter
@Setter
@NoArgsConstructor
public class BuddyOptIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long eventId;
    private LocalDateTime createdAt;

    public BuddyOptIn(Long userId, Long eventId) {
        this.userId = userId;
        this.eventId = eventId;
        this.createdAt = LocalDateTime.now();
    }
}
