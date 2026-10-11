package com.event.analyticsmodule.repository;

import com.event.analyticsmodule.entity.student.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
}