package com.event.eventmodule.repository;


import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.event.EventSpeakers;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventSpeakersRepository extends JpaRepository<EventSpeakers, Long> {
    @Nullable List<EventSpeakers> findByEvents_Id(Long eventId);
}