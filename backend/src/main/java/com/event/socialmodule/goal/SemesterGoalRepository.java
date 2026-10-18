package com.event.socialmodule.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SemesterGoalRepository extends JpaRepository<SemesterGoal, Long> {
    Optional<SemesterGoal> findByUserIdAndSemesterKey(Long userId, String semesterKey);
}
