package com.event.eventmodule.repository;

import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.event.EventSummaryProjection;
import com.event.eventmodule.entity.home.EventSummaryDto;
import org.springframework.data.domain.Pageable;    // ✔ CORRECT
import org.springframework.data.domain.Page;      //  ✔ CORRECT

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;


public interface EventsRepository extends JpaRepository<Events, Long>, JpaSpecificationExecutor<Events> {

    @Query("""
    SELECT new com.event.eventmodule.entity.home.EventSummaryDto(
        e.id,
        e.name,
        e.description,
        e.category,
        e.eventField,
        e.eventDate,
        e.venue,
        e.maxPeople,
        e.costOfRegistration,
        e.imageUrl,
        e.isApproved
    )
    FROM Events e
""")
    Page<EventSummaryDto> fetchEventSummaries(Pageable pageable);

    @Query("SELECT e.id AS id, e.name AS name, e.description AS description, e.category AS category, " +
            "e.eventField AS eventField, e.eventDate AS eventDate, e.venue AS venue, e.maxPeople AS maxPeople, " +
            "e.costOfRegistration AS costOfRegistration, e.imageUrl AS imageUrl " +
            "FROM Events e WHERE e.isFeatured = true AND e.isApproved = true")
    List<EventSummaryProjection> findFeaturedEventSummaries();

    Optional<Events> findByIdAndOrganizerId(Long eventId, Long organizerId);

    List<Events> findByOrganizerId(Long organizerId);

    long countByIsApprovedTrue();

    List<Events> findByIsApprovedTrueAndEventDateAfterOrderByEventDateAsc(java.util.Date after);

    @Query("SELECT DISTINCT e.category FROM Events e WHERE e.isApproved = true AND e.category IS NOT NULL ORDER BY e.category")
    List<String> findApprovedCategories();

    @Query("select e.eventDate from Events e where e.eventDate >= :from and e.eventDate < :to")
    List<Date> findEventDatesBetween(@org.springframework.data.repository.query.Param("from") Date from,
                                     @org.springframework.data.repository.query.Param("to") Date to);
}
