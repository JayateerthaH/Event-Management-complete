package com.event.authmodule.repository;

import com.event.authmodule.entitys.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    RefreshToken findByToken(String token);

    /**
     * Atomically consumes a token. Returns 1 only for the request that actually removed it, so concurrent
     * refreshes or a refresh racing a logout can't both succeed (or fail with an optimistic-lock 500).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.token = :token")
    int deleteByTokenValue(@Param("token") String token);
}