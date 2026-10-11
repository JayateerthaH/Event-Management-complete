package com.event.security;

import com.event.authmodule.entitys.UserInfo;
import com.event.authmodule.entitys.type.RoleType;
import com.event.eventmodule.Registration.RegisterRepository;
import com.event.eventmodule.Registration.RegistrationController;
import com.event.eventmodule.Registration.RegistrationService;
import com.event.eventmodule.Registration.UserRegistrationRepository;
import com.event.eventmodule.controller.AnalyticsController;
import com.event.eventmodule.service.AnalyticsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Verifies @SelfOrAdmin on endpoints that take a userId from the URL: users may only read their own data.
 * Runs the real controllers behind real method security, with mocked services (no database needed).
 */
@SpringJUnitConfig(OwnershipAuthorizationTest.Config.class)
class OwnershipAuthorizationTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        RegistrationController registrationController() {
            return new RegistrationController(mock(RegistrationService.class),
                    mock(UserRegistrationRepository.class), mock(RegisterRepository.class));
        }

        @Bean
        AnalyticsController analyticsController() {
            return new AnalyticsController(mock(AnalyticsService.class));
        }
    }

    @Autowired RegistrationController registrationController;
    @Autowired AnalyticsController analyticsController;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userCanReadOwnData() {
        loginAs(7L, RoleType.STUDENT);

        assertThatCode(() -> registrationController.getUpcomingEvents(7L)).doesNotThrowAnyException();
        assertThatCode(() -> registrationController.getPastEvents(7L)).doesNotThrowAnyException();
        assertThatCode(() -> analyticsController.getUserAttendance(7L)).doesNotThrowAnyException();
    }

    @Test
    void userCannotReadAnotherUsersData() {
        loginAs(7L, RoleType.STUDENT);

        assertThatThrownBy(() -> registrationController.getUpcomingEvents(8L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> registrationController.getPastEvents(8L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> analyticsController.getUserAttendance(8L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> analyticsController.getOrganizerRegistrations(8L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void organizerCannotReadAnotherOrganizersStats() {
        loginAs(7L, RoleType.ORGANIZER);

        assertThatCode(() -> analyticsController.getOrganizerRegistrations(7L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> analyticsController.getOrganizerRegistrations(8L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminCanReadAnyUsersData() {
        loginAs(1L, RoleType.ADMIN);

        assertThatCode(() -> registrationController.getUpcomingEvents(8L)).doesNotThrowAnyException();
        assertThatCode(() -> analyticsController.getUserAttendance(8L)).doesNotThrowAnyException();
        assertThatCode(() -> analyticsController.getOrganizerRegistrations(8L)).doesNotThrowAnyException();
    }

    // Mirrors JwtAuthFilter: the principal is the UserInfo entity
    private void loginAs(Long userId, RoleType role) {
        UserInfo user = UserInfo.builder()
                .userId(userId)
                .username("user" + userId + "@example.com")
                .roles(new HashSet<>(Set.of(role)))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
