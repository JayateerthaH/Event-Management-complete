package com.event.eventmodule.entity.certificate;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Date;

@Getter
@AllArgsConstructor
public class CertificateResponseDto {

    private Long eventId;
    private String eventTitle;
    private Date eventDate;
    
}
