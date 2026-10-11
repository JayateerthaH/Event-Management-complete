package com.event.schedulermodule.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@NoArgsConstructor // required by Jackson for @RequestBody binding
public class EmailRequestDto {
    @Email
    @NotEmpty
    private String to;

    @NotEmpty
    private String subject;

    @NotEmpty
    private String body;

    // @NotEmpty only applies to strings/collections; it throws UnexpectedTypeException on these types
    @NotNull
    private LocalDateTime dateTime;

    @NotNull
    private ZoneId timeZone;
}
