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

    /** [average stars, number of ratings] across all of an organizer's events. */
    @org.springframework.data.jpa.repository.Query("""
        select avg(f.noOfStars), count(f) from FeedBack f, com.event.eventmodule.entity.Events e
        where e.id = f.eventId and e.organizerId = :organizerId and f.noOfStars > 0""")
    List<Object[]> ratingForOrganizer(@org.springframework.data.repository.query.Param("organizerId") Long organizerId);
    
    void deleteAllByEventId(Long eventId);
}