package com.event.authmodule.entitys;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(schema = "auth")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(
            name = "refresh_token_gen",
            sequenceName = "refresh_token_seq",
            schema = "auth",           // <--- THIS IS THE FIX
            allocationSize = 50
    )
    @Column(nullable = false)
    private Long id;

    private String token;

    private Instant expiryTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserInfo userInfo;

}
