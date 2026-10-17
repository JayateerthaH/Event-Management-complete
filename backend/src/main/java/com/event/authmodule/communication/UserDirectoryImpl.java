package com.event.authmodule.communication;

import com.event.authmodule.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDirectoryImpl implements UserDirectory {

    private final UserInfoRepository userInfoRepository;

    @Override
    public List<Long> studentIds() {
        return userInfoRepository.findStudentIds().stream().distinct().sorted().toList();
    }

    @Override
    public Map<Long, String> emails(Collection<Long> userIds) {
        Map<Long, String> emails = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) return emails;
        for (Object[] row : userInfoRepository.findUsernames(userIds)) {
            String email = (String) row[1];
            if (email != null && email.contains("@")) emails.put((Long) row[0], email);
        }
        return emails;
    }
}
