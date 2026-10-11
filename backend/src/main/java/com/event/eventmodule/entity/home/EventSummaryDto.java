package com.event.eventmodule.entity.home;

import com.event.eventmodule.entity.Events;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor  // Required for ModelMapper
public class EventSummaryDto {
    private Long id;
    private String name;
    private String description;
    private String category;
    private String field;
    private Date eventDate;
    private String venue;
    private Long maxPeople;
    private String costOfRegistration;
    private String imageUrl;
    private Boolean isApproved;

    /** Public-safe view of an event: never includes its registrations (attendee PII). */
    public static EventSummaryDto from(Events e) {
        return new EventSummaryDto(e.getId(), e.getName(), e.getDescription(), e.getCategory(),
                e.getEventField(), e.getEventDate(), e.getVenue(), e.getMaxPeople(),
                e.getCostOfRegistration(), e.getImageUrl(), e.getIsApproved());
    }
}
