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
public class Organizer {

    @Id
    private Long id;

    private String name;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private UserInfo userInfo;
}
