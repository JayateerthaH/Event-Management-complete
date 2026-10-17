package com.event.eventmodule.api;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import com.event.eventmodule.repository.FeedBackRepository;
import com.event.eventmodule.filter.EventService;
import com.event.eventmodule.config.EventAccessService;
import com.event.eventmodule.Registration.RegisterRepository;
import com.event.eventmodule.Registration.RegistrationService;
import com.event.eventmodule.dtoPayload.EventsDto;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.analysis.MonthlyAttendanceCount;
import com.event.eventmodule.repository.CollaboratorsRepository;
import com.event.eventmodule.repository.EventsRepository;
import com.event.eventmodule.entity.event.EventSchedule;
import com.event.eventmodule.repository.AboutEventRepository;
import com.event.eventmodule.repository.EventDetailsRepository;
import com.event.eventmodule.repository.EventScheduleRepository;
import com.event.eventmodule.repository.EventSpeakersRepository;
import com.event.eventmodule.repository.FAQsRepository;
import com.event.eventmodule.repository.KeyHighlightsRepository;
import com.event.eventmodule.repository.WhatToBringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Public facade of the event module for other modules (analytics, participation).
 * Split out of RegistrationService so that class only handles registrations.
 */
@Service
@RequiredArgsConstructor
public class EventApiImpl implements EventApi {

    private final RegistrationService registrationService;
    private final com.event.eventmodule.saved.SavedEventRepository savedEventRepository;
    private final com.event.eventmodule.Registration.UserRegistrationRepository userRegistrationRepository;
    private final RegisterRepository registerRepository;
    private final EventsRepository eventsRepository;
    private final CollaboratorsRepository collaboratorsRepository;
    private final EventService eventService;
    private final EventAccessService eventAccessService;
    private final FeedBackRepository feedBackRepository;
    private final AboutEventRepository aboutEventRepository;
    private final EventDetailsRepository eventDetailsRepository;
    private final EventScheduleRepository eventScheduleRepository;
    private final FAQsRepository faqsRepository;
    private final EventSpeakersRepository speakersRepository;
    private final WhatToBringRepository whatToBringRepository;
    private final KeyHighlightsRepository keyHighlightsRepository;

    @Override
    public List<EventsDto> getPendingEvents(Long userId) {
        return registrationService.getUpcomingEventsForUser(userId).stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public List<EventsDto> getPastEvents(Long userId) {
        return registrationService.getPastEventsForUser(userId).stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public List<MonthlyAttendanceCount> getAttendanceCount(Long userId) {
        return registerRepository.getUserAttendanceLast6Months(userId);
    }

    @Override
    public List<MonthlyAttendanceCount> getRegisteredCount(Long userId) {
        return registerRepository.getOrganizerAttendanceLast6Months(userId);
    }

    @Override
    public void markAttendance(Long userId, Long eventId) {
        registerRepository.markAttended(userId, eventId);
    }

    @Override
    public boolean canScan(Long eventId, String email, Long userId) {
        if (collaboratorsRepository.existsByEventIdAndEmail(eventId, email)) {
            return true;
        }
        Events event = eventsRepository.findById(eventId).orElse(null);
        return event != null
                && event.getOrganizerId() != null
                && event.getOrganizerId().equals(userId);
    }

    @Override
    public long countAllEvents() {
        return eventsRepository.count();
    }

    @Override
    public List<EventCard> savedEvents(Long userId) {
        List<Long> ids = savedEventRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(com.event.eventmodule.saved.SavedEvent::getEventId).toList();
        Map<Long, Events> byId = new HashMap<>();
        eventsRepository.findAllById(ids).forEach(e -> byId.put(e.getId(), e));
        return ids.stream().map(byId::get)
                .filter(e -> e != null && Boolean.TRUE.equals(e.getIsApproved()))
                .map(this::toCard).toList();
    }

    @Override
    public boolean isRegistered(Long userId, Long eventId) {
        return userId != null && eventId != null && userRegistrationRepository.existsByUserIdAndEventId(userId, eventId);
    }

    @Override
    public java.util.Map<YearMonth, Long> eventsPerMonth(YearMonth from, YearMonth to) {
        ZoneId zone = ZoneId.systemDefault();
        java.util.Map<YearMonth, Long> counts = new LinkedHashMap<>();
        for (YearMonth m = from; !m.isAfter(to); m = m.plusMonths(1)) counts.put(m, 0L);
        Date start = Date.from(from.atDay(1).atStartOfDay(zone).toInstant());
        Date end = Date.from(to.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant());
        for (Date d : eventsRepository.findEventDatesBetween(start, end)) {
            // getTime() works for java.sql.Date too, whose toInstant() throws
            YearMonth m = YearMonth.from(Instant.ofEpochMilli(d.getTime()).atZone(zone));
            counts.computeIfPresent(m, (k, v) -> v + 1);
        }
        return counts;
    }

    @Override
    public List<EventCard> upcomingOpenEvents() {
        return eventsRepository.findByIsApprovedTrueAndEventDateAfterOrderByEventDateAsc(new Date()).stream()
                .filter(e -> !Boolean.TRUE.equals(e.getIsCompleted()))
                .map(this::toCard)
                .toList();
    }

    @Override
    public List<EventCard> registeredEvents(Long userId) {
        return java.util.stream.Stream.concat(
                        registrationService.getUpcomingEventsForUser(userId).stream(),
                        registrationService.getPastEventsForUser(userId).stream())
                .map(this::toCard)
                .toList();
    }

    @Override
    public List<EventCard> searchEvents(EventSearchCriteria c, int limit) {
        return eventService.filterEvents(c.category(), c.keywords(), c.venue(), c.department(), c.dateRange(),
                        c.costType(), PageRequest.of(0, Math.max(1, limit), Sort.by("eventDate").ascending()))
                .getContent().stream()
                .map(s -> new EventCard(s.getId(), s.getName(), s.getDescription(), s.getCategory(), s.getField(),
                        s.getEventDate(), s.getVenue(), s.getMaxPeople(), s.getCostOfRegistration(), s.getImageUrl(),
                        registerRepository.countRegistrationsByEventId(s.getId())))
                .toList();
    }

    @Override
    public List<String> eventCategories() {
        return eventsRepository.findApprovedCategories();
    }

    @Override
    public boolean canManageEvent(Long eventId, String userId, String roles, String email) {
        return eventAccessService.isAllowed(eventId, userId, roles, email);
    }

    @Override
    public List<FeedbackEntry> feedbackForEvent(Long eventId) {
        return feedBackRepository.findAllByEventId(eventId).stream()
                .map(f -> new FeedbackEntry(f.getNoOfStars() == null ? 0 : f.getNoOfStars(), f.getFeedBack()))
                .toList();
    }

    @Override
    public Optional<EventProfile> eventProfile(Long eventId) {
        return eventsRepository.findById(eventId).map(e -> {
            var details = eventDetailsRepository.findByEvents_Id(eventId);
            return new EventProfile(
                    toCard(e),
                    Boolean.TRUE.equals(e.getIsApproved()),
                    aboutEventRepository.findByEvents_Id(eventId).map(a -> a.getAbout()).orElse(null),
                    details.map(d -> d.getOrganizedBy()).orElse(null),
                    details.map(d -> d.getLevel()).orElse(null),
                    details.map(d -> d.getLanguage()).orElse(null),
                    details.map(d -> d.getDurationInHours()).orElse(null),
                    details.map(d -> d.getDepartments() == null ? List.<String>of()
                            : d.getDepartments().stream().map(Enum::name).sorted().toList()).orElse(List.of()),
                    eventScheduleRepository.findByEvents_Id(eventId).map(EventApiImpl::scheduleItems).orElse(List.of()),
                    nullToEmpty(faqsRepository.findByEvents_Id(eventId)).stream()
                            .map(f -> new EventProfile.Faq(f.getQuestions(), f.getAnswers())).toList(),
                    nullToEmpty(speakersRepository.findByEvents_Id(eventId)).stream()
                            .map(sp -> new EventProfile.Speaker(sp.getName(), sp.getProfession(), sp.getShortExperience())).toList(),
                    nullToEmpty(whatToBringRepository.findByEvents_Id(eventId)).stream()
                            .map(w -> w.getItem()).filter(Objects::nonNull).toList(),
                    keyHighlightsRepository.findByEvents_Id(eventId).stream()
                            .map(h -> joinNonBlank(h.getName(), h.getSmallDescription())).filter(Objects::nonNull).toList());
        });
    }

    // The schedule is stored as three parallel lists
    private static List<EventProfile.ScheduleItem> scheduleItems(EventSchedule s) {
        List<String> times = nullToEmpty(s.getTime()), titles = nullToEmpty(s.getSchedule()),
                descriptions = nullToEmpty(s.getSmallDescription());
        int n = Math.max(times.size(), Math.max(titles.size(), descriptions.size()));
        List<EventProfile.ScheduleItem> items = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            items.add(new EventProfile.ScheduleItem(at(times, i), at(titles, i), at(descriptions, i)));
        }
        return items;
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static String at(List<String> list, int i) {
        return i < list.size() ? list.get(i) : null;
    }

    private static String joinNonBlank(String a, String b) {
        String joined = java.util.stream.Stream.of(a, b).filter(x -> x != null && !x.isBlank())
                .map(String::trim).collect(java.util.stream.Collectors.joining(": "));
        return joined.isEmpty() ? null : joined;
    }

    private EventCard toCard(Events e) {
        return new EventCard(e.getId(), e.getName(), e.getDescription(), e.getCategory(), e.getEventField(),
                e.getEventDate(), e.getVenue(), e.getMaxPeople(), e.getCostOfRegistration(), e.getImageUrl(),
                registerRepository.countRegistrationsByEventId(e.getId()));
    }

    private EventsDto convertToDto(Events e) {
        return new EventsDto(e.getId(), e.getName(), e.getEventDate(), e.getCategory(), e.getVenue());
    }
}
