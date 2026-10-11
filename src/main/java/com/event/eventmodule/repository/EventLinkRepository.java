package com.event.eventmodule.repository;

import com.event.eventmodule.entity.feedback.EventLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventLinkRepository extends JpaRepository<EventLink, Long> {
    List<EventLink> findAllByEventIdIn(List<Long> eventIds);
    void deleteAllByEventId(Long eventId);
}