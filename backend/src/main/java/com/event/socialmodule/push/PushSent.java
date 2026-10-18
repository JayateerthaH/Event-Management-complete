package com.event.socialmodule.push;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** A push sent to a user, so non-urgent pushes can be capped per week. */
@Entity
@Table(name = "push_sent", schema = "social", indexes = @Index(columnList = "userId, sentAt"))
@Getter
@NoArgsConstructor
public class PushSent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private boolean urgent;

    private LocalDateTime sentAt;

    public PushSent(Long userId, boolean urgent) {
        this.userId = userId;
        this.urgent = urgent;
        this.sentAt = LocalDateTime.now();
    }
}
