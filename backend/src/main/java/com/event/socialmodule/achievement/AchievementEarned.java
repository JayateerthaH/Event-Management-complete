package com.event.socialmodule.achievement;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** When a student first earned a badge (so they're told once, and the date can be shown). */
@Entity
@Table(name = "achievement_earned", schema = "social",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "achievementKey"}))
@Getter
@NoArgsConstructor
public class AchievementEarned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(length = 40)
    private String achievementKey;

    private LocalDateTime earnedAt;

    public AchievementEarned(Long userId, String achievementKey) {
        this.userId = userId;
        this.achievementKey = achievementKey;
        this.earnedAt = LocalDateTime.now();
    }
}
