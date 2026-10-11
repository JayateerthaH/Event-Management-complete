package com.event.eventmodule.service.events;

import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.announcement.Announcement;
import com.event.eventmodule.repository.AnnouncementRepository;
import com.event.eventmodule.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final EventsRepository eventsRepository;

    public Announcement createAnnouncement(String title, String msg, Set<Long> userIds, Long eventId) {
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setAnnouncement(msg);
        announcement.setUserIds(userIds);
        Events event = eventsRepository.findById(eventId).orElse(null);
        announcement.setEvents(event);
        return announcementRepository.save(announcement);
    }

    public List<Announcement> getAnnouncementsForUser(Long userId) {
        return announcementRepository.findByUserId(userId);
    }

    public List<Announcement> getAnnouncementsForEventAndUser(Long eventId, Long userId) {
        return announcementRepository.findByEventIdAndUserId(eventId, userId);
    }
}
