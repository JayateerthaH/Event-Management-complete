package com.event.authmodule.repository;

import com.event.authmodule.entitys.UserInfo;
import com.event.authmodule.entitys.type.AuthProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    Optional<UserInfo> findByUsername(String username);

    @org.springframework.data.jpa.repository.Query(value = "select user_id from auth.app_user where 'STUDENT' = any(role)", nativeQuery = true)
    java.util.List<Long> findStudentIds();

    @org.springframework.data.jpa.repository.Query("select u.userId, u.username from UserInfo u where u.userId in :ids")
    java.util.List<Object[]> findUsernames(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);

    Optional<UserInfo> findByProviderIdAndProviderType(String providerId, AuthProviderType providerType);

    Optional<UserInfo> getUserIdByUsername(String username);
}