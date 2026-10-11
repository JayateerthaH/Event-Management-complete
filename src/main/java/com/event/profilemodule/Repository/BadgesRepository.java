package com.event.profilemodule.Repository;

import com.event.profilemodule.entity.admin.Badges;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgesRepository extends JpaRepository<Badges, Long> {
}