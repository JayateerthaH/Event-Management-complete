package com.event.profilemodule.Services;

import com.event.profilemodule.entity.admin.Badges;
import com.event.profilemodule.entity.admin.Interests;
import com.event.profilemodule.entity.admin.Notifications;
import com.event.profilemodule.entity.admin.Preferences;
import com.event.profilemodule.entity.student.SocialProfileUpdate;
import com.event.profilemodule.entity.student.ProfileStudent;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StudentService {
    List<ProfileStudent> getStudentsByPreference(String preferenceType);
    ProfileStudent getStudentByUserId(Long UserId);
    ProfileStudent updateStudent(Long userId, ProfileStudent studentRequestDto);
    Set<Interests> updateStudentInterests(Long userId, Set<Long> interestIds);
    Set<Preferences> updateVolunteerPreferences(Long userId, Set<Long> preferenceIds);
    Set<Badges> updateBadges(Long userId, Set<Long> badgesIds);
    Set<Interests> getAllInterests(Long userId);
    Set<Preferences> getAllPreference(Long userId);
    Set<Badges> getAllBadges(Long userId);

    @Transactional
    Set<SocialProfileUpdate> updateSocialProfiles(Long userId, Map<Long, String> profileMap);

    Set<SocialProfileUpdate> getAllSocialProfiles(Long userId);
    Set<Notifications> updateNotificationType(Long userId, Set<Long> profileIds);
    Set<Notifications> getNotificationType(Long userId);

    ProfileStudent getStudent(Long id);
}
