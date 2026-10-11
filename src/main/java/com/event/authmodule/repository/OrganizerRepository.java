package com.event.authmodule.repository;

import com.event.authmodule.entitys.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizerRepository extends JpaRepository<Organizer, Long> {
    boolean existsByUserInfoUsername(String username);
}