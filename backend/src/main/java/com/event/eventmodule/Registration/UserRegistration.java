package com.event.eventmodule.Registration;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "events")
public class UserRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    private Long userId;
    private Long registrationId;
    private Long eventId;

    public UserRegistration(Long userId, Long registrationId, Long eventId) {
        this.userId = userId;
        this.registrationId = registrationId;
        this.eventId = eventId;
    }

}
