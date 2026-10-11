package com.event.sharedmodule;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Getter
@Setter
public class OverallCheckAvailability {

    public boolean isAllowed(String rolesHeader){
        Set<String> roles = Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        return roles.contains("ADMIN");
    }
}
