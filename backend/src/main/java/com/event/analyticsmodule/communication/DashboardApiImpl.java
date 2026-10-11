package com.event.analyticsmodule.communication;

// dashboard/internal/DashboardApiImpl.java

import com.event.analyticsmodule.entity.organizer.BasicInformation;
import com.event.analyticsmodule.entity.student.DashboardBasicInfo;
import com.event.analyticsmodule.repository.DashboardBasicInfoRepository;
import com.event.analyticsmodule.repository.BasicInformationRepository;
import com.event.analyticsmodule.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardApiImpl implements DashboardApi {

    private final DashboardBasicInfoRepository dashboardBasicInfoRepository;
    private final BasicInformationRepository basicInformationRepository;
    private final DashboardService dashboardService;

    @Override
    @Async
    @Transactional
    public void initializeStudentDashboard(Long userId) {
        log.info("Initializing student dashboard for userId: {}", userId);

        dashboardBasicInfoRepository.save(
                DashboardBasicInfo.builder()
                        .userId(userId)
                        .achievements(0L)
                        .eventsAttended(0L)
                        .certificatesEarned(0L)
                        .hoursParticipated(0L)
                        .build()
        );
    }

    @Override
    @Async
    @Transactional
    public void initializeOrganizerDashboard(Long userId) {
        log.info("Initializing organizer dashboard for userId: {}", userId);

        // Preserving your original logic — delete student record on role upgrade
        dashboardBasicInfoRepository.deleteByUserId(userId);

        // Idempotent: a second row would make findByUserId() fail with a non-unique result
        if (basicInformationRepository.findByUserId(userId) != null) {
            log.info("Organizer dashboard already exists for userId: {}", userId);
            return;
        }

        basicInformationRepository.save(
                BasicInformation.builder()
                        .userId(userId)
                        .revenue(0L)
                        .totalEvents(0L)
                        .totalRegistration(0L)
                        .totalAttended(0L)
                        .build()
        );
    }

    @Override
    public long totalPlatformRevenue() {
        return basicInformationRepository.sumRevenue();
    }

    @Override
    public void updateStatsOnEventCompletion(DashboardUpdateRequest request) {
        log.info("Updating stats for organizerId: {}", request.organizerId());
        dashboardService.updateBasicInfo(request);
    }
}