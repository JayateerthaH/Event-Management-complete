package com.event.sharedmodule;

import com.event.eventmodule.entity.announcement.Announcement;
import com.event.profilemodule.entity.student.ProfileStudent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SharedConfig {
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        mapper.getConfiguration()
                .setSkipNullEnabled(true)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setAmbiguityIgnored(true)                              // ✅ fix ambiguous mapping error
                .setMatchingStrategy(MatchingStrategies.STRICT);        // ✅ stricter — avoids false matches

        // Your ProfileStudent protection
        mapper.typeMap(ProfileStudent.class, ProfileStudent.class).addMappings(m -> {
            m.skip(ProfileStudent::setId);
            m.skip(ProfileStudent::setInterests);
            m.skip(ProfileStudent::setPreferences);
            m.skip(ProfileStudent::setBadges);
            m.skip(ProfileStudent::setNotifications);
        });

        // ✅ Fix circular reference — skip Events inside Announcement mapping
        mapper.typeMap(Announcement.class, Announcement.class).addMappings(m -> {
            m.skip(Announcement::setEvents);
        });

        return mapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
