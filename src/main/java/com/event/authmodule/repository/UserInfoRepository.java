package com.event.authmodule.repository;

import com.event.authmodule.entitys.UserInfo;
import com.event.authmodule.entitys.type.AuthProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    Optional<UserInfo> findByUsername(String username);

    Optional<UserInfo> findByProviderIdAndProviderType(String providerId, AuthProviderType providerType);

    Optional<UserInfo> getUserIdByUsername(String username);
}