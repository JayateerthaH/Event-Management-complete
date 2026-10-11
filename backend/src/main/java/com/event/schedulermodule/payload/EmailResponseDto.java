package com.event.schedulermodule.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class EmailResponseDto {
    private boolean success;
    private String jobId;
    private String jobGroup;
    private String message;
    public EmailResponseDto(boolean success,String message){
        this.success = success;
        this.message = message;
    }
}
