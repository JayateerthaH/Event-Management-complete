package com.event.eventmodule.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventFilterRequest {

    private String category;
    private String search;
    private String venue;
    private String eventField; // Added for Department filter
    private String dateRange;   // TODAY / THIS_WEEK / THIS_MONTH
    private String costType;    // FREE / PAID
    private Integer page = 0;
    private Integer size = 6;
}
