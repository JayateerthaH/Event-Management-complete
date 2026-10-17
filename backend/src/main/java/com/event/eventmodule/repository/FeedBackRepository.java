package com.event.eventmodule.repository;

import com.event.eventmodule.entity.feedback.FeedBack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedBackRepository extends JpaRepository<FeedBack, Long> {
    List<FeedBack> findAllByEventId(Long eventId);
    long countByUserId(Long userId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    
    void deleteAllByEventId(Long eventId);
}