package com.event.eventmodule.repository;

import com.event.eventmodule.entity.feedback.FeedBackTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedBackTableRepository extends JpaRepository<FeedBackTable, Long> {
    @Query("SELECT f.eventId FROM FeedBackTable f WHERE f.userId = :userId")
    List<Long> findAllEventIdsByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndEventId(Long userId, Long eventId);

    void deleteAllByEventId(Long eventId);

}