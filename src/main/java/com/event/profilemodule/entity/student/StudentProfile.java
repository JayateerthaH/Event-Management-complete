package com.event.profilemodule.entity.student;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Builder
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "profile")
public class StudentProfile {

    @Id
    // No @GeneratedValue needed! The ID comes from the User.
    @Column(name = "student_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // <--- MAGIC ANNOTATION: Copies the ID from 'student' into 'id'
    @JoinColumn(name = "student_id")
    @JsonIgnore // Prevent infinite loop in JSON response
    private ProfileStudent student;

    private String name;
    private String email;
    private Long phoneNumber;
    private String department;
    private String yearOfStudy;
    private String bio;

    @Builder.Default
    private boolean privacy = false;
}