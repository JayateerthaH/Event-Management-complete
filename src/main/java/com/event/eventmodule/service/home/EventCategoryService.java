package com.event.eventmodule.service.home;

import com.event.eventmodule.entity.home.CategoryCountDto;
import com.event.eventmodule.repository.EventCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventCategoryService {

    private final EventCategoryRepository eventCategoryRepository;

    public List<CategoryCountDto> getCategoryCounts() {
        return eventCategoryRepository.getCategoryCounts()
                .stream()
                .map(p -> new CategoryCountDto(
                        p.getCategory(),
                        p.getTotalEvents()
                ))
                .toList();
    }

}
