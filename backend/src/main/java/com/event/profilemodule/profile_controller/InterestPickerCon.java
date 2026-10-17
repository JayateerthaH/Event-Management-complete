package com.event.profilemodule.profile_controller;

import com.event.eventmodule.api.EventApi;
import com.event.profilemodule.Repository.InterestsRepository;
import com.event.profilemodule.Services.StudentService;
import com.event.profilemodule.entity.admin.Interests;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * The interest picker shown after signup: the admin's interests plus the categories of approved events.
 * Picking a category that isn't an interest yet creates it, so students can choose from real event topics
 * even before an admin has set up the interest list. Names are checked against those two lists only.
 */
@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
public class InterestPickerCon {

    static final int MAX_PICKS = 15;

    private final InterestsRepository interestsRepository;
    private final StudentService studentService;
    private final EventApi eventApi;

    public record Picks(List<String> names) {}

    @GetMapping("/interest-options")
    @Transactional(readOnly = true)
    public List<String> options() {
        TreeMap<String, String> byKey = new TreeMap<>();
        interestsRepository.findAll().forEach(i -> add(byKey, i.getInterestName()));
        eventApi.eventCategories().forEach(c -> add(byKey, c));
        return new ArrayList<>(byKey.values());
    }

    @PutMapping("/interests/by-name")
    @Transactional
    public List<String> pick(@RequestAttribute("userId") String userId, @RequestBody Picks picks) {
        List<String> names = picks == null || picks.names() == null ? List.of() : picks.names().stream()
                .filter(Objects::nonNull).map(String::trim).filter(n -> !n.isEmpty()).distinct().toList();
        if (names.size() > MAX_PICKS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pick at most " + MAX_PICKS + " interests");
        }
        Map<String, String> categories = new HashMap<>();
        eventApi.eventCategories().forEach(c -> { if (c != null) categories.put(c.trim().toLowerCase(Locale.ROOT), c.trim()); });

        Set<Long> ids = new LinkedHashSet<>();
        for (String name : names) {
            Interests interest = interestsRepository.findFirstByInterestNameIgnoreCase(name).orElseGet(() -> {
                String category = categories.get(name.toLowerCase(Locale.ROOT));
                if (category == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "\"" + name + "\" isn't one of the interests you can pick");
                }
                Interests created = new Interests();
                created.setInterestName(category);
                return interestsRepository.save(created);
            });
            ids.add(interest.getInterestId());
        }
        return studentService.updateStudentInterests(Long.valueOf(userId), ids).stream()
                .map(Interests::getInterestName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private static void add(Map<String, String> byKey, String name) {
        if (name != null && !name.isBlank()) byKey.putIfAbsent(name.trim().toLowerCase(Locale.ROOT), name.trim());
    }
}
