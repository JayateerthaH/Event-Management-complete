package com.event.eventmodule.saved;

import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventCard;
import com.event.eventmodule.repository.EventsRepository;
import com.event.sharedmodule.UserActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** "Interested": students save events to come back to; saved events also shape their suggestions. */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class SavedEventController {

    static final int MAX_SAVED = 200;

    private final SavedEventRepository savedEventRepository;
    private final EventsRepository eventsRepository;
    private final EventApi eventApi;
    private final ApplicationEventPublisher events;

    @GetMapping("/saved")
    public List<EventCard> saved(@RequestAttribute("userId") String userId) {
        return eventApi.savedEvents(Long.valueOf(userId));
    }

    @GetMapping("/saved/ids")
    public List<Long> savedIds(@RequestAttribute("userId") String userId) {
        return savedEventRepository.findByUserIdOrderByCreatedAtDesc(Long.valueOf(userId)).stream()
                .map(SavedEvent::getEventId).toList();
    }

    @PostMapping("/{eventId:\\d+}/save")
    @Transactional
    public Map<String, Object> save(@RequestAttribute("userId") String userId, @PathVariable Long eventId) {
        Long me = Long.valueOf(userId);
        eventsRepository.findById(eventId).filter(e -> Boolean.TRUE.equals(e.getIsApproved()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        if (!savedEventRepository.existsByUserIdAndEventId(me, eventId)) {
            if (savedEventRepository.findByUserIdOrderByCreatedAtDesc(me).size() >= MAX_SAVED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "You can save up to " + MAX_SAVED + " events. Remove some you no longer need.");
            }
            savedEventRepository.save(new SavedEvent(me, eventId));
            events.publishEvent(new UserActivityEvent(me, "event_saved", eventId));
        }
        return Map.of("saved", true, "savedCount", savedEventRepository.countByEventId(eventId));
    }

    @DeleteMapping("/{eventId:\\d+}/save")
    @Transactional
    public Map<String, Object> unsave(@RequestAttribute("userId") String userId, @PathVariable Long eventId) {
        savedEventRepository.deleteByUserIdAndEventId(Long.valueOf(userId), eventId);
        return Map.of("saved", false, "savedCount", savedEventRepository.countByEventId(eventId));
    }
}
