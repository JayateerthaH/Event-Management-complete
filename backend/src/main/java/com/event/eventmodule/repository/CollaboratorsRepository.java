package com.event.eventmodule.repository;

import com.event.eventmodule.entity.event.Collaborators;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CollaboratorsRepository extends JpaRepository<Collaborators, Long> {
    @Query("SELECT c.email FROM Collaborators c WHERE c.eventId = :eventId")
    List<String> findEmailByEventId(Long eventId);

    boolean existsByEventIdAndEmail(Long eventId, String email);

    @Query("SELECT c.eventId FROM Collaborators c WHERE c.email = :email")
    List<Long> findEventIdsByEmail(@Param("email") String email);

    void deleteAllByEventId(Long eventId);
}