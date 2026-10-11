package com.event.schedulermodule.Quartz_Job;


import com.event.notificationmodule.NotificationApi;
import com.event.schedulermodule.payload.EmailRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailJob extends QuartzJobBean {

    private final NotificationApi notificationApi;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();

        String subject = jobDataMap.getString("subject");
        String body = jobDataMap.getString("body");
        String recipientEmail = jobDataMap.getString("email");

        try {
            notificationApi.sendEmail(recipientEmail, subject, body);
        } catch (Exception e) {
            // Quartz's own failure type: the scheduler records/logs the job failure instead of an opaque RuntimeException
            throw new JobExecutionException("Scheduled email to " + recipientEmail + " failed", e, false);
        }
    }
}
