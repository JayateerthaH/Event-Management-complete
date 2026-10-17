package com.event.eventmodule.entity.event;

import com.event.eventmodule.entity.Events;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "events")
public class EventSpeakers {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    private String name;
    private String profession;
    @Column(columnDefinition = "TEXT")
    private String shortExperience;

    private List<String> profile;

    @ManyToOne
    @JoinColumn(name = "event_id") // stored here
    @JsonBackReference
    private Events events;

}
