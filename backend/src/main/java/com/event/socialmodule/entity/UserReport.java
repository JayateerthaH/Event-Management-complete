package com.event.socialmodule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A report about another student, reviewed by admins (Reports / Flags). */
@Entity
@Table(name = "user_report", schema = "social")
@Getter
@Setter
@NoArgsConstructor
public class UserReport {

    public enum Status { OPEN, RESOLVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reporterId;
    private Long reportedId;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Status status = Status.OPEN;

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public UserReport(Long reporterId, Long reportedId, String reason) {
        this.reporterId = reporterId;
        this.reportedId = reportedId;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }
}
