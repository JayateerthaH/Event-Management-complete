package com.event.authmodule.entitys;

import com.event.authmodule.entitys.type.RoleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "auth")
public class OnBoard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(
            name = "ann_gen",
            sequenceName = "announcement_seq",
            schema = "auth", // <--- CRITICAL
            allocationSize = 50
    )
    private Long id;

    private String username;
    @Enumerated(EnumType.STRING)
    private RoleType roleType;

    public OnBoard(String username, RoleType roleType) {
        this.username = username;
        this.roleType = roleType;
    }
}
