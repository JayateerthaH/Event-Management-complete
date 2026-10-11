package com.event.profilemodule.Repository;

import com.event.profilemodule.entity.admin.Notifications;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationsRepository extends JpaRepository<Notifications, Long> {
}