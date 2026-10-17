package com.event.eventmodule.calendar;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A student's private calendar subscription link (the token is the only secret in the URL). */
@Entity
@Table(name = "calendar_feed", schema = "events", uniqueConstraints = @UniqueConstraint(columnNames = "token"))
@Getter
@Setter
@NoArgsConstructor
public class CalendarFeed {

    @Id
    private Long userId;

    @Column(length = 64, nullable = false)
    private String token;

    private LocalDateTime createdAt;
}
