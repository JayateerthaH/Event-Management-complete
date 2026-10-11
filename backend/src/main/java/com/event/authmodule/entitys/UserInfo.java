package com.event.authmodule.entitys;

import com.event.authmodule.entitys.type.AuthProviderType;
import com.event.authmodule.entitys.type.RoleType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "app_user",indexes = {
        @Index(name = "idx_provider_id_provider_type",columnList = "providerId, providerType")
},schema = "auth")
public class UserInfo implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(
            name = "ann_gen",
            sequenceName = "announcement_seq",
            schema = "auth", // <--- CRITICAL
            allocationSize = 50
    )
    private Long userId;

    @Column(unique = true)
    private String username;
    private String password;


    private String providerId;

    @Enumerated(EnumType.STRING)
    private AuthProviderType providerType;

    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "user_info_roles",             // The name of the join table
            joinColumns = @JoinColumn(name = "user_info_user_id"),
            schema = "auth"                       // <--- THIS IS THE FIX
    )
    @Column(name = "role")
    Set<RoleType>roles = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role->new SimpleGrantedAuthority("ROLE_"+role.name()))
                .collect(Collectors.toSet());
    }

}
