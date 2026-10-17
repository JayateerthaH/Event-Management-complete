package com.event.socialmodule.digest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One digest per student per week, even if the job runs twice (restart, manual run). */
@Entity
@Table(name = "digest_sent", schema = "social", uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "weekStart"}))
@Getter
@NoArgsConstructor
public class DigestSent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private LocalDate weekStart;
    private boolean emailed;
    private LocalDateTime sentAt;

    public DigestSent(Long userId, LocalDate weekStart, boolean emailed) {
        this.userId = userId;
        this.weekStart = weekStart;
        this.emailed = emailed;
        this.sentAt = LocalDateTime.now();
    }
}
