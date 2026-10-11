package com.event.analyticsmodule.repository;

import com.event.analyticsmodule.entity.student.DashboardBasicInfo;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DashboardBasicInfoRepository extends JpaRepository<DashboardBasicInfo, Long> {
    DashboardBasicInfo findByUserId(Long userId);

    void deleteByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("""
        UPDATE DashboardBasicInfo b
        SET
            b.hoursParticipated = b.hoursParticipated + :hours,
            b.eventsAttended = b.eventsAttended + 1,
            b.certificatesEarned = b.certificatesEarned + 1
        WHERE b.userId IN :userIds
    """)
    int updateAttendanceStats(
            @Param("userIds") List<Long> userIds,
            @Param("hours") Long hours
    );
}