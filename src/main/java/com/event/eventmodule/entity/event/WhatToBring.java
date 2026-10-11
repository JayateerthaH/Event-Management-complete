package com.event.eventmodule.entity.event;

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
public class WhatToBring {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String item;

    @ManyToOne
    @JoinColumn(name = "event_id")
    @JsonBackReference
    private Events events;
}
