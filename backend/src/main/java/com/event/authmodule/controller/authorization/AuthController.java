package com.event.authmodule.controller.authorization;

import com.event.authmodule.dtoPayload.LoginRequestDto;
import com.event.authmodule.dtoPayload.SignUpRequestDto;
import com.event.authmodule.security.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto, HttpServletResponse response){
        return ResponseEntity.ok(authService.login(loginRequestDto,response));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignUpRequestDto signupRequestDto){
        return (authService.signup(signupRequestDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        return authService.refreshToken(request,response);

    }

    // Idempotent: works with or without a (valid) refresh cookie
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.noContent().build();
    }

    // Admin onboarding lives only in AdminController (/admin/onBoardNewAdmin), which requires ROLE_ADMIN.
    // It must never be exposed under /auth/**, which is permitAll.

}
