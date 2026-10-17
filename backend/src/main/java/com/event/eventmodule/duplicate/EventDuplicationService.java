package com.event.eventmodule.duplicate;

import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.event.*;
import com.event.eventmodule.entity.event.eventdetail.EventDetails;
import com.event.eventmodule.entity.event.highlight.KeyHighlights;
import com.event.eventmodule.repository.EventsRepository;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * "Duplicate event": a new event with the same details (about, details, highlights, schedule, speakers,
 * FAQs, what to bring) on a new date. It goes through admin approval again. Registrations, attendance,
 * announcements and images are not copied; the organizer adds a cover for the new event.
 */
@Service
@RequiredArgsConstructor
public class EventDuplicationService {

    private final EventsRepository eventsRepository;

    @Transactional
    public Events duplicate(Long sourceId, Long newOwnerId, Date newDate, String newName) {
        Events src = eventsRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + sourceId));
        if (newDate == null) throw bad("Choose a date for the new event");
        if (!newDate.after(new Date())) throw bad("The new event's date must be in the future");
        String name = newName == null || newName.isBlank() ? src.getName() : newName.trim();
        if (name == null || name.isBlank()) throw bad("Enter a name for the new event");
        if (name.length() > 255) throw bad("The name can be at most 255 characters");

        Events e = new Events();
        e.setName(name);
        e.setDescription(src.getDescription());
        e.setCategory(src.getCategory());
        e.setEventField(src.getEventField());
        e.setEventDate(newDate);
        e.setVenue(src.getVenue());
        e.setMaxPeople(src.getMaxPeople());
        e.setCostOfRegistration(src.getCostOfRegistration());
        e.setOrganizerId(newOwnerId);
        e.setIsApproved(false);
        e.setIsFeatured(false);
        e.setIsCompleted(false);

        if (src.getAboutEvent() != null) {
            AboutEvent a = new AboutEvent();
            a.setAbout(src.getAboutEvent().getAbout());
            a.setEvents(e);
            e.setAboutEvent(a);
        }
        if (src.getEventDetails() != null) {
            EventDetails s = src.getEventDetails(), d = new EventDetails();
            d.setOrganizedBy(s.getOrganizedBy());
            d.setDepartments(s.getDepartments() == null ? null : new HashSet<>(s.getDepartments()));
            d.setDurationInHours(s.getDurationInHours());
            d.setLanguage(s.getLanguage());
            d.setLevel(s.getLevel());
            d.setContactEmail(s.getContactEmail());
            d.setContactPhone(s.getContactPhone());
            d.setEvents(e);
            e.setEventDetails(d);
        }
        if (src.getKeyHighlights() != null) {
            KeyHighlights s = src.getKeyHighlights(), k = new KeyHighlights();
            k.setHighlights(s.getHighlights());
            k.setName(s.getName());
            k.setSmallDescription(s.getSmallDescription());
            k.setEvents(e);
            e.setKeyHighlights(k);
        }
        if (src.getEventSchedule() != null) {
            EventSchedule s = src.getEventSchedule(), c = new EventSchedule();
            c.setTime(copy(s.getTime()));
            c.setSchedule(copy(s.getSchedule()));
            c.setSmallDescription(copy(s.getSmallDescription()));
            c.setEvents(e);
            e.setEventSchedule(c);
        }
        List<EventSpeakers> speakers = new ArrayList<>();
        for (EventSpeakers s : nonNull(src.getSpeakers())) {
            EventSpeakers c = new EventSpeakers();
            c.setName(s.getName());
            c.setProfession(s.getProfession());
            c.setShortExperience(s.getShortExperience());
            c.setProfile(copy(s.getProfile()));
            c.setEvents(e);
            speakers.add(c);
        }
        e.setSpeakers(speakers);
        List<FAQs> faqs = new ArrayList<>();
        for (FAQs s : nonNull(src.getFaQs())) {
            FAQs c = new FAQs();
            c.setQuestions(s.getQuestions());
            c.setAnswers(s.getAnswers());
            c.setEvents(e);
            faqs.add(c);
        }
        e.setFaQs(faqs);
        List<WhatToBring> items = new ArrayList<>();
        for (WhatToBring s : nonNull(src.getWhatToBring())) {
            WhatToBring c = new WhatToBring();
            c.setItem(s.getItem());
            c.setEvents(e);
            items.add(c);
        }
        e.setWhatToBring(items);
        return eventsRepository.save(e);
    }

    private static <T> List<T> copy(List<T> list) {
        return list == null ? null : new ArrayList<>(list);
    }

    private static <T> List<T> nonNull(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
