package com.event.socialmodule.follow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** A student following an organizer (club/department) to hear about their new events. */
@Entity
@Table(name = "organizer_follow", schema = "social",
        uniqueConstraints = @UniqueConstraint(columnNames = {"followerId", "organizerId"}),
        indexes = @Index(columnList = "organizerId"))
@Getter
@NoArgsConstructor
public class OrganizerFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long followerId;
    private Long organizerId;
    private LocalDateTime createdAt;

    public OrganizerFollow(Long followerId, Long organizerId) {
        this.followerId = followerId;
        this.organizerId = organizerId;
        this.createdAt = LocalDateTime.now();
    }
}
