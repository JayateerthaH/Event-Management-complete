package com.event.profilemodule.Services;

import com.event.profilemodule.entity.admin.Badges;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;

import java.util.List;

public interface BadgesService {

    @Transactional
    void deleteBadge(Long id) throws ResourceNotFoundException;
    Badges createBadge(Badges badge);
    Badges getBadge(Long id) throws ResourceNotFoundException;
    List<Badges> getAllBadges();
    Badges updateBadge(Long id, Badges badge) throws ResourceNotFoundException;

}