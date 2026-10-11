package com.event.eventmodule.service;

import com.event.eventmodule.Registration.RegisterRepository;
import com.event.eventmodule.entity.feedback.EventLink;
import com.event.eventmodule.entity.feedback.FeedBack;
import com.event.eventmodule.entity.feedback.FeedBackDto;
import com.event.eventmodule.entity.feedback.FeedBackTable;
import com.event.eventmodule.repository.EventLinkRepository;
import com.event.eventmodule.repository.EventsRepository;
import com.event.eventmodule.repository.FeedBackRepository;
import com.event.eventmodule.repository.FeedBackTableRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedBackService {
    private final FeedBackRepository feedBackRepository;
    private final RegisterRepository registerRepository;
    private final FeedBackTableRepository feedBackTableRepository;
    private final EventLinkRepository eventLinkRepository;
    private final EventsRepository eventsRepository;
    private final ModelMapper modelMapper;

    public List<FeedBackDto> getAllTheFeedback(Long eventId) {

        return feedBackRepository.findAllByEventId(eventId)
                .stream()
                .map(feedback -> modelMapper.map(feedback, FeedBackDto.class))
                .toList();

    }

    public List<EventLink> getAllEvents(Long userId) {

        // 1️⃣ Get eventIds for user
        List<Long> eventIds =
                feedBackTableRepository.findAllEventIdsByUserId(userId);

        if (eventIds.isEmpty()) {
            return List.of();
        }

        // 2️⃣ Fetch links for those events
        List<EventLink> links = eventLinkRepository.findAllByEventIdIn(eventIds);

        // 3️⃣ Populate event titles
        links.forEach(link -> eventsRepository.findById(link.getEventId())
                .ifPresent(event -> link.setEventTitle(event.getName())));

        return links;
    }

    public void createFeedBackRequest(EventLink eventLink) {
        List<Long> userIds = registerRepository.findUserIdsByEventIdAndIsAttendedTrue(eventLink.getEventId());

        List<FeedBackTable> records = userIds.stream()
                .map(userId -> {
                    FeedBackTable fb = new FeedBackTable();
                    fb.setUserId(userId);
                    fb.setEventId(eventLink.getEventId());
                    return fb;
                })
                .toList();

        feedBackTableRepository.saveAll(records);
        eventLinkRepository.save(eventLink);
    }

    @Transactional
    public FeedBackDto submitFeedBack(FeedBack feedBack) {

        feedBackTableRepository.deleteByUserIdAndEventId(feedBack.getUserId(), feedBack.getEventId());

        return modelMapper.map(feedBackRepository.save(feedBack), FeedBackDto.class);
    }
}
