package com.event.authmodule.entitys;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(schema = "auth")
public class Student {

    @Id
    private Long id;
    private String name;
    @Column(nullable = false)
    private String email;

    @OneToOne
    @MapsId
    private UserInfo userInfo;
}
