package com.event.authmodule.repository;

import com.event.authmodule.entitys.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    boolean existsByUserInfoUsername(String username);
}