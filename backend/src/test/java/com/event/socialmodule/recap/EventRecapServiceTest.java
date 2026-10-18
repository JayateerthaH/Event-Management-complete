package com.event.socialmodule.recap;

import com.event.eventmodule.api.EventApi;
import com.event.eventmodule.api.EventCard;
import com.event.eventmodule.api.EventProfile;
import com.event.sharedmodule.EventCompletedEvent;
import com.event.socialmodule.entity.BuddyNotification;
import com.event.socialmodule.follow.OrganizerFollowRepository;
import com.event.socialmodule.repository.BuddyNotificationRepository;
import com.event.socialmodule.settings.NotificationPrefsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EventRecapServiceTest {

    private final EventApi eventApi = mock(EventApi.class);
    private final BuddyNotificationRepository notifications = mock(BuddyNotificationRepository.class);
    private final NotificationPrefsService prefs = mock(NotificationPrefsService.class);
    private final EventRecapService service = new EventRecapService(eventApi, notifications, prefs, mock(OrganizerFollowRepository.class));

    private EventProfile profile(Date when) {
        EventCard card = new EventCard(5L, "Hack Night", "", "Workshop", null, when, "Lab", 50L, "0", null, 12);
        return new EventProfile(card, true, null, "IEEE", null, null, 2L, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void thanksEachAttendeeOnceWhoAllowsIt() {
        when(eventApi.eventProfile(5L)).thenReturn(Optional.of(profile(new Date())));
        when(prefs.allows(eq(1L), any())).thenReturn(true);
        when(prefs.allows(eq(2L), any())).thenReturn(false);
        service.thankAttendees(new EventCompletedEvent(5L, "Hack Night", List.of(1L, 1L, 2L)));
        ArgumentCaptor<BuddyNotification> saved = ArgumentCaptor.forClass(BuddyNotification.class);
        verify(notifications, times(1)).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(1L);
        assertThat(saved.getValue().getType()).isEqualTo(BuddyNotification.Type.EVENT_RECAP);
        assertThat(saved.getValue().getMessage()).isEqualTo(
                "Thanks for coming to Hack Night! Tell the organizers how it went, check for your certificate, and follow IEEE for their next event.");
    }

    @Test
    void organizerRecapWaitsUntilTheEventStarts() {
        when(eventApi.eventProfile(5L)).thenReturn(Optional.of(profile(new Date(System.currentTimeMillis() + 3_600_000))));
        assertThat(service.recap(5L).available()).isFalse();
    }

    @Test
    void organizerRecapCountsShowUpAndRating() {
        when(eventApi.eventProfile(5L)).thenReturn(Optional.of(profile(new Date(System.currentTimeMillis() - 3_600_000))));
        when(eventApi.checkedInCount(5L)).thenReturn(9L);
        when(eventApi.feedbackForEvent(5L)).thenReturn(List.of(new com.event.eventmodule.api.FeedbackEntry(4, "good"), new com.event.eventmodule.api.FeedbackEntry(5, "")));
        when(eventApi.organizerOf(5L)).thenReturn(Optional.empty());
        var r = service.recap(5L);
        assertThat(r.showUpRate()).isEqualTo(0.75);
        assertThat(r.averageRating()).isEqualTo(4.5);
        assertThat(r.feedbackCount()).isEqualTo(2);
    }
}
