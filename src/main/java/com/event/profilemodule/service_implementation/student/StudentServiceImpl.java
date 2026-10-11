package com.event.profilemodule.service_implementation.student;

import com.event.profilemodule.Repository.*;
import com.event.profilemodule.entity.admin.Badges;
import com.event.profilemodule.entity.admin.Interests;
import com.event.profilemodule.entity.admin.Notifications;
import com.event.profilemodule.entity.admin.Preferences;
import com.event.profilemodule.Services.StudentService;
import com.event.profilemodule.entity.student.SocialProfileKey;
import com.event.profilemodule.entity.student.SocialProfileUpdate;
import com.event.profilemodule.entity.student.ProfileStudent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional   // getOrCreateProfile writes, so every entry point needs a transaction
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final HelperStudentRepository studentRepository;
    private final InterestsRepository interestsRepository;
    private final PreferencesRepository preferencesRepository;
    private final BadgesRepository badgesRepository;
    private final NotificationsRepository notificationsRepository;
    private final SocialProfileUpdateRepository repository;
    private final ModelMapper modelMapper;

    /**
     * Get student profile, auto-creating if it doesn't exist yet.
     */
    private ProfileStudent getOrCreateProfile(Long userId) {
        // The profile page fires several requests at once; a plain find-then-save raced on the primary key (409)
        return studentRepository.findById(userId).orElseGet(() -> {
            studentRepository.insertIfAbsent(userId);
            return studentRepository.findById(userId).orElseThrow();
        });
    }

    @Override
    public List<ProfileStudent> getStudentsByPreference(String preferenceType) {
        return studentRepository.findByPreferences_PreferenceType(preferenceType);
    }

    @Override
    public ProfileStudent getStudentByUserId(Long userId) {
        return getOrCreateProfile(userId);
    }

    @Override
    @Transactional
    public ProfileStudent updateStudent(Long userId, ProfileStudent request) {
        // 1. Fetch or Create
        ProfileStudent student = getOrCreateProfile(userId);

        // 2. Map (Safely skips ID and Collections due to AppConfig)
        modelMapper.map(request, student);

        // 3. Save
        return studentRepository.save(student);
    }

    @Override
    public Set<Preferences> getAllPreference(Long userId) {
        assert userId != null;
        ProfileStudent student = getOrCreateProfile(userId);
        return student.getPreferences();
    }

    @Override
    public Set<Preferences> updateVolunteerPreferences(Long userId, Set<Long> preferenceIds) {
        assert userId != null;
        ProfileStudent student = getOrCreateProfile(userId);
        Set<Preferences> preferences = new HashSet<>(preferencesRepository.findAllById(preferenceIds));
        student.setPreferences(preferences);
        studentRepository.save(student);
        return preferences;
    }

    @Override
    public Set<Interests> getAllInterests(Long userId) {
        assert userId != null;
        ProfileStudent student = getOrCreateProfile(userId);
        return student.getInterests();
    }

    @Override
    public Set<Interests> updateStudentInterests(Long userId, Set<Long> interestIds) {
        assert userId != null;
        ProfileStudent student = getOrCreateProfile(userId);
        Set<Interests> interests = new HashSet<>(interestsRepository.findAllById(interestIds));
        student.setInterests(interests);
        studentRepository.save(student);
        return interests;
    }

    @Override
    public Set<Badges> updateBadges(Long userId, Set<Long> badgesIds) {
        ProfileStudent student = getOrCreateProfile(userId);
        Set<Badges> badges = new HashSet<>(badgesRepository.findAllById(badgesIds));
        student.setBadges(badges);
        studentRepository.save(student);
        return badges;
    }

    @Override
    public Set<Badges> getAllBadges(Long userId) {
        assert userId != null;
        ProfileStudent student = getOrCreateProfile(userId);
        return student.getBadges();
    }

    @Transactional
    @Override
    public Set<SocialProfileUpdate> updateSocialProfiles(Long userId, Map<Long, String> profileMap) {
        assert userId != null;

        // Delete all existing social profiles for this user first,
        // so removed entries are actually deleted from the database.
        Set<SocialProfileUpdate> existing = repository.findByIdUserId(userId);
        if (!existing.isEmpty()) {
            repository.deleteAll(existing);
            repository.flush();
        }

        Set<SocialProfileUpdate> set = new HashSet<>();
        for (Map.Entry<Long, String> entry : profileMap.entrySet()) {
            Long platformId = entry.getKey();
            String urlLink = entry.getValue();

            SocialProfileKey key = new SocialProfileKey(platformId, userId);
            SocialProfileUpdate profile = new SocialProfileUpdate();
            profile.setId(key);
            profile.setUrlLink(urlLink);

            repository.save(profile);
            set.add(profile);
        }
        return set;
    }

    @Override
    public Set<SocialProfileUpdate> getAllSocialProfiles(Long userId) {
        assert userId != null;
        return repository.findByIdUserId(userId);
    }

    @Override
    public Set<Notifications> updateNotificationType(Long userId, Set<Long> profileIds) {
        assert userId != null;
        ProfileStudent student = getOrCreateProfile(userId);
        Set<Notifications> notifications = new HashSet<>(notificationsRepository.findAllById(profileIds));
        student.setNotifications(notifications);
        studentRepository.save(student);
        return notifications;
    }

    @Override
    public Set<Notifications> getNotificationType(Long userId) {
        assert userId != null;
        ProfileStudent student = getOrCreateProfile(userId);
        return student.getNotifications();
    }

    public ProfileStudent getStudent(Long userId) {
        return getOrCreateProfile(userId);
    }
}
