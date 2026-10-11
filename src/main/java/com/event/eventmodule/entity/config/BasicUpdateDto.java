package com.event.eventmodule.entity.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BasicUpdateDto {
    private List<Long> studentIds;
    private Long hours;
    private Long totalRegistrations;
    private Long organizerId;
    private Long costPerRegistration;
}
