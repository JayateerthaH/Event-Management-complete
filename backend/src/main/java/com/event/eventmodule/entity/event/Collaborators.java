package com.event.eventmodule.entity.event;

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
public class Collaborators {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    private String email;
    private Long eventId;

    public Collaborators(Long eventId, String collabUserId) {
        this.email = collabUserId;
        this.eventId = eventId;
    }
}
