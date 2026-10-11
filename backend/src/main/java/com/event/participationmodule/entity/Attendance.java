// com.attendance.Attendance.entity.Attendance.java

package com.event.participationmodule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance",schema = "attendance")
@Getter
@Setter
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private Long eventId;

    private LocalDate date; // <-- ADD THIS FIELD

    private LocalDateTime timestamp;

    public Attendance() {}

    public Attendance(String email, LocalDate date, LocalDateTime timestamp) {
        this.email = email;
        this.date = date;
        this.timestamp = timestamp;
    }

    public Attendance(String email, Long eventId, LocalDate date, LocalDateTime timestamp) {
        this.email = email;
        this.eventId = eventId;
        this.date = date;
        this.timestamp = timestamp;
    }

}
