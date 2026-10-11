package com.event.eventmodule.entity.event.eventdetail;

import com.event.eventmodule.entity.Events;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "events")
public class EventDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    private String organizedBy;          // Computer Science Department

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "event_departments",
            joinColumns = @JoinColumn(name = "event_detail_id")
    )
    @Column(name = "department")
    private Set<Department> departments; // Multiple departments

    private Long durationInHours;        // 9 Hours
    private String language;             // English
    private String level;                // All Levels Welcome
    private String contactEmail;         // techfest@college.edu
    private String contactPhone;         // +91-9876543210

    @OneToOne(mappedBy = "eventDetails")
    @JsonBackReference
    private Events events;

}
