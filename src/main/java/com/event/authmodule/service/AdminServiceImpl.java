package com.event.authmodule.service;

import com.event.eventmodule.api.EventApi;
import com.event.authmodule.dtoPayload.AdminOverviewDto;
import com.event.analyticsmodule.communication.DashboardApi;
import com.event.authmodule.dtoPayload.AdminResponseDto;
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
import com.event.authmodule.repository.UserInfoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final OrganizerRepository organizerRepository;
    private final AdminRepository adminRepository;
    private final UserInfoRepository userInfoRepository;
    private final ModelMapper modelMapper;
    private final OnBoardDtoRepository onBoardDtoRepository;
    private final DashboardApi dashboardApi;
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

        // ✅ Replace userClient.createOrganizer() with direct API call
        dashboardApi.initializeOrganizerDashboard(user.getUserId());

        onBoardDtoRepository.deleteByUsernameAndRoleType(user.getUsername(), RoleType.ORGANIZER);

        OrganizerResponseDto response = modelMapper.map(organizerRepository.save(organizer), OrganizerResponseDto.class);
        response.setEmail(user.getUsername());   // email lives on UserInfo, which ModelMapper doesn't flatten
        return response;
    }

    @Override
    public AdminOverviewDto getOverview() {
        return new AdminOverviewDto(
                userInfoRepository.count(),
                eventApi.countAllEvents(),
                organizerRepository.count(),
                dashboardApi.totalPlatformRevenue());
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
        userInfoRepository.save(user);

        // No dashboard call needed for Admin — same as your original code
        onBoardDtoRepository.deleteByUsernameAndRoleType(user.getUsername(), RoleType.ADMIN);

        AdminResponseDto response = modelMapper.map(adminRepository.save(admin), AdminResponseDto.class);
        response.setEmail(user.getUsername());
        return response;
    }
}
