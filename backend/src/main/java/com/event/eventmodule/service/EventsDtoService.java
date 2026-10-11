package com.event.eventmodule.service;


import com.event.eventmodule.entity.home.AttendeeDto;
import org.springframework.security.access.AccessDeniedException;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import com.event.eventmodule.Registration.RegisterRepository;
import com.event.eventmodule.entity.home.OrganizerEventDto;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.home.EventSummaryDto;
import com.event.eventmodule.image.FileService;
import com.event.eventmodule.repository.EventsRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventsDtoService {

    @Autowired
    private FileService fileService;

    private final EventsRepository eventsRepository;
    private final ModelMapper modelMapper;
    private final RegisterRepository registerRepository;
    private final EventSummaryService eventSummaryService;

//    public EventsDto createEvent(EventsDto eventsDto,MultipartFile file,HttpServletRequest request) throws IOException {
////        String url = uploadImage(file,request);
////        eventsDto.setImageUrl(url);
//        return eventsDtoRepository.save(eventsDto);
//    }
    public Events createEvent(Events events, HttpServletRequest request) throws IOException {
//        String url = uploadImage(file,request);
//        eventsDto.setImageUrl(url);
        // The body is bound straight onto the entity, so reset fields only the system/admins may set:
        // a client-supplied id would update an existing event, and isApproved/isFeatured bypass admin review.
        events.setId(null);
        events.setIsApproved(false);
        events.setIsFeatured(false);
        events.setIsCompleted(false);
        return eventsRepository.save(events);
    }


    public String uploadImage(MultipartFile file, HttpServletRequest request)
            throws IOException {


        String fileName = fileService.saveFile(file);

        // Full URL: http://localhost:8080/images/xxxx.png
        String fullUrl = request.getScheme() + "://" + request.getServerName() + ":" +
                request.getServerPort() + "/images/" + fileName;

        Map<String, String> response = new HashMap<>();
        response.put("fileName", fileName); // store in DB
        response.put("url", fullUrl);       // send to frontend

        return fullUrl;
    }

    public List<OrganizerEventDto> getEvents(Long id) {
        return eventsRepository.findByOrganizerId(id)
                .stream()
                .map(event -> OrganizerEventDto.from(event, registerRepository.countRegistrationsByEventId(event.getId())))
                .toList();
    }

    /** Registrations across all events the organizer created (dashboard "Attendees" tab). */
    public List<AttendeeDto> getAttendees(Long organizerId) {
        return registerRepository.findByEventOrganizer(organizerId).stream().map(AttendeeDto::from).toList();
    }

    /** Deletes an event with everything attached to it. Only its creator or an admin may do this. */
    public void deleteEvent(Long eventId, Long userId, boolean isAdmin) {
        Events event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        if (!isAdmin && !userId.equals(event.getOrganizerId())) {
            throw new AccessDeniedException("Only the event's creator can delete it");
        }
        eventSummaryService.deleteEventCompletely(eventId);
    }
}
