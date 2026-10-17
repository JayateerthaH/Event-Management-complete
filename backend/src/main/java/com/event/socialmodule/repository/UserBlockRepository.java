package com.event.socialmodule.repository;

import com.event.socialmodule.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    /** Everyone the user blocked or was blocked by: they never see each other. */
    @Query("select case when b.blockerId = :u then b.blockedId else b.blockerId end from UserBlock b where b.blockerId = :u or b.blockedId = :u")
    List<Long> hiddenFrom(@Param("u") Long userId);

    List<UserBlock> findByBlockerId(Long blockerId);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}
