package com.event.eventmodule.entity.event;

import com.event.eventmodule.entity.Events;
import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(schema = "events")
public class AboutEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    @Column(length = 300)
    private String about;

    @OneToOne(mappedBy = "aboutEvent")
    @JsonBackReference
    private Events events;

}
