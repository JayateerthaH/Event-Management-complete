package com.event.eventmodule.Registration;

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
public class Register {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    private String name;
    private String email;
    private String phone;
    private String department;
    private String yearOfStudy;
    private Boolean isAttended = false;

    private Long userId;

    @ManyToOne
    @JoinColumn(name = "event_id") // stored here
    @JsonBackReference
    private Events eventsDto;

}
