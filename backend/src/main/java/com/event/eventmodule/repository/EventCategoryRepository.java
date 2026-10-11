package com.event.eventmodule.repository;

import com.event.eventmodule.entity.home.CategoryCountDto;
import com.event.eventmodule.entity.home.EventCategory;
//import com.example.Events.entity.home.HomeDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventCategoryRepository extends JpaRepository<EventCategory, Long> {

    @Query("""
    SELECT new com.event.eventmodule.entity.home.CategoryCountDto(
        e.category,
        COUNT(e.id)
    )
    FROM Events e
    WHERE e.isApproved = true
    GROUP BY e.category
""")
    List<CategoryCountDto> getCategoryCounts();
}