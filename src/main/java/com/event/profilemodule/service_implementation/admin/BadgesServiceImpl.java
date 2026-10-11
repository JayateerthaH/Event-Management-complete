package com.event.profilemodule.service_implementation.admin;

import com.event.profilemodule.Repository.BadgesRepository;
import com.event.profilemodule.Services.BadgesService;
import com.event.profilemodule.entity.admin.Badges;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class BadgesServiceImpl implements BadgesService {

    @Autowired
    private BadgesRepository badgesRepository;

    @Override
    public Badges createBadge(Badges badge) {
        log.info("Creating new badge: {}", badge.getBadgeName());
        return badgesRepository.save(badge);
    }

    @Override
    public Badges getBadge(Long id) throws ResourceNotFoundException {
        return badgesRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Badge not found with id {}", id);
                    return new ResourceNotFoundException("Badge not found with id " + id);
                });
    }

    @Override
    public List<Badges> getAllBadges() {
        return badgesRepository.findAll();
    }

    @Transactional
    @Override
    public Badges updateBadge(Long id, Badges newBadge) throws ResourceNotFoundException {
        log.info("Updating badge with id {}", id);
        newBadge.setId(id);
        return badgesRepository.save(newBadge);
    }

    @Transactional
    @Override
    public void deleteBadge(Long id) throws ResourceNotFoundException {
        if (!badgesRepository.existsById(id)) {
            log.error("Attempted to delete non-existing badge with id {}", id);
            throw new ResourceNotFoundException("Badge not found with id " + id);
        }

        log.warn("Deleting badge with id {}", id);
        badgesRepository.deleteById(id);
    }
}
