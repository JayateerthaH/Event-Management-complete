package com.event.eventmodule.repository;


import com.event.eventmodule.entity.event.AboutEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AboutEventRepository extends JpaRepository<AboutEvent, Long> {
    Optional<AboutEvent> findByEvents_Id(Long eventId);
}