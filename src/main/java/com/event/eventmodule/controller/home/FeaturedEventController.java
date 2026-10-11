package com.event.eventmodule.controller.home;

//import com.example.Events.entity.home.CategoryDto;
import com.event.eventmodule.entity.home.EventSummaryDto;
import com.event.eventmodule.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FeaturedEventController {

    private final EventsRepository eventsRepository;
    private final ModelMapper modelMapper;

    @GetMapping("/home/featured")
    public List<EventSummaryDto> getCategories(){

        List<EventSummaryDto> dtos = eventsRepository.findFeaturedEventSummaries()
                .stream()
                .map(e -> modelMapper.map(e, EventSummaryDto.class))
                .toList();

        return dtos;
    }
}
