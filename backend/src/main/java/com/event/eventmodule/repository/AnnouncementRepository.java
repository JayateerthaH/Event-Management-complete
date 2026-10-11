package com.event.eventmodule.repository;

import com.event.eventmodule.entity.announcement.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    @Query("SELECT a FROM Announcement a WHERE :userId MEMBER OF a.userIds ORDER BY a.createdAt DESC")
    List<Announcement> findByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM Announcement a WHERE a.events.id = :eventId AND :userId MEMBER OF a.userIds ORDER BY a.createdAt DESC")
    List<Announcement> findByEventIdAndUserId(@Param("eventId") Long eventId, @Param("userId") Long userId);
}
