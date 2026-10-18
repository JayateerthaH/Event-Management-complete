package com.event.profilemodule.communication;

import com.event.profilemodule.Repository.HelperStudentRepository;
import com.event.profilemodule.entity.admin.Interests;
import com.event.profilemodule.entity.admin.Preferences;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // interests/preferences are lazy collections
public class ProfileApiImpl implements ProfileApi {

    private final HelperStudentRepository studentRepository;
    private final com.event.profilemodule.Repository.InterestsRepository interestsRepository;

    @Override
    public List<String> interestNames(Long userId) {
        return studentRepository.findById(userId)
                .map(p -> p.getInterests().stream().map(Interests::getInterestName).filter(Objects::nonNull).toList())
                .orElse(List.of());
    }

    @Override
    public List<String> preferenceTypes(Long userId) {
        return studentRepository.findById(userId)
                .map(p -> p.getPreferences().stream().map(Preferences::getPreferenceType).filter(Objects::nonNull).toList())
                .orElse(List.of());
    }

    @Override
    public Optional<String> department(Long userId) {
        return studentRepository.findById(userId)
                .map(p -> p.getDepartment())
                .filter(d -> !d.isBlank());
    }

    @Override
    public Optional<String> bio(Long userId) {
        return studentRepository.findById(userId).map(p -> blankToNull(p.getBio()));
    }

    @Override
    public Map<Long, String> displayNames() {
        Map<Long, String> names = new HashMap<>();
        for (Object[] row : studentRepository.findIdAndName()) {
            names.put((Long) row[0], ((String) row[1]).trim());
        }
        return names;
    }

    @Override
    public Map<Long, PublicProfile> publicProfiles(java.util.Collection<Long> userIds) {
        Map<Long, PublicProfile> profiles = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) return profiles;
        for (var p : studentRepository.findAllById(userIds)) {
            List<String> interests = p.getInterests() == null ? List.of()
                    : p.getInterests().stream().map(Interests::getInterestName).filter(Objects::nonNull).sorted().toList();
            profiles.put(p.getId(), new PublicProfile(blankToNull(p.getName()), blankToNull(p.getDepartment()),
                    blankToNull(p.getYearOfStudy()), interests));
        }
        return profiles;
    }

    @Override
    public List<String> allInterestNames() {
        return interestsRepository.findAll().stream().map(Interests::getInterestName)
                .filter(n -> n != null && !n.isBlank()).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
