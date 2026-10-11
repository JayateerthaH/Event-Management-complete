package com.event.eventmodule.entity.event;

import java.util.Date;

public interface EventSummaryProjection {
    Long getId();
    String getName();
    String getDescription();
    String getCategory();
    String getEventField(); // FIXED
    Date getEventDate();
    String getVenue();
    Long getMaxPeople();
    String getCostOfRegistration();
    String getImageUrl();
}
