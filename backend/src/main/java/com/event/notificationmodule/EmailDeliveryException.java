package com.event.notificationmodule;

/** Thrown when the email provider could not accept a message. Callers decide whether to retry or log. */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
