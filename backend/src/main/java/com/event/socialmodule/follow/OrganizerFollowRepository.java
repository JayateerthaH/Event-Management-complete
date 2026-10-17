package com.event.socialmodule.follow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizerFollowRepository extends JpaRepository<OrganizerFollow, Long> {
    boolean existsByFollowerIdAndOrganizerId(Long followerId, Long organizerId);
    long countByOrganizerId(Long organizerId);
    List<OrganizerFollow> findByOrganizerId(Long organizerId);
    List<OrganizerFollow> findByFollowerIdOrderByCreatedAtDesc(Long followerId);
    void deleteByFollowerIdAndOrganizerId(Long followerId, Long organizerId);
}
