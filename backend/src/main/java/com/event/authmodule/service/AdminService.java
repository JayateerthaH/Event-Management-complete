package com.event.authmodule.service;


import com.event.authmodule.dtoPayload.AdminOverviewDto;
import com.event.authmodule.dtoPayload.AdminResponseDto;
import com.event.authmodule.dtoPayload.AdminUserDto;
import com.event.authmodule.dtoPayload.OnBoardAdminRequestDto;
import com.event.authmodule.dtoPayload.OnBoardOrganizerRequestDto;
import com.event.authmodule.dtoPayload.OrganizerResponseDto;

public interface AdminService {

    OrganizerResponseDto onBoardOrganizer(OnBoardOrganizerRequestDto onBoardOrganizerRequestDto);

    AdminResponseDto onBoardAdmin(OnBoardAdminRequestDto onBoardAdminRequestDto);

    AdminOverviewDto getOverview();

    /** Accounts whose email or profile name contains {@code query}, at most 50. */
    java.util.List<AdminUserDto> searchUsers(String query);

}
