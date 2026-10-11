package com.event.profilemodule.Services;

import com.event.profilemodule.entity.admin.Notifications;
import com.event.sharedmodule.exception.ResourceNotFoundException;

import java.util.List;

public interface NotificationsService {

    Notifications createNotification(Notifications notification);

    Notifications getNotification(Long id) throws ResourceNotFoundException;

    List<Notifications> getAllNotifications();

    Notifications updateNotification(Long id, Notifications notification) throws ResourceNotFoundException;

    void deleteNotification(Long id) throws ResourceNotFoundException;
}
