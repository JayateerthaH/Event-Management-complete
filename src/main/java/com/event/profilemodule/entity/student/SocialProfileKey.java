package com.event.profilemodule.entity.student;

import jakarta.persistence.Embeddable;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@Data
public class SocialProfileKey implements Serializable {

    private Long platformId;
    private Long userId;

    public SocialProfileKey(Long platformId, Long userId) {
        this.platformId = platformId;
        this.userId = userId;
    }
}
