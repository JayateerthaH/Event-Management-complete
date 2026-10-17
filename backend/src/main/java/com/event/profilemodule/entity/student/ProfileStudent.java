package com.event.profilemodule.entity.student;


import com.event.profilemodule.entity.admin.Badges;
import com.event.profilemodule.entity.admin.Interests;
import com.event.profilemodule.entity.admin.Notifications;
import com.event.profilemodule.entity.admin.Preferences;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(schema = "profile")
public class ProfileStudent {

    @Id
    private Long id; // Set manually from Auth Service

    // --- Merged Profile Fields ---
    private String name;
    private String email;
    private Long phoneNumber;
    private String department;
    private String yearOfStudy;
    @Column(columnDefinition = "TEXT")
    private String bio;

    @Builder.Default
    private boolean privacy = false;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "users_interests",
            schema = "profile", // <--- THIS is how you tell the join table where to go
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "interest_id")
    )
    private Set<Interests> interests = new HashSet<>();

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "users_preferences",
            schema = "profile", // <--- THIS is how you tell the join table where to go
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "preference_id")
    )
    private Set<Preferences> preferences = new HashSet<>();

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "users_badges",
            schema = "profile", // <--- THIS is how you tell the join table where to go
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "badges_id")
    )
    private Set<Badges> badges = new HashSet<>();

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "users_notifications",
            schema = "profile", // <--- THIS is how you tell the join table where to go
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "notification_id")
    )
    private Set<Notifications> notifications = new HashSet<>();
}