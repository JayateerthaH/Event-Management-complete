package com.event.socialmodule.repository;

import com.event.socialmodule.entity.BuddySettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuddySettingsRepository extends JpaRepository<BuddySettings, Long> {
    List<BuddySettings> findByDiscoverableTrueAndDisplayNameIsNotNull();
}
