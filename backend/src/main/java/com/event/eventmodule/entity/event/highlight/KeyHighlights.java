package com.event.eventmodule.entity.event.highlight;

import com.event.eventmodule.entity.Events;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "events")
public class KeyHighlights {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    @Column(name = "highlight_value")
    private String highlights;

    private String name;

    private String smallDescription;

    @OneToOne(mappedBy = "keyHighlights")
    @JsonBackReference
    private Events events;

}
