package com.event.profilemodule.entity.student;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(schema = "profile")
public class SocialProfileUpdate {

    @EmbeddedId
    private SocialProfileKey id;
    
    @Column(columnDefinition = "TEXT")
    private String urlLink;

}
