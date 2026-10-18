package com.event.socialmodule.nudge;

import com.event.eventmodule.api.EventApi;
import com.event.socialmodule.entity.BuddyNotification;
import com.event.socialmodule.entity.BuddyNotification.Type;
import com.event.socialmodule.repository.BuddyNotificationRepository;
import com.event.socialmodule.settings.NotificationPrefsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** The evening after an event, attendees who haven't rated it are asked once (feedback often unlocks the certificate). */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackNudge {

    static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final EventApi eventApi;
    private final BuddyNotificationRepository notifications;
    private final NotificationPrefsService prefs;

    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void askYesterdaysAttendees() {
        LocalDate today = LocalDate.now(ZONE);
        Date from = Date.from(today.minusDays(1).atStartOfDay(ZONE).toInstant());
        Date to = Date.from(today.atStartOfDay(ZONE).toInstant());
        int sent = 0;
        for (EventApi.CheckIn c : eventApi.checkInsWithoutFeedback(from, to)) {
            if (notifications.existsByUserIdAndTypeAndEventId(c.userId(), Type.FEEDBACK_ASK, c.eventId()) || !prefs.allows(c.userId(), Type.FEEDBACK_ASK)) continue;
            notifications.save(new BuddyNotification(c.userId(), Type.FEEDBACK_ASK, null, c.eventId(),
                    "How was " + c.eventName() + "? Rate it in 30 seconds (it can unlock your certificate)."));
            sent++;
        }
        if (sent > 0) log.info("Asked {} attendees for feedback", sent);
    }
}
