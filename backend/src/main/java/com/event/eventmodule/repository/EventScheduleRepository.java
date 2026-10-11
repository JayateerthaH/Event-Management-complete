package com.event.eventmodule.repository;

import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.event.EventSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventScheduleRepository extends JpaRepository<EventSchedule, Long> {
    Optional<EventSchedule> findByEvents_Id(Long eventId);
}