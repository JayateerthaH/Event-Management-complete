package com.event.socialmodule.goal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** "Attend N events this semester": a long-horizon goal instead of a daily streak. */
@Entity
@Table(name = "semester_goal", schema = "social",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "semesterKey"}))
@Getter
@Setter
@NoArgsConstructor
public class SemesterGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    /** e.g. "2026-2" for July–December 2026. */
    @Column(length = 8)
    private String semesterKey;

    private int target;

    private LocalDateTime createdAt;

    /** When the student first reached the target (they're told once). */
    private LocalDateTime reachedAt;

    public SemesterGoal(Long userId, String semesterKey, int target) {
        this.userId = userId;
        this.semesterKey = semesterKey;
        this.target = target;
        this.createdAt = LocalDateTime.now();
    }
}
