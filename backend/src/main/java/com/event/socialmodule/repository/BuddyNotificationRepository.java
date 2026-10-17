package com.event.socialmodule.repository;

import com.event.socialmodule.entity.BuddyNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BuddyNotificationRepository extends JpaRepository<BuddyNotification, Long> {
    List<BuddyNotification> findTop30ByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("update BuddyNotification n set n.read = true where n.userId = :u and n.read = false")
    int markAllRead(@Param("u") Long userId);
}
