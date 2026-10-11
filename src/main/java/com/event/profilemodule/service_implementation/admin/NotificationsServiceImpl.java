package com.event.profilemodule.service_implementation.admin;

import com.event.profilemodule.Repository.NotificationsRepository;
import com.event.profilemodule.Services.NotificationsService;
import com.event.profilemodule.entity.admin.Notifications;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationsServiceImpl implements NotificationsService {

    private final NotificationsRepository notificationsRepository;

    @Override
    public Notifications createNotification(Notifications notification) {
        return notificationsRepository.save(notification);
    }

    @Override
    public Notifications getNotification(Long id) throws ResourceNotFoundException {
        return notificationsRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Notification not found with id {}", id);
                    return new ResourceNotFoundException("Notification not found with id " + id);
                });
    }

    @Override
    public List<Notifications> getAllNotifications() {
        return notificationsRepository.findAll();
    }

    @Override
    @Transactional
    public Notifications updateNotification(Long id, Notifications updatedNotification) throws ResourceNotFoundException {
        updatedNotification.setId(id);
        return notificationsRepository.save(updatedNotification);
    }

    @Override
    @Transactional
    public void deleteNotification(Long id) throws ResourceNotFoundException {
        // Spring Data 3+ deleteById silently ignores missing ids, so check explicitly
        if (!notificationsRepository.existsById(id)) {
            log.error("Attempted to delete non-existing notification with id {}", id);
            throw new ResourceNotFoundException("Notification not found with id " + id);
        }
        log.warn("Deleting notification with id {}", id);
        notificationsRepository.deleteById(id);
    }
}
