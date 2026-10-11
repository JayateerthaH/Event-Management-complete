package com.event.participationmodule.dto;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EmailRequest {
    private String to;
    private String subject;
    private String body;
    private String base64Qr; // QR as Base64

    // getters & setters
}
