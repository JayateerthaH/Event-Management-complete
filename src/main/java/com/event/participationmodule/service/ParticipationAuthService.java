package com.event.participationmodule.service;

import com.event.sharedmodule.exception.ResourceNotFoundException;
import com.event.participationmodule.Repository.UserRepository;
import com.event.participationmodule.Repository.UserRequestRepository;
import com.event.participationmodule.entity.UserRequest;
import com.event.participationmodule.entity.Users;
import com.event.notificationmodule.EmailDeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationAuthService {

    private final UserRepository userRepository;
    private final QRService qrService;
    private final EmailService emailService;
    private final UserRequestRepository userRequestRepository;

    @Transactional
    public String sendQr(String email,Long userId,Long eventId) throws Exception {

        final String random = UUID.randomUUID().toString();
        byte[] qrPng = qrService.generateQRCode(random);

        userRequestRepository.save(new UserRequest(eventId,email,userId));
        userRepository.save(new Users(random,email,eventId));

        // Send Email with QR. A delivery failure must not undo the registration (the seat is taken and the
        // user is told they registered), so it is logged loudly; the student can use resendQr() later.
        try {
            emailQr(email, qrPng);
        } catch (EmailDeliveryException e) {
            log.error("QR email NOT delivered to {} (userId={}, eventId={}); registration kept", email, userId, eventId, e);
            return "Registration saved but QR email could not be sent to " + email;
        }

        return "Registration success! QR sent to " + email;
    }

    /**
     * Re-sends the attendance QR for the caller's own registration, to the email given at registration.
     * The existing QR code is reused, so any copy the student already has stays valid and repeated
     * calls change nothing. Delivery failures propagate (502) so the student knows it didn't arrive.
     */
    @Transactional
    public String resendQr(Long userId, Long eventId) throws Exception {
        UserRequest registration = userRequestRepository.findFirstByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not registered for event " + eventId));

        if (Boolean.TRUE.equals(registration.getIsAttended())) {
            throw new IllegalArgumentException("Attendance already marked for this event");
        }

        String email = registration.getEmail();
        // Normally created at registration; recreate it if that step never completed
        Users qrOwner = userRepository.findFirstByEmailAndEventId(email, eventId)
                .orElseGet(() -> userRepository.save(new Users(UUID.randomUUID().toString(), email, eventId)));

        emailQr(email, qrService.generateQRCode(qrOwner.getRandomId()));

        log.info("QR re-sent to {} (userId={}, eventId={})", email, userId, eventId);
        return "QR code sent to " + email;
    }

    private void emailQr(String email, byte[] qrPng) {
        emailService.sendQRMail(
                email,
                "Your Login QR Code",
                "Here is your QR code for attendance.",
                qrPng
        );
    }

}
