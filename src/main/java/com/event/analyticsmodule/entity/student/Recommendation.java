package com.event.analyticsmodule.entity.student;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "analytics")
public class Recommendation {
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

    private Long userId;
}
