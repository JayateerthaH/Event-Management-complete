package com.event.socialmodule.repository;

import com.event.socialmodule.entity.BuddyOptIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuddyOptInRepository extends JpaRepository<BuddyOptIn, Long> {
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    List<BuddyOptIn> findByEventId(Long eventId);
    List<BuddyOptIn> findByUserId(Long userId);
    void deleteByUserIdAndEventId(Long userId, Long eventId);
}
