package com.event.participationmodule.Repository;

import com.event.participationmodule.entity.UserRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRequestRepository extends JpaRepository<UserRequest, Long> {
    Optional<UserRequest> findByEmailAndEventId(String email, Long eventId);

    Optional<UserRequest> findFirstByUserIdAndEventId(Long userId, Long eventId);
}