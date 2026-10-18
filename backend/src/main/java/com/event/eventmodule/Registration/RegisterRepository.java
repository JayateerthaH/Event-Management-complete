package com.event.eventmodule.Registration;

import com.event.eventmodule.entity.analysis.MonthlyAttendanceCount;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface RegisterRepository extends JpaRepository<Register, Long> {

    @Query("""
        SELECT r.userId
        FROM Register r
        WHERE r.eventsDto.id = :eventId
          AND r.isAttended = true
    """)
    List<Long> findUserIdsByEventIdAndIsAttendedTrue(@Param("eventId") Long eventId);

    @Query("""
        SELECT COUNT(r)
        FROM Register r
        WHERE r.eventsDto.id = :eventId
    """)
    Long countRegistrationsByEventId(@Param("eventId") Long eventId);

    @Query("""
        SELECT COUNT(DISTINCT r.userId)
        FROM Register r
        WHERE r.eventsDto.id = :eventId
          AND LOWER(TRIM(r.department)) = LOWER(TRIM(:department))
          AND (r.userId IS NULL OR r.userId <> :excludeUserId)
    """)
    long countFromDepartment(@Param("eventId") Long eventId, @Param("department") String department,
                             @Param("excludeUserId") Long excludeUserId);

    /** Students who checked in at another of this organizer's events and haven't registered for this one. */
    @Query("""
        SELECT DISTINCT r.userId FROM Register r
        WHERE r.eventsDto.organizerId = :organizerId AND r.isAttended = true
          AND r.eventsDto.id <> :eventId AND r.userId IS NOT NULL
          AND r.userId NOT IN (SELECT r2.userId FROM Register r2 WHERE r2.eventsDto.id = :eventId AND r2.userId IS NOT NULL)
    """)
    List<Long> pastAttendeesNotRegistered(@Param("organizerId") Long organizerId, @Param("eventId") Long eventId);

    /** Per student: how many events dated in [from, to) they checked in at. Rows are [userId, count]. */
    @Query("""
        SELECT r.userId, COUNT(DISTINCT r.eventsDto.id) FROM Register r
        WHERE r.isAttended = true AND r.userId IS NOT NULL
          AND r.eventsDto.eventDate >= :from AND r.eventsDto.eventDate < :to
        GROUP BY r.userId
    """)
    List<Object[]> attendedCountsBetween(@Param("from") java.util.Date from, @Param("to") java.util.Date to);

    /** Rows: [department label, check-ins] for events dated in [from, to), grouped ignoring case and spaces ("Computer Science" = "ComputerScience"). */
    @Query("""
        SELECT MIN(TRIM(r.department)), COUNT(r) FROM Register r
        WHERE r.isAttended = true AND r.department IS NOT NULL AND TRIM(r.department) <> ''
          AND r.eventsDto.eventDate >= :from AND r.eventsDto.eventDate < :to
        GROUP BY LOWER(REPLACE(r.department, ' ', ''))
        ORDER BY COUNT(r) DESC
    """)
    List<Object[]> checkInsByDepartment(@Param("from") java.util.Date from, @Param("to") java.util.Date to);

    /** Check-ins at events dated in [from, to): rows are [userId, eventId, eventName]. */
    @Query("""
        SELECT DISTINCT r.userId, r.eventsDto.id, r.eventsDto.name FROM Register r
        WHERE r.isAttended = true AND r.userId IS NOT NULL
          AND r.eventsDto.eventDate >= :from AND r.eventsDto.eventDate < :to
    """)
    List<Object[]> checkInsBetween(@Param("from") java.util.Date from, @Param("to") java.util.Date to);

    @Query("SELECT r.department FROM Register r WHERE r.userId = :userId AND r.department IS NOT NULL ORDER BY r.id DESC")
    List<String> departmentsUsedBy(@Param("userId") Long userId, org.springframework.data.domain.Pageable page);

    @Query("SELECT r FROM Register r JOIN FETCH r.eventsDto e WHERE e.organizerId = :organizerId ORDER BY e.eventDate DESC, r.id")
    List<Register> findByEventOrganizer(@Param("organizerId") Long organizerId);

    @Query("SELECT COUNT(DISTINCT r.userId) FROM Register r")
    long countDistinctRegisteredUsers();

    @Query("SELECT r.userId FROM Register r WHERE r.eventsDto.id = :eventId")
    List<Long> findUserIdsByEventId(@Param("eventId") Long eventId);

    @Query(value = """
        SELECT
            to_char(months.month, 'Mon YYYY') AS month,
            COUNT(r.id) AS totalEvents
        FROM
            generate_series(
                date_trunc('month', CURRENT_DATE) - interval '5 months',
                date_trunc('month', CURRENT_DATE),
                interval '1 month'
            ) AS months(month)
        LEFT JOIN events.events e
            ON date_trunc('month', e.event_date) = months.month
        LEFT JOIN events.register r
            ON r.event_id = e.id
           AND r.user_id = :userId
           AND r.is_attended = true
        GROUP BY months.month
        ORDER BY months.month
        """,
            nativeQuery = true)
    List<MonthlyAttendanceCount> getUserAttendanceLast6Months(
            @Param("userId") Long userId
    );

    @Query(value = """
        SELECT
            to_char(months.month, 'Mon YYYY') AS month,
            COUNT(r.id) AS totalEvents
        FROM
            generate_series(
                date_trunc('month', CURRENT_DATE) - interval '5 months',
                date_trunc('month', CURRENT_DATE),
                interval '1 month'
            ) AS months(month)
        LEFT JOIN events.events e
            ON date_trunc('month', e.event_date) = months.month
           AND e.organizer_id = :organizerId
        LEFT JOIN events.register r
            ON r.event_id = e.id
        GROUP BY months.month
        ORDER BY months.month
        """,
            nativeQuery = true)
    List<MonthlyAttendanceCount> getOrganizerAttendanceLast6Months(@Param("organizerId") Long userId);

    @Query("""
        SELECT r
        FROM Register r
        WHERE r.userId = :userId
          AND r.isAttended = true
          AND EXISTS (
              SELECT 1
              FROM CertificateTemplate ct
              WHERE ct.eventId = r.eventsDto.id
          )
        """)
    List<Register> findEligibleCertificates(
            @Param("userId") Long userId
    );

    @Modifying
    @Transactional
    @Query("""
        UPDATE Register r
        SET r.isAttended = true
        WHERE r.userId = :userId
          AND r.eventsDto.id = :eventId
    """)
    void markAttended(
            @Param("userId") Long userId,
            @Param("eventId") Long eventId
    );

    @Query("select r.name from Register r " +
            "where r.userId = :userId " +
            "and r.eventsDto.id = :eventId " +
            "and r.isAttended = true " +
            "order by r.id desc")
    Optional<String> findNameByUserIdAndEventId(@Param("userId") Long userId,@Param("eventId") Long eventId);

    @Query("""
        SELECT r FROM Register r
        JOIN FETCH r.eventsDto e
        WHERE e.eventDate BETWEEN :start AND :end
    """)
    List<Register> findRegistrationsForEventsBetween(
            @Param("start") Date start,
            @Param("end") Date end
    );

    // RegisterRepository.java
    @Query("SELECT FUNCTION('MONTHNAME', e.eventDate) AS month, COUNT(r.id) AS count " +
            "FROM Register r JOIN r.eventsDto e " +
            "WHERE r.userId = :userId AND r.isAttended = true " +
            "GROUP BY FUNCTION('MONTH', e.eventDate), FUNCTION('MONTHNAME', e.eventDate)")
    List<MonthlyAttendanceCount> getMonthlyAttendanceCount(@Param("userId") Long userId);


    @Query("SELECT r FROM Register r JOIN FETCH r.eventsDto WHERE r.userId = :userId AND r.isAttended = true")
    List<Register> findAttendedByUserId(@Param("userId") Long userId);
}
