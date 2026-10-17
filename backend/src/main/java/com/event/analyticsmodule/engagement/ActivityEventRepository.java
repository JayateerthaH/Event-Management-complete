package com.event.analyticsmodule.engagement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {

    @Query("select count(distinct a.userId) from ActivityEvent a where a.createdAt >= :since and a.userId is not null")
    long activeUsersSince(@Param("since") LocalDateTime since);

    @Query("select a.type, count(a) from ActivityEvent a where a.createdAt >= :since group by a.type")
    List<Object[]> countsByTypeSince(@Param("since") LocalDateTime since);

    /** Rows: day (date), distinct active users. */
    @Query(value = """
            select cast(created_at as date) as day, count(distinct user_id)
            from analytics.activity_event where created_at >= :since and user_id is not null
            group by 1 order by 1""", nativeQuery = true)
    List<Object[]> dailyActiveSince(@Param("since") LocalDateTime since);

    /** Rows: user id, signup time, for signups in the window. */
    @Query("select a.userId, a.createdAt from ActivityEvent a where a.type = 'signup' and a.createdAt between :from and :to")
    List<Object[]> signupsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select count(a) > 0 from ActivityEvent a where a.userId = :userId and a.createdAt >= :from and a.createdAt < :to")
    boolean activeBetween(@Param("userId") Long userId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
