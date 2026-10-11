package com.event.eventmodule.entity.home;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategoryCountDto {
    private String category;
    private Long totalEvents;
}
