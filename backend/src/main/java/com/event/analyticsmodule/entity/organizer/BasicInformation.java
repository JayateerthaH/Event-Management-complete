package com.event.analyticsmodule.entity.organizer;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(schema = "analytics")
public class BasicInformation {

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

    private Long totalEvents;
    private Long totalRegistration;
    private Long revenue;
    private Long totalAttended;

    private Long userId;

}