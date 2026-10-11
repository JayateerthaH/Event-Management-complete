package com.event.schedulermodule.controller;


import com.event.schedulermodule.Quartz_Job.EmailJob;
import com.event.schedulermodule.payload.EmailRequestDto;
import com.event.schedulermodule.payload.EmailResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
public class EmailSchedularController {

    private final Scheduler scheduler;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/schedule/email")
    public ResponseEntity<EmailResponseDto> scheduleEmail(@RequestBody EmailRequestDto emailRequestDto) throws SchedulerException {
        // Validation errors → 400 and scheduler failures → 500 are rendered as ApiError by GlobalExceptionHandler
        if (emailRequestDto.getDateTime() == null || emailRequestDto.getTimeZone() == null) {
            throw new IllegalArgumentException("dateTime and timeZone are required.");
        }
        ZonedDateTime dateTime = ZonedDateTime.of(
                emailRequestDto.getDateTime(),
                emailRequestDto.getTimeZone()
        );

        if (dateTime.isBefore(ZonedDateTime.now())) {
            throw new IllegalArgumentException("dateTime must be after current time.");
        }

        JobDetail jobDetail = buildJobDetail(emailRequestDto);
        Trigger trigger = buildTrigger(jobDetail, dateTime);

        scheduler.scheduleJob(jobDetail, trigger);

        EmailResponseDto emailResponse = new EmailResponseDto(
                true,
                jobDetail.getKey().getName(),
                jobDetail.getKey().getGroup(),
                "Email Scheduled Successfully!"
        );
        return ResponseEntity.ok(emailResponse);
    }


    private JobDetail buildJobDetail(EmailRequestDto scheduleEmailRequest) {

        JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put("email", scheduleEmailRequest.getTo());
        jobDataMap.put("subject", scheduleEmailRequest.getSubject());
        jobDataMap.put("body", scheduleEmailRequest.getBody());

        return JobBuilder.newJob(EmailJob.class)
                .withIdentity(UUID.randomUUID().toString(), "email-jobs")
                .withDescription("Send Email Job")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildTrigger(JobDetail jobDetail, ZonedDateTime startAt) {

        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), "email-triggers")
                .withDescription("Send Email Trigger")
                .startAt(Date.from(startAt.toInstant()))
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withMisfireHandlingInstructionFireNow()
                )
                .build();

    }

}
