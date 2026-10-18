package com.event.socialmodule.push;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One browser/phone that agreed to receive push notifications for a user. */
@Entity
@Table(name = "push_subscription", schema = "social", indexes = @Index(columnList = "userId"))
@Getter
@Setter
@NoArgsConstructor
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    /** The push service URL for this device (unique per browser install). */
    @Column(length = 1000, unique = true, nullable = false)
    private String endpoint;

    @Column(length = 200, nullable = false)
    private String p256dh;

    @Column(length = 100, nullable = false)
    private String auth;

    private LocalDateTime createdAt;
    private LocalDateTime lastSuccessAt;
}
