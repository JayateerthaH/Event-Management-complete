package com.event.eventmodule.saved;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedEventRepository extends JpaRepository<SavedEvent, Long> {
    List<SavedEvent> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    long countByEventId(Long eventId);

    @org.springframework.data.jpa.repository.Query("select s.userId from SavedEvent s where s.eventId = :eventId")
    List<Long> findUserIdsByEventId(@org.springframework.data.repository.query.Param("eventId") Long eventId);

    /** Rows: event id, saves since then. */
    @org.springframework.data.jpa.repository.Query("select s.eventId, count(s) from SavedEvent s where s.createdAt >= :since group by s.eventId")
    List<Object[]> savesSince(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);
    void deleteByUserIdAndEventId(Long userId, Long eventId);
}
