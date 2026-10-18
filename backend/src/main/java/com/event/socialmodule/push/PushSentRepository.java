package com.event.socialmodule.push;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface PushSentRepository extends JpaRepository<PushSent, Long> {
    long countByUserIdAndUrgentFalseAndSentAtAfter(Long userId, LocalDateTime after);
}
