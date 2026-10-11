package com.event.eventmodule.Registration;

import com.event.eventmodule.entity.home.EventSummaryDto;
import com.event.eventmodule.entity.Events;
import com.event.eventmodule.entity.config.UserDto;
import com.event.sharedmodule.SelfOrAdmin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;
    private final UserRegistrationRepository userRegistrationRepository;
    private final RegisterRepository registerRepository;

    @PostMapping("/{eventId}/register")
    public synchronized ResponseEntity<?> register(@PathVariable Long eventId, @RequestBody Register register,@RequestAttribute("userId") String userId) throws Exception {
        return registrationService.save(Long.valueOf(userId),eventId,register);
    }

    // Returns every user's registration mapping — admin only
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/registration")
    public List<UserRegistration> registration(){
        return userRegistrationRepository.findAll();
    }

    @SelfOrAdmin
    @GetMapping("/user/{userId}/upcoming")
    public List<EventSummaryDto> getUpcomingEvents(@PathVariable Long userId) {
        return registrationService.getUpcomingEventsForUser(userId).stream().map(EventSummaryDto::from).toList();
    }

    @SelfOrAdmin
    @GetMapping("/user/{userId}/past")
    public List<EventSummaryDto> getPastEvents(@PathVariable Long userId) {
        return registrationService.getPastEventsForUser(userId).stream().map(EventSummaryDto::from).toList();
    }

    // Attendance is normally marked via QR scan (AttendanceService → EventApi).
    // Unrestricted, any user could mark themselves attended and claim a certificate.
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/mark/attendance")
    public void markAttendance(@RequestBody UserRequestDto userRequestDto){
         registerRepository.markAttended(userRequestDto.getUserId(),userRequestDto.getEventId());
    }

}
