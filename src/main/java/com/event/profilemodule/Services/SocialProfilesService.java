package com.event.profilemodule.Services;

import com.event.profilemodule.entity.admin.SocialProfiles;
import com.event.sharedmodule.exception.ResourceNotFoundException;

import java.util.List;

public interface SocialProfilesService {
    SocialProfiles addSocialProfile(SocialProfiles profile);

    List<SocialProfiles> getSocialProfiles();

    SocialProfiles updateSocialProfile(Long id, SocialProfiles socialProfile) throws ResourceNotFoundException;

    void deleteSocialProfile(Long id) throws ResourceNotFoundException;
}
