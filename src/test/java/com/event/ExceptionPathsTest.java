package com.event;

import com.event.notificationmodule.EmailDeliveryException;
import com.event.participationmodule.Repository.UserRepository;
import com.event.participationmodule.Repository.UserRequestRepository;
import com.event.participationmodule.entity.UserRequest;
import com.event.participationmodule.entity.Users;
import com.event.participationmodule.service.EmailService;
import com.event.participationmodule.service.ParticipationAuthService;
import com.event.participationmodule.service.QRService;
import com.event.profilemodule.Repository.InterestsRepository;
import com.event.profilemodule.Repository.NotificationsRepository;
import com.event.profilemodule.service_implementation.admin.InterestsServiceImpl;
import com.event.profilemodule.service_implementation.admin.NotificationsServiceImpl;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ExceptionPathsTest {

    // Spring Data 3+ deleteById ignores missing ids, so the services must detect "not found" themselves
    @Test
    void deletingMissingInterestIsNotFound() {
        InterestsRepository repo = mock(InterestsRepository.class);
        when(repo.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> new InterestsServiceImpl(repo).deleteInterest(42L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repo, never()).deleteById(any());
    }

    @Test
    void deletingMissingNotificationIsNotFound() {
        NotificationsRepository repo = mock(NotificationsRepository.class);
        when(repo.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> new NotificationsServiceImpl(repo).deleteNotification(42L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repo, never()).deleteById(any());
    }

    @Test
    void deletingExistingInterestDeletesIt() {
        InterestsRepository repo = mock(InterestsRepository.class);
        when(repo.existsById(5L)).thenReturn(true);

        new InterestsServiceImpl(repo).deleteInterest(5L);
        verify(repo).deleteById(5L);
    }

    // A failed QR email must not undo the registration rows
    @Test
    void qrEmailFailureKeepsRegistration() throws Exception {
        UserRepository users = mock(UserRepository.class);
        UserRequestRepository requests = mock(UserRequestRepository.class);
        EmailService email = mock(EmailService.class);
        doThrow(new EmailDeliveryException("brevo down", new RuntimeException()))
                .when(email).sendQRMail(anyString(), anyString(), anyString(), any());

        String result = new ParticipationAuthService(users, new QRService(), email, requests)
                .sendQr("student@example.com", 7L, 3L);

        verify(requests).save(any(UserRequest.class));
        verify(users).save(any(Users.class));
        assertThat(result).contains("could not be sent");
    }
}
