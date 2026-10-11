package com.event.sharedmodule.exception;

// Unchecked so it propagates through lambdas/services and is mapped to 404 by GlobalExceptionHandler.
// (Previously extended Throwable, which Spring MVC wraps in an IllegalStateException → 500.)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String s) {
        super(s);
    }
}
