package com.event.profilemodule.entity.admin;

import com.event.profilemodule.entity.student.ProfileStudent;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;


import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "profile")
public class Preferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long preferenceId;
    private String preferenceType;

    @ManyToMany(mappedBy = "preferences")
    @JsonIgnore
    private Set<ProfileStudent> users = new HashSet<>();


}
