package com.event.profilemodule.Repository;

import com.event.profilemodule.entity.admin.Interests;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestsRepository extends JpaRepository<Interests, Long> {
}