package com.event.authmodule;

import com.event.analyticsmodule.communication.DashboardApi;
import com.event.authmodule.dtoPayload.AdminOverviewDto;
import com.event.authmodule.dtoPayload.AdminUserDto;
import com.event.authmodule.entitys.UserInfo;
import com.event.authmodule.entitys.type.RoleType;
import com.event.authmodule.entitys.OnBoard;
import com.event.authmodule.repository.OnBoardDtoRepository;
import com.event.authmodule.repository.OrganizerRepository;
import com.event.authmodule.repository.StudentRepository;
import com.event.authmodule.repository.UserInfoRepository;
import com.event.authmodule.service.AdminServiceImpl;
import com.event.eventmodule.api.EventApi;
import com.event.profilemodule.communication.ProfileApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceUsersTest {

    @Mock UserInfoRepository users;
    @Mock ProfileApi profileApi;
    @Mock EventApi eventApi;
    @Mock OrganizerRepository organizers;
    @Mock DashboardApi dashboardApi;
    @Mock StudentRepository students;
    @Mock OnBoardDtoRepository requests;
    @InjectMocks AdminServiceImpl service;

    private static UserInfo user(long id, String email, LocalDateTime created, RoleType... roles) {
        return UserInfo.builder().userId(id).username(email).createdAt(created)
                .roles(roles.length == 0 ? EnumSet.noneOf(RoleType.class) : EnumSet.of(roles[0], roles)).build();
    }

    private final List<UserInfo> all = List.of(
            user(1, "ada@example.com", null, RoleType.ADMIN),
            user(2, "olga@example.com", LocalDateTime.now(), RoleType.ORGANIZER, RoleType.STUDENT),
            user(3, "sam@example.com", LocalDateTime.now(), RoleType.STUDENT));

    @Test
    void searchReturnsEachUserOnceWithAllRolesAndProfileName() {
        when(users.findAll()).thenReturn(all);
        when(profileApi.displayNames()).thenReturn(Map.of(2L, "Olga Organizer"));

        List<AdminUserDto> byEmail = service.searchUsers("OLGA@");
        assertThat(byEmail).containsExactly(new AdminUserDto(2L, "olga@example.com", "Olga Organizer", List.of("ORGANIZER", "STUDENT")));

        assertThat(service.searchUsers("organizer")).extracting(AdminUserDto::id).containsExactly(2L);   // by name
        assertThat(service.searchUsers("ada")).singleElement().extracting(AdminUserDto::roles).isEqualTo(List.of("ADMIN"));
        assertThat(service.searchUsers("  ")).isEmpty();
    }

    @Test
    void overviewHasRoleCountsAndSixMonths() {
        when(users.findAll()).thenReturn(all);
        when(eventApi.eventsPerMonth(any(), any())).thenReturn(Map.of(YearMonth.now(), 4L));

        AdminOverviewDto overview = service.getOverview();

        assertThat(overview.totalUsers()).isEqualTo(3);
        assertThat(overview.roleDistribution()).containsExactly(
                new AdminOverviewDto.RoleCount("Students", 2),
                new AdminOverviewDto.RoleCount("Organizers", 1),
                new AdminOverviewDto.RoleCount("Admins", 1));
        assertThat(overview.systemAnalytics()).hasSize(6);
        AdminOverviewDto.MonthStat current = overview.systemAnalytics().get(5);
        assertThat(current.users()).isEqualTo(2);   // the account without a creation date isn't counted
        assertThat(current.events()).isEqualTo(4);
    }

    @Test
    void requestsForRolesTheAccountAlreadyHasAreRemoved() {
        OnBoard stale = new OnBoard("ada@example.com", RoleType.ADMIN);        // already an admin
        OnBoard waiting = new OnBoard("sam@example.com", RoleType.ORGANIZER);  // still a student
        OnBoard orphan = new OnBoard("gone@example.com", RoleType.ORGANIZER);  // account deleted
        when(requests.findAll()).thenReturn(List.of(stale, waiting, orphan));
        when(users.findByUsername("ada@example.com")).thenReturn(Optional.of(all.get(0)));
        when(users.findByUsername("sam@example.com")).thenReturn(Optional.of(all.get(2)));
        when(users.findByUsername("gone@example.com")).thenReturn(Optional.empty());

        assertThat(service.pendingRequests()).containsExactly(waiting);
        verify(requests).delete(stale);
        verify(requests).delete(orphan);
        verify(requests, never()).delete(waiting);
    }

    @Test
    void organizersAndAdminsLoseTheirStudentRecord() {
        when(users.findAll()).thenReturn(all);
        when(students.existsById(1L)).thenReturn(true);   // admin with a leftover student record

        assertThat(service.removeStaleStudentRecords()).isEqualTo(1);
        verify(students).deleteById(1L);
        verify(students, never()).deleteById(3L);         // still a student
    }
}
