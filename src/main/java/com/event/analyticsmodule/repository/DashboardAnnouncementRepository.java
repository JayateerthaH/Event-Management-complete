package com.event.analyticsmodule.repository;

import com.event.analyticsmodule.entity.organizer.DashboardAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardAnnouncementRepository extends JpaRepository<DashboardAnnouncement, Long> {
}