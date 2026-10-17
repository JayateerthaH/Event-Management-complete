package com.event.eventmodule.Registration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {
    List<WaitlistEntry> findByEventIdOrderByCreatedAtAscIdAsc(Long eventId);
    Optional<WaitlistEntry> findByEventIdAndUserId(Long eventId, Long userId);
    long countByEventId(Long eventId);
}
