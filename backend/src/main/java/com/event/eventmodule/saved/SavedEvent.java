package com.event.eventmodule.saved;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** An event a student marked "Interested" (saved for later). */
@Entity
@Table(name = "saved_event", schema = "events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "eventId"}),
        indexes = @Index(columnList = "eventId"))
@Getter
@Setter
@NoArgsConstructor
public class SavedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long eventId;
    private LocalDateTime createdAt;

    public SavedEvent(Long userId, Long eventId) {
        this.userId = userId;
        this.eventId = eventId;
        this.createdAt = LocalDateTime.now();
    }
}
