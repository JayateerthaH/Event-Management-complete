package com.event.eventmodule.saved;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedEventRepository extends JpaRepository<SavedEvent, Long> {
    List<SavedEvent> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    long countByEventId(Long eventId);
    void deleteByUserIdAndEventId(Long userId, Long eventId);
}
