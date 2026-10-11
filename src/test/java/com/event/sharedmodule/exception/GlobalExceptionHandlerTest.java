package com.event.sharedmodule.exception;

import com.event.notificationmodule.EmailDeliveryException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every failure must come back as an ApiError JSON body with the right status and a numeric statusCode.
 * Malformed JSON and type mismatches are produced by Spring's real request processing, not thrown by hand.
 */
class GlobalExceptionHandlerTest {

    @RestController
    static class ThrowingController {
        @GetMapping("/item/{id}")
        String item(@PathVariable Long id) { return "ok"; }

        @PostMapping("/body")
        String body(@RequestBody Map<String, Object> body) { return "ok"; }

        @GetMapping("/throw/{kind}")
        String fail(@PathVariable String kind) {
            switch (kind) {
                case "notFound" -> throw new ResourceNotFoundException("Event not found with id: 9");
                case "badArg" -> throw new IllegalArgumentException("Invalid QR Data");
                case "denied" -> throw new AccessDeniedException("nope");
                case "auth" -> throw new BadCredentialsException("Bad credentials");
                case "conflict" -> throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"uk_secret_column\"");
                case "email" -> throw new EmailDeliveryException("brevo down", new RuntimeException());
                case "badJwt" -> throw new MalformedJwtException("Unrecognized token 'i' at [Source: internal]");
                case "expiredJwt" -> throw new ExpiredJwtException(null, null, "JWT expired 5000 ms ago at 2026-01-01");
                case "gone" -> throw new ResponseStatusException(HttpStatus.GONE, "Sorry The Seats Are Full");
                default -> throw new IllegalStateException("internal detail: password=hunter2");
            }
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void malformedJsonIs400() throws Exception {
        expect(mvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{not json")),
                400, "Malformed request body");
    }

    @Test
    void wrongPathVariableTypeIs400() throws Exception {
        expect(mvc.perform(get("/item/abc")), 400, "Invalid value for parameter 'id'");
    }

    @Test
    void wrongMethodKeepsItsStatus() throws Exception {
        expect(mvc.perform(post("/item/1")), 405, "Method Not Allowed");
    }

    @Test
    void domainExceptionsMapToTheirStatuses() throws Exception {
        expect(mvc.perform(get("/throw/notFound")), 404, "Event not found with id: 9");
        expect(mvc.perform(get("/throw/badArg")), 400, "Invalid QR Data");
        expect(mvc.perform(get("/throw/denied")), 403, "Access denied");
        expect(mvc.perform(get("/throw/auth")), 401, "Authentication failed");
        expect(mvc.perform(get("/throw/expiredJwt")), 401, "Token expired");
        expect(mvc.perform(get("/throw/badJwt")), 401, "Invalid token")
                .andExpect(jsonPath("$.error", not(containsString("Unrecognized"))));
        expect(mvc.perform(get("/throw/gone")), 410, "Sorry The Seats Are Full");
        expect(mvc.perform(get("/throw/email")), 502, "Email could not be sent");
    }

    @Test
    void conflictDoesNotLeakSqlDetails() throws Exception {
        expect(mvc.perform(get("/throw/conflict")), 409, "Request conflicts with existing data")
                .andExpect(jsonPath("$.error", not(containsString("uk_secret_column"))));
    }

    @Test
    void unexpectedErrorsAre500WithoutInternalDetails() throws Exception {
        expect(mvc.perform(get("/throw/other")), 500, "An unexpected error occurred")
                .andExpect(jsonPath("$.error", not(containsString("hunter2"))));
    }

    private static ResultActions expect(ResultActions result, int status, String messagePart) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(jsonPath("$.statusCode").value(status))
                .andExpect(jsonPath("$.error", containsString(messagePart)))
                .andExpect(jsonPath("$.timeStamp").exists());
    }
}
