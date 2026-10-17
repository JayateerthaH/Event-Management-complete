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
    public Map<Long, String> displayNames() {
        Map<Long, String> names = new HashMap<>();
        for (Object[] row : studentRepository.findIdAndName()) {
            names.put((Long) row[0], ((String) row[1]).trim());
        }
        return names;
    }
}
