package com.event.eventmodule.entity.announcement;


import com.event.eventmodule.entity.Events;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "events")
public class Announcement {
    @Id
    // Explicit, unique sequence: the implicit name "announcement_seq" clashed with sequences other modules declare
    // under that name (schema update treats them as one), so it was never created and every insert failed.
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "events_announcement_gen")
    @SequenceGenerator(name = "events_announcement_gen", sequenceName = "event_announcement_seq",
            schema = "events", allocationSize = 50)
    @Column(nullable = false)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String announcement;
    private String title;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "event_id")
    @JsonBackReference   // ✅ Announcement is the CHILD (Many side)
    private Events events;

    @ElementCollection
    @CollectionTable(
            name = "announcement_users",
            joinColumns = @JoinColumn(name = "announcement_id")
    )
    @Column(name = "user_id")
    @JsonIgnore   // recipient list; students reading announcements must not see other users' ids
    private Set<Long> userIds = new HashSet<>();
}