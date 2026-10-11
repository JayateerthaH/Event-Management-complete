package com.event.profilemodule.Repository;

import com.event.profilemodule.entity.student.SocialProfileKey;
import com.event.profilemodule.entity.student.SocialProfileUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface SocialProfileUpdateRepository extends JpaRepository<SocialProfileUpdate, SocialProfileKey> {
    Set<SocialProfileUpdate> findByIdUserId(Long userId);
}