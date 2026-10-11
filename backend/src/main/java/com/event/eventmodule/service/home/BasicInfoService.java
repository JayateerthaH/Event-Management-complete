package com.event.eventmodule.service.home;


import com.event.eventmodule.Registration.RegisterRepository;
import com.event.eventmodule.entity.home.BasicInfo;
import com.event.eventmodule.repository.BasicInfoRepository;
import com.event.eventmodule.repository.EventsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BasicInfoService {

    private static final long HOME_ID = 1L;

    private final BasicInfoRepository basicInfoRepository;
    private final EventsRepository eventsRepository;
    private final RegisterRepository registerRepository;

    @Transactional
    public void incrementEvents() {
        BasicInfo info = getOrCreate();
        info.setTotalEventsHosted(info.getTotalEventsHosted() + 1);
    }

    @Transactional
    public void incrementStudents() {
        BasicInfo info = getOrCreate();
        info.setTotalStudentParticipated(info.getTotalStudentParticipated() + 1);
    }

    @Transactional
    public void incrementCollegeConnected() {
        BasicInfo info = getOrCreate();
        info.setTotalCollegeConnected(info.getTotalCollegeConnected() + 1);
    }

    /**
     * Home page stats. Events and students are counted from real data (nothing in the app ever called the
     * increment endpoints, so the stored counters stayed at 0). Colleges have no data source and keep
     * the admin-managed counter.
     */
    public BasicInfo getBasicInfo() {
        long colleges = basicInfoRepository.findById(HOME_ID)
                .map(BasicInfo::getTotalCollegeConnected)
                .orElse(0L);
        return new BasicInfo(
                HOME_ID,
                eventsRepository.countByIsApprovedTrue(),
                registerRepository.countDistinctRegisteredUsers(),
                colleges);
    }

    // The single stats row (id = 1) may not exist yet in a fresh database
    private BasicInfo getOrCreate() {
        return basicInfoRepository.findById(HOME_ID)
                .orElseGet(() -> basicInfoRepository.save(new BasicInfo(HOME_ID, 0L, 0L, 0L)));
    }
}
