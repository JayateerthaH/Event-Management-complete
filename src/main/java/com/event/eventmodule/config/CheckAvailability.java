package com.event.eventmodule.config;

import com.event.eventmodule.repository.CollaboratorsRepository;
import com.event.eventmodule.repository.EventsRepository;
import lombok.*;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Getter
@Setter
public class CheckAvailability implements EventAccessService {
    private final EventsRepository eventsRepository;
    private final CollaboratorsRepository collaboratorsRepository;

    @Override
    public boolean isAllowed(Long eventId, String userId, String rolesHeader, String userEmail) {

        if (userId == null || rolesHeader == null || rolesHeader.isEmpty()) {
            return false;
        }

        Set<String> roles = Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        if (roles.contains("ADMIN") || roles.contains("ROLE_ADMIN")) {
            return true;
        }

        // Check if user is the event creator
        if (eventsRepository.findByIdAndOrganizerId(eventId, Long.valueOf(userId)).isPresent()) {
            return true;
        }

        // Check if user is a collaborator for this event (by email)
        if (userEmail != null && !userEmail.isEmpty()) {
            return collaboratorsRepository.existsByEventIdAndEmail(eventId, userEmail);
        }

        return false;
    }
}