package com.event.eventmodule.controller.admin;

import com.event.eventmodule.entity.Events;
import com.event.eventmodule.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
public class FeaturedController {

    private final EventsRepository eventsRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/feature/{eventId}")
    public String makeEventAsFeatured(@PathVariable Long eventId){
        Events eventsDto = eventsRepository.findById(eventId).orElseThrow();
        eventsDto.setIsFeatured(true);
        eventsDto.setId(eventId);
        eventsRepository.save(eventsDto);
        return "Successfully Added as Featured Event";
    }

}
