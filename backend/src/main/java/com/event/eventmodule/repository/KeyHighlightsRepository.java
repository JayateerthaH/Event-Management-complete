package com.event.eventmodule.repository;

import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.event.highlight.KeyHighlights;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KeyHighlightsRepository extends JpaRepository<KeyHighlights, Long> {
    Optional<KeyHighlights> findByEvents_Id(Long eventId);
}