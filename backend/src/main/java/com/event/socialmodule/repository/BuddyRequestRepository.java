package com.event.socialmodule.repository;

import com.event.socialmodule.entity.BuddyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BuddyRequestRepository extends JpaRepository<BuddyRequest, Long> {

    /** Requests between two people, in either direction. */
    @Query("select r from BuddyRequest r where (r.fromUserId = :a and r.toUserId = :b) or (r.fromUserId = :b and r.toUserId = :a)")
    List<BuddyRequest> between(@Param("a") Long a, @Param("b") Long b);

    /** Requests involving the user with any of these statuses. */
    @Query("select r from BuddyRequest r where (r.fromUserId = :u or r.toUserId = :u) and r.status in :statuses order by r.createdAt desc")
    List<BuddyRequest> involving(@Param("u") Long userId, @Param("statuses") Collection<BuddyRequest.Status> statuses);

    long countByFromUserIdAndCreatedAtAfter(Long fromUserId, LocalDateTime after);
}
