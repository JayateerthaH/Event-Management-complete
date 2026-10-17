package com.event.socialmodule.achievement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementEarnedRepository extends JpaRepository<AchievementEarned, Long> {
    List<AchievementEarned> findByUserId(Long userId);
}
