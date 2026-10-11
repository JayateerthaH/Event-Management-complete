package com.event.authmodule.repository;

import com.event.authmodule.entitys.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
}