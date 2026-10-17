package com.event.eventmodule.filter;

import com.event.eventmodule.entity.home.EventSummaryDto;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventsRepository eventsRepository;

    public Page<EventSummaryDto> filterEvents(String category, String search, String venue, String eventField,
                                              String dateRange, String costType, Pageable pageable) {
        return filterEvents(single(category), search, single(venue), eventField, dateRange, costType, pageable);
    }

    private static java.util.List<String> single(String value) {
        return value == null || value.isBlank() ? java.util.List.of() : java.util.List.of(value);
    }

    public Page<EventSummaryDto> filterEvents(
            java.util.List<String> categories,
            String search,
            java.util.List<String> venues,
            String eventField,
            String dateRange,
            String costType,   // 🔥 NEW
            Pageable pageable
    ) {
        // Normalize dateRange to handle "this-week" -> "THIS_WEEK"
        if (dateRange != null) {
            dateRange = dateRange.replace("-", "_");
        }

        Date now = new Date();
        Calendar cal = Calendar.getInstance();

        Date start = null;
        Date end = null;

        if ("TODAY".equalsIgnoreCase(dateRange)) {
            cal.setTime(now);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            start = cal.getTime();

            cal.setTime(now);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            end = cal.getTime();
        }

        else if ("TOMORROW".equalsIgnoreCase(dateRange)) {
            cal.setTime(now);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            start = cal.getTime();

            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            end = cal.getTime();
        }

        else if ("THIS_WEEK".equalsIgnoreCase(dateRange)) {
            start = now;

            // Upcoming 7-day window from now (avoids locale/week-start issues)
            cal.setTime(now);
            cal.add(Calendar.DAY_OF_MONTH, 7);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            end = cal.getTime();
        }

        else if ("THIS_MONTH".equalsIgnoreCase(dateRange)) {
            start = now;

            cal.setTime(now);
            cal.set(Calendar.DAY_OF_MONTH,
                    cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            end = cal.getTime();
        }

        Specification<Events> spec = Specification
                .where(EventsDtoSpecification.hasAnyCategory(categories))
                .and(EventsDtoSpecification.hasSearch(search))
                .and(EventsDtoSpecification.hasAnyVenue(venues))
                .and(EventsDtoSpecification.hasEventField(eventField))
                .and(EventsDtoSpecification.hasCost(costType)) // 🔥
                .and(EventsDtoSpecification.hasDateRange(start, end))
                .and(EventsDtoSpecification.isApproved());

        // Summaries only: the Events entity serializes its registrations (attendee names, emails, phones)
        return eventsRepository.findAll(spec, pageable).map(EventSummaryDto::from);
    }
}
