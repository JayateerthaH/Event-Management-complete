package com.event.socialmodule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A student's choices for Event buddies. Nothing is shared until they opt in. */
@Entity
@Table(name = "buddy_settings", schema = "social")
@Getter
@Setter
@NoArgsConstructor
public class BuddySettings {

    @Id
    private Long userId;

    /** The name other students see (required before appearing anywhere). */
    @Column(length = 60)
    private String displayName;

    /** What friends see after both accept, e.g. an Instagram handle; optional. */
    @Column(length = 120)
    private String contact;

    /** Appear in "Find people" search, and be able to search. Off by default. */
    private boolean discoverable = false;

    /** Tell friends when I register for an event. On by default; friends are mutual. */
    private boolean shareAttendance = true;

    private LocalDateTime updatedAt;

    public BuddySettings(Long userId) {
        this.userId = userId;
    }
}
