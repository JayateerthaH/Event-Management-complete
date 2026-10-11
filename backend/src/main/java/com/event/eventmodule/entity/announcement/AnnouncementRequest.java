package com.event.eventmodule.entity.announcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnnouncementRequest {
    private String title;
    private String message;
}
