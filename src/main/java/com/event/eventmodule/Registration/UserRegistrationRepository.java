package com.event.eventmodule.Registration;

import com.event.eventmodule.entity.Events;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface UserRegistrationRepository extends JpaRepository<UserRegistration, Long> {
    List<UserRegistration> findByUserId(Long userId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    void deleteAllByEventId(Long eventId);

}