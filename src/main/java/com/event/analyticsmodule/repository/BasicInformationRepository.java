package com.event.analyticsmodule.repository;

import com.event.analyticsmodule.entity.organizer.BasicInformation;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BasicInformationRepository extends JpaRepository<BasicInformation, Long> {
    BasicInformation findByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(b.revenue), 0) FROM BasicInformation b")
    long sumRevenue();

    @Modifying
    @Transactional
    @Query("""
        UPDATE BasicInformation b
        SET
            b.totalEvents = b.totalEvents + 1,
            b.totalRegistration = b.totalRegistration + :totalRegistration,
            b.totalAttended = b.totalAttended + :totalAttended,
            b.revenue = b.revenue + :revenue
        WHERE b.userId = :userId
    """)
    void updateOrganizerStats(
            @Param("userId") Long userId,
            @Param("totalRegistration") Long totalRegistration,
            @Param("totalAttended") Long totalAttended,
            @Param("revenue") Long revenue
    );
}