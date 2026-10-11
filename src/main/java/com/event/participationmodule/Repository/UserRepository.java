package com.event.participationmodule.Repository;

import com.event.participationmodule.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByRandomId(String randomId);

    Optional<Users> findFirstByEmailAndEventId(String email, Long eventId);
}
