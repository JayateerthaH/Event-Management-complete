package com.event.analyticsmodule.entity.student;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(schema = "analytics")
public class DashboardBasicInfo {

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

    private Long eventsAttended;
    private Long hoursParticipated;
    private Long certificatesEarned;
    private Long achievements;

    private Long userId;

}
