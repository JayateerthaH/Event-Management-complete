package com.event.profilemodule.Repository;

import com.event.profilemodule.entity.admin.Interests;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterestsRepository extends JpaRepository<Interests, Long> {
    Optional<Interests> findFirstByInterestNameIgnoreCase(String interestName);
}