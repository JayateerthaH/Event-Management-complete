package com.event.eventmodule.controller.home;

//import com.example.Events.entity.home.CategoryDto;
import com.event.eventmodule.entity.home.CategoryCountDto;
import com.event.eventmodule.service.home.EventCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EventCategoryController {

    private final EventCategoryService eventCategoryService;

    @GetMapping("/home/category")
    public ResponseEntity<List<CategoryCountDto>> getCategoryCount() {
        return ResponseEntity.ok(eventCategoryService.getCategoryCounts());
    }

}

