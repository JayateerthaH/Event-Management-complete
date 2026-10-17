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
import com.event.sharedmodule.exception.ApiError;
import com.event.sharedmodule.exception.GlobalExceptionHandler;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;

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

    // A too-long value (PostgreSQL 22001) is bad input, not a conflict with existing data
    @Test
    void tooLongValueIsBadRequestNotConflict() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<ApiError> tooLong = handler.handleConflict(new DataIntegrityViolationException("insert failed",
                new SQLException("value too long for type character varying(255)", "22001")));
        ResponseEntity<ApiError> duplicate = handler.handleConflict(new DataIntegrityViolationException("insert failed",
                new SQLException("duplicate key value violates unique constraint", "23505")));

        assertThat(tooLong.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(tooLong.getBody().getError()).isEqualTo("One of the fields is too long");
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
