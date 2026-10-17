package com.event.authmodule.service;

import com.event.eventmodule.api.EventApi;
import com.event.authmodule.dtoPayload.AdminOverviewDto;
import com.event.analyticsmodule.communication.DashboardApi;
import com.event.authmodule.dtoPayload.AdminResponseDto;
import com.event.authmodule.dtoPayload.AdminUserDto;
import com.event.profilemodule.communication.ProfileApi;
import com.event.authmodule.dtoPayload.OnBoardAdminRequestDto;
import com.event.authmodule.dtoPayload.OnBoardOrganizerRequestDto;
import com.event.authmodule.dtoPayload.OrganizerResponseDto;
import com.event.authmodule.entitys.Admin;
import com.event.authmodule.entitys.Organizer;
import com.event.authmodule.entitys.UserInfo;
import com.event.authmodule.entitys.type.RoleType;
import com.event.authmodule.repository.AdminRepository;
import com.event.authmodule.repository.OnBoardDtoRepository;
import com.event.authmodule.repository.OrganizerRepository;
import com.event.authmodule.repository.StudentRepository;
import com.event.authmodule.entitys.OnBoard;
import com.event.authmodule.repository.UserInfoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final OrganizerRepository organizerRepository;
    private final AdminRepository adminRepository;
    private final UserInfoRepository userInfoRepository;
    private final StudentRepository studentRepository;
    private final ModelMapper modelMapper;
    private final OnBoardDtoRepository onBoardDtoRepository;
    private final DashboardApi dashboardApi;
    private final ProfileApi profileApi;
    private final EventApi eventApi;  // ← add this, remove UserClient

    @Override
    @Transactional
    public OrganizerResponseDto onBoardOrganizer(OnBoardOrganizerRequestDto dto) {

        UserInfo user = userInfoRepository.findByUsername(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (organizerRepository.existsByUserInfoUsername(dto.getEmail())) {
            throw new IllegalArgumentException("Already an Organizer");
        }

        Organizer organizer = Organizer.builder()
                .userInfo(user)
                .build();

        user.getRoles().add(RoleType.ORGANIZER);
        user.getRoles().remove(RoleType.STUDENT);
        userInfoRepository.save(user);
        studentRepository.deleteById(user.getUserId());   // organizer only: no student record either

        // ✅ Replace userClient.createOrganizer() with direct API call
        dashboardApi.initializeOrganizerDashboard(user.getUserId());

        onBoardDtoRepository.deleteByUsernameAndRoleType(user.getUsername(), RoleType.ORGANIZER);

        OrganizerResponseDto response = modelMapper.map(organizerRepository.save(organizer), OrganizerResponseDto.class);
        response.setEmail(user.getUsername());   // email lives on UserInfo, which ModelMapper doesn't flatten
        return response;
    }

    @Override
    public AdminOverviewDto getOverview() {
        List<UserInfo> users = userInfoRepository.findAll();

        List<AdminOverviewDto.RoleCount> roles = List.of(
                new AdminOverviewDto.RoleCount("Students", countWithRole(users, RoleType.STUDENT)),
                new AdminOverviewDto.RoleCount("Organizers", countWithRole(users, RoleType.ORGANIZER)),
                new AdminOverviewDto.RoleCount("Admins", countWithRole(users, RoleType.ADMIN)));

        // Last 6 months including the current one
        YearMonth now = YearMonth.now();
        YearMonth from = now.minusMonths(5);
        Map<YearMonth, Long> events = eventApi.eventsPerMonth(from, now);
        DateTimeFormatter label = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
        List<AdminOverviewDto.MonthStat> months = new ArrayList<>();
        for (YearMonth m = from; !m.isAfter(now); m = m.plusMonths(1)) {
            YearMonth month = m;
            long created = users.stream()
                    .filter(u -> u.getCreatedAt() != null && YearMonth.from(u.getCreatedAt()).equals(month))
                    .count();
            months.add(new AdminOverviewDto.MonthStat(m.format(label), created, events.getOrDefault(m, 0L)));
        }

        return new AdminOverviewDto(
                users.size(),
                eventApi.countAllEvents(),
                organizerRepository.count(),
                dashboardApi.totalPlatformRevenue(),
                roles,
                months);
    }

    private static long countWithRole(List<UserInfo> users, RoleType role) {
        return users.stream().filter(u -> u.getRoles() != null && u.getRoles().contains(role)).count();
    }

    @Override
    public List<AdminUserDto> searchUsers(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return List.of();
        Map<Long, String> names = profileApi.displayNames();
        return userInfoRepository.findAll().stream()
                .filter(u -> contains(u.getUsername(), q) || contains(names.get(u.getUserId()), q))
                .sorted(Comparator.comparing(UserInfo::getUsername, String.CASE_INSENSITIVE_ORDER))
                .limit(50)
                .map(u -> new AdminUserDto(
                        u.getUserId(),
                        u.getUsername(),
                        names.get(u.getUserId()),
                        u.getRoles() == null ? List.of()
                                : u.getRoles().stream().sorted().map(Enum::name).toList()))   // ADMIN, ORGANIZER, STUDENT
                .toList();
    }

    @Transactional
    @Override
    public List<OnBoard> pendingRequests() {
        List<OnBoard> pending = new ArrayList<>();
        for (OnBoard request : onBoardDtoRepository.findAll()) {
            UserInfo user = userInfoRepository.findByUsername(request.getUsername()).orElse(null);
            // Stale: the account is gone, or already has the role (e.g. promoted directly)
            if (user == null || (user.getRoles() != null && user.getRoles().contains(request.getRoleType()))) {
                onBoardDtoRepository.delete(request);
            } else {
                pending.add(request);
            }
        }
        return pending;
    }

    @Transactional
    @Override
    public int removeStaleStudentRecords() {
        int removed = 0;
        for (UserInfo user : userInfoRepository.findAll()) {
            boolean student = user.getRoles() != null && user.getRoles().contains(RoleType.STUDENT);
            if (!student && studentRepository.existsById(user.getUserId())) {
                studentRepository.deleteById(user.getUserId());
                removed++;
            }
        }
        return removed;
    }

    private static boolean contains(String value, String lowerCaseQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerCaseQuery);
    }

    @Transactional
    @Override
    public AdminResponseDto onBoardAdmin(OnBoardAdminRequestDto dto) {

        UserInfo user = userInfoRepository.findByUsername(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (adminRepository.existsByUserInfoUsername(dto.getEmail())) {
            throw new IllegalArgumentException("Already an Admin");
        }

        Admin admin = Admin.builder()
                .userInfo(user)
                .build();

        user.getRoles().add(RoleType.ADMIN);
        user.getRoles().remove(RoleType.STUDENT);
        studentRepository.deleteById(user.getUserId());
        userInfoRepository.save(user);

        // No dashboard call needed for Admin — same as your original code
        onBoardDtoRepository.deleteByUsernameAndRoleType(user.getUsername(), RoleType.ADMIN);

        AdminResponseDto response = modelMapper.map(adminRepository.save(admin), AdminResponseDto.class);
        response.setEmail(user.getUsername());
        return response;
    }
}
