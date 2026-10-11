package com.event.participationmodule.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "users",schema = "attendance")
@AllArgsConstructor
@NoArgsConstructor
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long eventId;
    private String email;
    private String randomId;

    public Users(String random, String email, Long eventId) {
        this.randomId = random;
        this.email = email;
        this.eventId = eventId;
    }

}
