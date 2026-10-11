package com.event.profilemodule.service_implementation.admin;

import com.event.profilemodule.Repository.HelperStudentRepository;
import com.event.profilemodule.Repository.SocialProfilesRepository;
import com.event.profilemodule.Services.SocialProfilesService;
import com.event.profilemodule.entity.admin.SocialProfiles;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialProfileServiceImpl implements SocialProfilesService {

    private final SocialProfilesRepository socialProfileRepository;
    private final HelperStudentRepository studentRepository;

    @Override
    public SocialProfiles addSocialProfile(SocialProfiles profile) {
        return socialProfileRepository.save(profile);
    }

    @Override
    public List<SocialProfiles> getSocialProfiles() {
        return socialProfileRepository.findAll();
    }

    @Override
    public SocialProfiles updateSocialProfile(Long id, SocialProfiles updated) throws ResourceNotFoundException {

        SocialProfiles profile = socialProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        profile.setPlatform(updated.getPlatform());

        return socialProfileRepository.save(profile);
    }

    @Override
    public void deleteSocialProfile(Long id) throws ResourceNotFoundException {

        SocialProfiles profile = socialProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        socialProfileRepository.delete(profile);
    }


}
