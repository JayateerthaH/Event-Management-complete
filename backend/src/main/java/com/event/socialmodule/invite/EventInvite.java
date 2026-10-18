package com.event.socialmodule.invite;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** An organizer invited past attendees to this event (allowed once per event, so nobody gets spammed). */
@Entity
@Table(name = "event_invite", schema = "social")
@Getter
@NoArgsConstructor
public class EventInvite {

    @Id
    private Long eventId;

    private Long sentBy;

    private int recipients;

    private LocalDateTime sentAt;

    public EventInvite(Long eventId, Long sentBy, int recipients) {
        this.eventId = eventId;
        this.sentBy = sentBy;
        this.recipients = recipients;
        this.sentAt = LocalDateTime.now();
    }
}
