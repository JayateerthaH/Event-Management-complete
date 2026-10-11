package com.event.analyticsmodule.dtoPayload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EventsDto {

    private Date eventDate;
    private String name;
    private String venue;
    private String category;

    private Long userId;

    private Long id; // External or public event identifier (NOT primary key)
}
