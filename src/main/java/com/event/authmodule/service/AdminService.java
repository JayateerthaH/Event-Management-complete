package com.event.authmodule.service;


import com.event.authmodule.dtoPayload.AdminOverviewDto;
import com.event.authmodule.dtoPayload.AdminResponseDto;
import com.event.authmodule.dtoPayload.OnBoardAdminRequestDto;
import com.event.authmodule.dtoPayload.OnBoardOrganizerRequestDto;
import com.event.authmodule.dtoPayload.OrganizerResponseDto;

public interface AdminService {

    OrganizerResponseDto onBoardOrganizer(OnBoardOrganizerRequestDto onBoardOrganizerRequestDto);

    AdminResponseDto onBoardAdmin(OnBoardAdminRequestDto onBoardAdminRequestDto);

    AdminOverviewDto getOverview();

}
