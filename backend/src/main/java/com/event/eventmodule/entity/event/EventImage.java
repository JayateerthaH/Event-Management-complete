package com.event.eventmodule.entity.event;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_image",schema = "events")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EventImage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_image_seq")
    @SequenceGenerator(name = "event_image_seq", sequenceName = "event_image_seq", allocationSize = 1)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
}
