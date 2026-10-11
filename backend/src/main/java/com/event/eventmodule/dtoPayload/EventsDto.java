package com.event.eventmodule.dtoPayload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventsDto {
    Long id;
    String name;
    Date eventDate;
    String category;
    String venue;
}