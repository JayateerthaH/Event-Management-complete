package com.event.participationmodule.communication;

import com.event.participationmodule.entity.UserRequest;
import com.event.participationmodule.service.ParticipationAuthService;
import org.springframework.stereotype.Service;

@Service
public class AttendanceApiImpl implements AttendanceApi {
    private final ParticipationAuthService participationAuthService;
    private final com.event.participationmodule.Repository.UserRequestRepository userRequestRepository;
    private final com.event.participationmodule.Repository.UserRepository userRepository;

    public AttendanceApiImpl(ParticipationAuthService participationAuthService,
                             com.event.participationmodule.Repository.UserRequestRepository userRequestRepository,
                             com.event.participationmodule.Repository.UserRepository userRepository) {
        this.participationAuthService = participationAuthService;
        this.userRequestRepository = userRequestRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean hasAttended(Long userId, Long eventId) {
        return userRequestRepository.findFirstByUserIdAndEventId(userId, eventId)
                .map(r -> Boolean.TRUE.equals(r.getIsAttended())).orElse(false);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void cancel(Long userId, Long eventId) {
        userRequestRepository.findFirstByUserIdAndEventId(userId, eventId).ifPresent(r -> {
            userRepository.findFirstByEmailAndEventId(r.getEmail(), eventId).ifPresent(userRepository::delete);
            userRequestRepository.delete(r);
        });
    }

    @Override
    public String register(UserRequest userRequest) throws Exception {
        participationAuthService.sendQr(
                userRequest.getEmail(),
                userRequest.getUserId(),
                userRequest.getEventId()
        );
        return "Registered Successfully";
    }
}