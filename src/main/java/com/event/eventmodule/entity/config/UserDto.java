package com.event.eventmodule.entity.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class UserDto {

    private Long userId;
    private String email;
    private Long eventId;

}
