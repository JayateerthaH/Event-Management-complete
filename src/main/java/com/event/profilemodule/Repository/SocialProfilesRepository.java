package com.event.profilemodule.Repository;

import com.event.profilemodule.entity.admin.SocialProfiles;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialProfilesRepository extends JpaRepository<SocialProfiles, Long> {
}