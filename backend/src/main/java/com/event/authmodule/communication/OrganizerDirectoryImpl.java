package com.event.authmodule.communication;

import com.event.authmodule.entitys.Organizer;
import com.event.authmodule.repository.OrganizerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizerDirectoryImpl implements OrganizerDirectory {

    private final OrganizerRepository organizerRepository;

    @Override
    public boolean isOrganizer(Long userId) {
        return userId != null && organizerRepository.existsById(userId);
    }

    @Override
    public Map<Long, String> names(Collection<Long> organizerIds) {
        Map<Long, String> names = new HashMap<>();
        if (organizerIds == null || organizerIds.isEmpty()) return names;
        for (Organizer o : organizerRepository.findAllById(organizerIds)) {
            if (o.getName() != null && !o.getName().isBlank()) names.put(o.getId(), o.getName().trim());
        }
        return names;
    }
}
