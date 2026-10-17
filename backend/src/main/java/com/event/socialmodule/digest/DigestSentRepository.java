package com.event.socialmodule.digest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DigestSentRepository extends JpaRepository<DigestSent, Long> {
    boolean existsByUserIdAndWeekStart(Long userId, LocalDate weekStart);
    long countByWeekStartAndEmailedTrue(LocalDate weekStart);
}
