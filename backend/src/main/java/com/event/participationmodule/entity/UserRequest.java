package com.event.participationmodule.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(schema = "attendance")
public class UserRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    private String email;
    private Long userId;
    private Long eventId;
    private Boolean isAttended;

    public UserRequest(Long eventId, String email, Long userId) {
        this.email = email;
        this.userId = userId;
        this.eventId = eventId;
    }
}
