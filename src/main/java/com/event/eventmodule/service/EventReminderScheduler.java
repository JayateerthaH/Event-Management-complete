package com.event.eventmodule.service;

import com.event.eventmodule.Registration.Register;
import com.event.eventmodule.Registration.RegisterRepository;
import com.event.eventmodule.entity.Events;
import com.event.notificationmodule.NotificationApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Internal Scheduler for the Event Module.
 * This class is package-private because no other module needs to call it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class EventReminderScheduler {

    private final RegisterRepository registerRepository;
    private final NotificationApi notificationApi; // Injecting the shared Notification API

    /**
     * Track which reminders have been sent in-memory: "eventId-userId-type"
     * Note: In a large production system, this would be a database table.
     */
    private final Set<String> sentReminders = Collections.synchronizedSet(new HashSet<>());

    /**
     * Runs every 15 minutes. Checks for events happening within the next 25 hours
     * and triggers email reminders via the Notification Module.
     */
    @Scheduled(fixedRate = 900_000)
    public void sendEventReminders() {
        Date now = new Date();
        // Look 25 hours ahead to capture the 1-day reminder window
        Date horizon = new Date(now.getTime() + 25L * 60 * 60 * 1000);

        List<Register> registrations;
        try {
            // Logic: Database-level filtering to avoid N+1 issues
            registrations = registerRepository.findRegistrationsForEventsBetween(now, horizon);
        } catch (Exception e) {
            log.error("Failed to fetch upcoming registrations for reminders", e);
            return;
        }

        for (Register reg : registrations) {
            Events event = reg.getEventsDto();
            if (event == null || event.getEventDate() == null || reg.getEmail() == null) continue;

            long millisUntilEvent = event.getEventDate().getTime() - now.getTime();
            long hoursUntilEvent = millisUntilEvent / (60 * 60 * 1000);

            // 1. Logic for "1 day before" (23 to 25 hours window)
            if (hoursUntilEvent >= 23 && hoursUntilEvent <= 25) {
                sendReminder(reg, event, "1day", "Event Tomorrow");
            }

            // 2. Logic for "1 hour before" (30 min to 90 min window)
            if (hoursUntilEvent >= 0 && hoursUntilEvent <= 1) {
                long minutesUntilEvent = millisUntilEvent / (60 * 1000);
                if (minutesUntilEvent >= 30 && minutesUntilEvent <= 90) {
                    sendReminder(reg, event, "1hour", "Event Starting Soon");
                }
            }
        }
    }

    /**
     * Bridges the gap between the Event logic and the Notification module.
     */
    private void sendReminder(Register reg, Events event, String type, String timingLabel) {
        String key = event.getId() + "-" + reg.getUserId() + "-" + type;
        if (sentReminders.contains(key)) return;

        try {
            String subject = timingLabel + ": " + event.getName();
            String body = buildEmailBody(reg, event, type);

            // ✅ CLEAN CODE: Direct Java function call to Notification module
            notificationApi.sendEmail(reg.getEmail(), subject, body);

            sentReminders.add(key);
            log.info("Successfully triggered {} reminder to {} for event {}", type, reg.getEmail(), event.getId());
        } catch (Exception e) {
            log.error("Failed to trigger {} reminder to {} for event {}", type, reg.getEmail(), event.getId(), e);
        }
    }

    /**
     * Generates the HTML template for the reminder.
     */
    private String buildEmailBody(Register reg, Events event, String type) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a");
        String eventDateStr = sdf.format(event.getEventDate());

        String timeMessage = "1day".equals(type)
                ? "is happening <strong>tomorrow</strong>"
                : "is starting <strong>in about 1 hour</strong>";

        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background:#f8f9fa;border-radius:12px;overflow:hidden;border:1px solid #e2e8f0;">
              <div style="background:linear-gradient(135deg,#7c3aed,#6d28d9);padding:30px;text-align:center;">
                <h1 style="color:#fff;margin:0;font-size:24px;">Event Reminder</h1>
              </div>
              <div style="padding:30px;">
                <p style="font-size:16px;color:#333;">Hi <strong>%s</strong>,</p>
                <p style="font-size:16px;color:#333;">Your registered event %s!</p>
                <div style="background:#fff;border:1px solid #e2e8f0;border-radius:10px;padding:20px;margin:20px 0;">
                  <h2 style="color:#7c3aed;margin:0 0 12px;">%s</h2>
                  <table style="width:100%%;border-collapse:collapse;">
                    <tr>
                      <td style="padding:8px 0;color:#666;font-size:14px;">Date & Time</td>
                      <td style="padding:8px 0;color:#333;font-size:14px;font-weight:600;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:8px 0;color:#666;font-size:14px;">Venue</td>
                      <td style="padding:8px 0;color:#333;font-size:14px;font-weight:600;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:8px 0;color:#666;font-size:14px;">Category</td>
                      <td style="padding:8px 0;color:#333;font-size:14px;font-weight:600;">%s</td>
                    </tr>
                  </table>
                </div>
                <div style="text-align:center;margin:24px 0;">
                  <a href="http://localhost:5173/events/%d"
                     style="display:inline-block;background:linear-gradient(135deg,#7c3aed,#6d28d9);color:#fff;padding:14px 32px;border-radius:8px;text-decoration:none;font-weight:700;font-size:15px;">
                    View Event Details
                  </a>
                </div>
                <p style="font-size:13px;color:#999;text-align:center;">
                  You received this email because you registered for this event on CampusEvents.
                </p>
              </div>
            </div>
            """.formatted(
                reg.getName() != null ? reg.getName() : "Student",
                timeMessage,
                event.getName() != null ? event.getName() : "Upcoming Event",
                eventDateStr,
                event.getVenue() != null ? event.getVenue() : "TBD",
                event.getCategory() != null ? event.getCategory() : "General",
                event.getId()
        );
    }

    /**
     * Midnight Cleanup: Clears the in-memory set to prevent memory leaks.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupSentReminders() {
        sentReminders.clear();
        log.info("Midnight cleanup: Cleared sent-reminder tracking set.");
    }
}