package com.event.socialmodule.portfolio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A student's public portfolio page. Private until the student publishes it; the link can be reset. */
@Entity
@Table(name = "portfolio", schema = "social")
@Getter
@Setter
@NoArgsConstructor
public class Portfolio {

    @Id
    private Long userId;

    /** The unguessable part of the public link (/p/{slug}). */
    @Column(length = 40, unique = true, nullable = false)
    private String slug;

    private boolean published = false;

    private LocalDateTime updatedAt;

    public Portfolio(Long userId, String slug) {
        this.userId = userId;
        this.slug = slug;
        this.updatedAt = LocalDateTime.now();
    }
}
