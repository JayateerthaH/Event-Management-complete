package com.event.eventmodule.filter;

import com.event.eventmodule.entity.Events;
import org.springframework.data.jpa.domain.Specification;

import java.util.Date;

public class EventsDtoSpecification {

    public static Specification<Events> hasCategory(String category) {
        return (root, query, cb) ->
                (category == null || category.isBlank())
                        ? cb.conjunction()
                        : cb.equal(
                        cb.lower(root.get("category")),
                        category.toLowerCase()
                );
    }

    /** Category is any of the given values (case-insensitive); no values means no restriction. */
    public static Specification<Events> hasAnyCategory(java.util.Collection<String> categories) {
        java.util.List<String> values = categories == null ? java.util.List.of()
                : categories.stream().filter(c -> c != null && !c.isBlank()).map(c -> c.trim().toLowerCase()).distinct().toList();
        return (root, query, cb) -> values.isEmpty() ? cb.conjunction() : cb.lower(root.get("category")).in(values);
    }

    /** Venue contains any of the given values (case-insensitive), like hasVenue for one. */
    public static Specification<Events> hasAnyVenue(java.util.Collection<String> venues) {
        java.util.List<String> values = venues == null ? java.util.List.of()
                : venues.stream().filter(v -> v != null && !v.isBlank()).map(v -> v.trim().toLowerCase()).distinct().toList();
        return (root, query, cb) -> values.isEmpty() ? cb.conjunction()
                : cb.or(values.stream().map(v -> cb.like(cb.lower(root.get("venue")), "%" + v + "%")).toArray(jakarta.persistence.criteria.Predicate[]::new));
    }

    // 🔥 SEARCH IN NAME + DESCRIPTION
    public static Specification<Events> hasSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }

            String pattern = "%" + search.toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    public static Specification<Events> hasVenue(String venue) {
        return (root, query, cb) ->
                (venue == null || venue.isBlank())
                        ? cb.conjunction()
                        : cb.like(
                        cb.lower(root.get("venue")),
                        "%" + venue.toLowerCase() + "%"
                );
    }

    public static Specification<Events> hasEventField(String eventField) {
        return (root, query, cb) ->
                (eventField == null || eventField.isBlank())
                        ? cb.conjunction()
                        : cb.equal(
                        cb.lower(root.get("eventField")),
                        eventField.toLowerCase()
                );
    }

    public static Specification<Events> hasDateRange(Date start, Date end) {
        return (root, query, cb) ->
                (start == null || end == null)
                        ? cb.conjunction()
                        : cb.between(root.get("eventDate"), start, end);
    }

    // COST FILTER (FREE / PAID)
    public static Specification<Events> hasCost(String costType) {
        return (root, query, cb) -> {
            if (costType == null || costType.isBlank()) {
                return cb.conjunction();
            }

            var cost = cb.lower(root.get("costOfRegistration"));
            var freePredicate = cb.or(
                    cb.equal(cost, "free"),
                    cb.equal(cost, "0"),
                    cb.equal(cost, "0.0"),
                    cb.equal(cost, "0.00")
            );

            if ("FREE".equalsIgnoreCase(costType)) {
                return freePredicate;
            }

            if ("PAID".equalsIgnoreCase(costType)) {
                return cb.and(
                        cb.isNotNull(root.get("costOfRegistration")),
                        cb.notEqual(cb.trim(cost), ""),
                        cb.not(freePredicate)
                );
            }

            return cb.conjunction();
        };
    }

    // Only show approved events to public users
    public static Specification<Events> isApproved() {
        return (root, query, cb) -> cb.equal(root.get("isApproved"), true);
    }

}
