package com.event.eventmodule.Registration;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A place in a full event's waitlist, with the registration details to use when a seat opens. */
@Entity
@Table(name = "waitlist_entry", schema = "events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"eventId", "userId"}),
        indexes = @Index(columnList = "eventId, createdAt"))
@Getter
@Setter
@NoArgsConstructor
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String department;
    private String yearOfStudy;
    private LocalDateTime createdAt;
}
