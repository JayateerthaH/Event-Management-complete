package com.event.authmodule.security;

import com.event.analyticsmodule.communication.DashboardApi;
import com.event.authmodule.dtoPayload.*;
import com.event.authmodule.entitys.OnBoard;
import com.event.authmodule.entitys.RefreshToken;
import com.event.authmodule.entitys.Student;
import com.event.authmodule.entitys.UserInfo;
import com.event.authmodule.entitys.type.AuthProviderType;
import com.event.authmodule.entitys.type.RoleType;
import com.event.authmodule.repository.OnBoardDtoRepository;
import com.event.authmodule.repository.RefreshTokenRepository;
import com.event.authmodule.repository.StudentRepository;
import com.event.authmodule.repository.UserInfoRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthUtil authUtil;
    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository studentRepository;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OnBoardDtoRepository onBoardDtoRepository;

    // Production with frontend and API on different sites needs COOKIE_SECURE=true and COOKIE_SAME_SITE=None,
    // otherwise the browser won't send the refresh cookie and users are logged out when the access token expires.
    @org.springframework.beans.factory.annotation.Value("${app.cookie.secure:false}")
    private boolean cookieSecure = false;
    @org.springframework.beans.factory.annotation.Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite = "Lax";
    private final DashboardApi dashboardApi;              // ✅ replaces UserClient

    // ─────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────
    @Transactional
    public LoginResponseDto login(LoginRequestDto loginRequestDto,
                                  HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDto.getUsername(),
                        loginRequestDto.getPassword()
                )
        );

        UserInfo userInfo = (UserInfo) authentication.getPrincipal();

        String accessToken = authUtil.generateAccessToken(userInfo);
        RefreshToken refreshToken = authUtil.generateRefreshToken(accessToken, userInfo.getUsername());

        addRefreshTokenCookie(response, refreshToken);

        log.info("Login successful for user: {}", userInfo.getUsername());
        return new LoginResponseDto(accessToken, userInfo.getUserId());
    }

    // ─────────────────────────────────────────────
    // SIGNUP INTERNAL (shared by email + OAuth2)
    // ─────────────────────────────────────────────
    // Not proxied (called via `this`), so the transaction comes from the public callers:
    // signup() and handleOAuth2loginRequest().
    private UserInfo signUpInternal(SignUpRequestDto signupRequestDto,
                                      AuthProviderType authProviderType,
                                      String providerId) {

        // 1. Check duplicate
        userInfoRepository.findByUsername(signupRequestDto.getUsername())
                .ifPresent(u -> { throw new IllegalArgumentException("User already exists"); });

        // 2. Save onboarding requests. Requested roles are only granted later by an admin
        //    (AdminServiceImpl), so the user starts out as a STUDENT either way.
        for (RoleType role : signupRequestDto.getRoles()) {
            if (role == RoleType.ORGANIZER) {
                onBoardDtoRepository.save(
                        new OnBoard(signupRequestDto.getUsername(), RoleType.ORGANIZER)
                );
            }
            if (role == RoleType.ADMIN) {
                onBoardDtoRepository.save(
                        new OnBoard(signupRequestDto.getUsername(), RoleType.ADMIN)
                );
            }
        }

        // 3. Build and save UserInfo
        UserInfo userInfo = UserInfo.builder()
                .username(signupRequestDto.getUsername())
                .providerId(providerId)
                .providerType(authProviderType)
                .roles(new HashSet<>(Set.of(RoleType.STUDENT)))
                .build();

        if (authProviderType == AuthProviderType.EMAIL) {
            userInfo.setPassword(passwordEncoder.encode(signupRequestDto.getPassword()));
        }

        userInfo = userInfoRepository.save(userInfo);

        // 4. Save Student record
        studentRepository.save(
                Student.builder()
                        .email(signupRequestDto.getUsername())
                        .userInfo(userInfo)
                        .build()
        );

        // 5. Initialize the student dashboard. The organizer dashboard is created on approval
        //    (AdminServiceImpl.onBoardOrganizer); creating it here too produced duplicate rows.
        dashboardApi.initializeStudentDashboard(userInfo.getUserId());

        log.info("Signup complete for user: {}", userInfo.getUsername());
        return userInfo;
    }

    // ─────────────────────────────────────────────
    // EMAIL SIGNUP
    // ─────────────────────────────────────────────
    @Transactional
    public ResponseEntity<SignUpResponseDto> signup(SignUpRequestDto signupRequestDto) {

        UserInfo user = signUpInternal(signupRequestDto, AuthProviderType.EMAIL, null);

        return ResponseEntity.ok(new SignUpResponseDto(
                user.getUserId(),
                user.getUsername()
        ));
    }

    // ─────────────────────────────────────────────
    // OAUTH2 LOGIN / SIGNUP
    // ─────────────────────────────────────────────
    @Transactional
    public ResponseEntity<LoginResponseDto> handleOAuth2loginRequest(OAuth2User oAuth2User,
                                                                     String registrationId,
                                                                     HttpServletResponse response) {

        AuthProviderType providerType = authUtil.getProviderTypeFromRegistrationId(registrationId);
        String providerId = authUtil.determineProviderIdFromOAuth2User(oAuth2User, registrationId);

        UserInfo userInfo = userInfoRepository
                .findByProviderIdAndProviderType(providerId, providerType)
                .orElse(null);

        String email = oAuth2User.getAttribute("email");

        UserInfo emailUser = userInfoRepository.findByUsername(email).orElse(null);

        if (userInfo == null && emailUser == null) {
            // New OAuth2 user — signup
            String username = authUtil.determineUsernameFromOAuth2User(
                    oAuth2User, registrationId, providerId);
            userInfo = signUpInternal(
                    new SignUpRequestDto(username, null, Set.of(RoleType.STUDENT)),
                    providerType,
                    providerId
            );
        } else if (userInfo != null) {
            // Existing OAuth2 user — update email if changed
            if (email != null && !email.isBlank() && !email.equals(userInfo.getUsername())) {
                userInfo.setUsername(email);
                userInfoRepository.save(userInfo);
            }
        } else {
            throw new BadCredentialsException(
                    "This email is already registered with a different provider: " + email);
        }

        String accessToken = authUtil.generateAccessToken(userInfo);
        RefreshToken refreshToken = authUtil.generateRefreshToken(accessToken, userInfo.getUsername());

        addRefreshTokenCookie(response, refreshToken);

        log.info("OAuth2 login successful for user: {}", userInfo.getUsername());
        return ResponseEntity.ok(new LoginResponseDto(accessToken, userInfo.getUserId()));
    }

    // ─────────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────────
    @Transactional
    public ResponseEntity<Map<String, Object>> refreshToken(HttpServletRequest request,
                                                            HttpServletResponse response) {

        List<String> presented = authUtil.extractRefreshTokensFromCookies(request);
        if (presented.isEmpty()) {
            throw new BadCredentialsException("Refresh token not found");
        }
        RefreshToken tokenEntity = presented.stream()
                .map(refreshTokenRepository::findByToken)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        refreshTokenService.verifyExpiration(tokenEntity);

        UserInfo user = tokenEntity.getUserInfo();

        // Rotate refresh token: single use — only the request that deletes it may continue
        if (refreshTokenRepository.deleteByTokenValue(tokenEntity.getToken()) != 1) {
            throw new BadCredentialsException("Refresh token already used or revoked");
        }
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        addRefreshTokenCookie(response, newRefreshToken);

        String newAccessToken = authUtil.generateAccessToken(user);

        log.info("Token refreshed for user: {}", user.getUsername());
        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "userId", user.getUserId()
        ));
    }

    // ─────────────────────────────────────────────
    // LOGOUT — revoke the refresh token server-side and expire the cookie
    // ─────────────────────────────────────────────
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authUtil.extractRefreshTokensFromCookies(request)
                .forEach(refreshTokenRepository::deleteByTokenValue);
        writeRefreshCookie(response, "", 0, REFRESH_COOKIE_PATH);
        writeRefreshCookie(response, "", 0, LEGACY_REFRESH_COOKIE_PATH);
    }

    // Single place that writes the refresh cookie, so path/lifetime always match the token
    // (RefreshTokenService issues 7-day tokens) and a rotated cookie overwrites the old one.
    // Path covers /auth/refresh and /auth/logout (logout must receive the cookie to revoke it).
    private static final String REFRESH_COOKIE_PATH = "/auth";
    private static final String LEGACY_REFRESH_COOKIE_PATH = "/auth/refresh";

    private void addRefreshTokenCookie(HttpServletResponse response, RefreshToken refreshToken) {
        writeRefreshCookie(response, refreshToken.getToken(), 7 * 24 * 60 * 60, REFRESH_COOKIE_PATH);
        // Expire any cookie issued under the old, narrower path so it can't shadow the new one
        writeRefreshCookie(response, "", 0, LEGACY_REFRESH_COOKIE_PATH);
    }

    private void writeRefreshCookie(HttpServletResponse response, String value, int maxAge, String path) {
        Cookie cookie = new Cookie("refreshToken", value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(cookie);
    }
}