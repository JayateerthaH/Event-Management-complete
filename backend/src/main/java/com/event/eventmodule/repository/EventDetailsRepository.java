package com.event.eventmodule.repository;

import com.event.eventmodule.entity.event.eventdetail.EventDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventDetailsRepository extends JpaRepository<EventDetails, Long> {
    Optional<EventDetails> findByEvents_Id(Long eventId);
}