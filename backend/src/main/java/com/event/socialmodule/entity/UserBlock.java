package com.event.socialmodule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Blocked people never see each other in Event buddies, search or friends, and can't send requests. */
@Entity
@Table(name = "user_block", schema = "social",
        uniqueConstraints = @UniqueConstraint(columnNames = {"blockerId", "blockedId"}))
@Getter
@Setter
@NoArgsConstructor
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long blockerId;
    private Long blockedId;
    private LocalDateTime createdAt;

    public UserBlock(Long blockerId, Long blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
        this.createdAt = LocalDateTime.now();
    }
}
