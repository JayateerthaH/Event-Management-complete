package com.event.analyticsmodule.entity.organizer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "analytics")
public class DashboardAnnouncement {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(
            name = "ann_gen",
            sequenceName = "announcement_seq",
            schema = "analytics", // <--- CRITICAL
            allocationSize = 50
    )
    @Column(nullable = false)
    private Long id;

    private String title;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private Long eventId;

    private Long userId;
}
