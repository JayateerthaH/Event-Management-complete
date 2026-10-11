package com.event.participationmodule.service;

import com.event.notificationmodule.EmailDeliveryException;
import com.event.participationmodule.Repository.UserRepository;
import com.event.participationmodule.Repository.UserRequestRepository;
import com.event.participationmodule.entity.UserRequest;
import com.event.participationmodule.entity.Users;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResendQrTest {

    private static final long USER_ID = 7L;
    private static final long EVENT_ID = 3L;
    // Registration-form email, deliberately different from any login email
    private static final String REG_EMAIL = "student.personal@example.com";

    private UserRepository users;
    private UserRequestRepository requests;
    private EmailService email;
    private ParticipationAuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        requests = mock(UserRequestRepository.class);
        email = mock(EmailService.class);
        service = new ParticipationAuthService(users, new QRService(), email, requests);
    }

    @Test
    void resendsTheExistingQrToTheRegistrationEmail() throws Exception {
        registered(false);
        when(users.findFirstByEmailAndEventId(REG_EMAIL, EVENT_ID))
                .thenReturn(Optional.of(new Users("existing-code-123", REG_EMAIL, EVENT_ID)));

        String result = service.resendQr(USER_ID, EVENT_ID);

        ArgumentCaptor<byte[]> png = ArgumentCaptor.forClass(byte[].class);
        verify(email).sendQRMail(eq(REG_EMAIL), anyString(), anyString(), png.capture());
        assertThat(decodeQr(png.getValue())).isEqualTo("existing-code-123");   // same code, still valid
        verify(users, never()).save(any());                                     // nothing rewritten
        assertThat(result).contains(REG_EMAIL);
    }

    @Test
    void notRegisteredIsNotFoundAndSendsNothing() {
        when(requests.findFirstByUserIdAndEventId(USER_ID, EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resendQr(USER_ID, EVENT_ID)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(email);
    }

    @Test
    void alreadyAttendedIsRejected() {
        registered(true);

        assertThatThrownBy(() -> service.resendQr(USER_ID, EVENT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already marked");
        verifyNoInteractions(email);
    }

    @Test
    void missingQrRecordIsRecreated() throws Exception {
        registered(false);
        when(users.findFirstByEmailAndEventId(REG_EMAIL, EVENT_ID)).thenReturn(Optional.empty());
        when(users.save(any(Users.class))).thenAnswer(inv -> inv.getArgument(0));

        service.resendQr(USER_ID, EVENT_ID);

        ArgumentCaptor<Users> saved = ArgumentCaptor.forClass(Users.class);
        verify(users).save(saved.capture());
        ArgumentCaptor<byte[]> png = ArgumentCaptor.forClass(byte[].class);
        verify(email).sendQRMail(eq(REG_EMAIL), anyString(), anyString(), png.capture());
        assertThat(decodeQr(png.getValue())).isEqualTo(saved.getValue().getRandomId());
    }

    @Test
    void deliveryFailureIsReportedToTheCaller() {
        registered(false);
        when(users.findFirstByEmailAndEventId(REG_EMAIL, EVENT_ID))
                .thenReturn(Optional.of(new Users("existing-code-123", REG_EMAIL, EVENT_ID)));
        doThrow(new EmailDeliveryException("brevo down", new RuntimeException()))
                .when(email).sendQRMail(anyString(), anyString(), anyString(), any());

        // Unlike registration, a resend is an explicit request, so the failure surfaces (502 via the handler)
        assertThatThrownBy(() -> service.resendQr(USER_ID, EVENT_ID)).isInstanceOf(EmailDeliveryException.class);
    }

    private void registered(boolean attended) {
        UserRequest request = new UserRequest(EVENT_ID, REG_EMAIL, USER_ID);
        request.setIsAttended(attended);
        when(requests.findFirstByUserIdAndEventId(USER_ID, EVENT_ID)).thenReturn(Optional.of(request));
    }

    private static String decodeQr(byte[] png) throws Exception {
        var image = ImageIO.read(new ByteArrayInputStream(png));
        var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        return new MultiFormatReader().decode(bitmap).getText();
    }
}
