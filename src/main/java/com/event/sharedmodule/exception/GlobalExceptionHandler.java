package com.event.sharedmodule.exception;

import com.event.notificationmodule.EmailDeliveryException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Handle username not found exception (the message already names the user)
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        return build(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // Handle authentication failure (also used by the security entry point for missing/invalid credentials)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex) {
        return build("Authentication failed: " + ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    // Handle JWT token related issues
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiError> handleJwtException(JwtException ex) {
        // Fixed messages: parser details are logged, not returned. "expired" tells the client to refresh.
        log.debug("Rejected JWT: {}", ex.getMessage());
        String message = ex instanceof ExpiredJwtException ? "Token expired" : "Invalid token";
        return build(message, HttpStatus.UNAUTHORIZED);
    }

    // Spring Security's AccessDeniedException: raised by @PreAuthorize, EventAccessAspect and service checks
    @ExceptionHandler({AccessDeniedException.class, java.nio.file.AccessDeniedException.class})
    public ResponseEntity<ApiError> handleAccessDeniedException(Exception ex) {
        return build("Access denied: Insufficient permissions", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex) {
        return build(ex.getMessage() != null ? ex.getMessage() : "Resource not found", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
        return build(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // Body is not valid JSON or doesn't match the expected types
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return build("Malformed request body", HttpStatus.BAD_REQUEST);
    }

    // e.g. /events/abc where a numeric id is expected
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build("Invalid value for parameter '" + ex.getName() + "'", HttpStatus.BAD_REQUEST);
    }

    // Unique/foreign-key violations; the SQL detail is logged, not returned
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build("Request conflicts with existing data", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<ApiError> handleEmailDelivery(EmailDeliveryException ex) {
        log.error("Email delivery failed", ex);
        return build("Email could not be sent, please try again later", HttpStatus.BAD_GATEWAY);
    }

    // Business-rule failures thrown with an explicit status, e.g. "Seats full" (410)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return build(ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(), status);
    }

    // Handle any other uncaught exceptions. Internal details are logged, not returned to the client.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        // Spring MVC's own exceptions (unknown route, wrong method, bad media type...) carry their status
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            return build(status.getReasonPhrase(), status);
        }
        log.error("Unhandled exception", ex);
        return build("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiError> build(String message, HttpStatus status) {
        return new ResponseEntity<>(new ApiError(message, status), status);
    }
}
