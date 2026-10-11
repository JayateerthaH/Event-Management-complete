package com.event.participationmodule.Repository;

import com.event.participationmodule.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsByEmailAndDate(String email, LocalDate date);

    boolean existsByEmailAndEventIdAndDate(String email, Long eventId, LocalDate today);
}
