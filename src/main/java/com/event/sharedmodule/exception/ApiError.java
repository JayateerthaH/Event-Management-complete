package com.event.sharedmodule.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
public class ApiError {

    private LocalDateTime timeStamp;
    private String error;
    private int statusCode;     // numeric (e.g. 404); HttpStatus serialized as "404 NOT_FOUND"

    public ApiError(){
        this.timeStamp = LocalDateTime.now();
    }

    public ApiError( String error, HttpStatus statusCode) {
        this();
        this.error = error;
        this.statusCode = statusCode.value();
    }

}
