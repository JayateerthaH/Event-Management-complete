package com.event.eventmodule.repository;

import com.event.eventmodule.entity.event.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventImageRepository extends JpaRepository<EventImage, Long> {
    List<EventImage> findByEventIdOrderByUploadedAtDesc(Long eventId);
    void deleteAllByEventId(Long eventId);
}
