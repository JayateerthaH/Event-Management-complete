package com.event.eventmodule.entity;

import com.event.eventmodule.Registration.Register;
import com.event.eventmodule.entity.announcement.Announcement;
import com.event.eventmodule.entity.event.*;
import com.event.eventmodule.entity.event.eventdetail.EventDetails;
import com.event.eventmodule.entity.event.highlight.KeyHighlights;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "events")
public class Events {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String category;
    private String eventField; // RENAMED
    private Date eventDate;
    private String venue;
    private Long maxPeople;
    private String costOfRegistration;
    private String imageUrl;

    private Boolean isFeatured=false;

    private Boolean isApproved=false;

    // Set once stats have been pushed to the dashboards; guards against double counting
    private Boolean isCompleted=false;

    private Long organizerId; //creator of event


    @OneToMany(mappedBy = "events",cascade = CascadeType.ALL,orphanRemoval = true)
    @JsonManagedReference
    private List<WhatToBring> whatToBring;

    @OneToMany(mappedBy = "events", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<FAQs> faQs = new ArrayList<>();

    @OneToMany(mappedBy = "events", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<EventSpeakers> speakers = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL,orphanRemoval = true)
    @JoinColumn(name = "eventSchedule_id")
    private EventSchedule eventSchedule;

    @OneToMany(mappedBy = "events", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @JsonManagedReference
    private List<Announcement> announcements = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL,orphanRemoval = true)
    @JoinColumn(name = "aboutEvent_id")
    @JsonManagedReference
    private AboutEvent aboutEvent;

    @OneToOne(cascade = CascadeType.ALL,orphanRemoval = true)
    @JoinColumn(name = "keyHighlights_id")
    private KeyHighlights keyHighlights;

    @OneToOne(cascade = CascadeType.ALL,orphanRemoval = true)
    @JoinColumn(name = "eventDetails_id")
    private EventDetails eventDetails;

    @OneToMany(mappedBy = "eventsDto", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Register> registers = new ArrayList<>();

}
