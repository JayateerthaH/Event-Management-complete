package com.event.authmodule.security;


import com.event.authmodule.entitys.RefreshToken;
import com.event.authmodule.entitys.UserInfo;
import com.event.authmodule.entitys.type.AuthProviderType;
import com.event.authmodule.entitys.type.RoleType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthUtil {

    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.secretKey}")
    private String jwtSecretKey;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UserInfo user) {

        Set<String> roles = user.getRoles()
                .stream()
                .map(RoleType::name)   // ✅ convert enum → String
                .collect(Collectors.toSet());

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getUserId().toString())
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000*60*10))
                .signWith(getSecretKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        String jwt = cleanToken(token);

        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();

        return claims.getSubject();
    }


    public AuthProviderType getProviderTypeFromRegistrationId(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> AuthProviderType.GOOGLE;
            case "github" -> AuthProviderType.GITHUB;
            case "facebook" -> AuthProviderType.FACEBOOK;
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + registrationId);
        };
    }


    public String determineProviderIdFromOAuth2User(OAuth2User oAuth2User, String registrationId) {
        String providerId = switch (registrationId.toLowerCase()) {
            case "google" -> oAuth2User.getAttribute("sub");
            case "github" -> oAuth2User.getAttribute("id").toString();

            default -> {
                log.error("Unsupported OAuth2 provider: {}", registrationId);
                throw new IllegalArgumentException("Unsupported OAuth2 provider: " + registrationId);
            }
        };

        if (providerId == null || providerId.isBlank()) {
            log.error("Unable to determine providerId for provider: {}", registrationId);
            throw new IllegalArgumentException("Unable to determine providerId for OAuth2 login");
        }
        return providerId;
    }

    public String determineUsernameFromOAuth2User(OAuth2User oAuth2User, String registrationId, String providerId) {
        String email = oAuth2User.getAttribute("email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        return switch (registrationId.toLowerCase()) {
            case "google" -> oAuth2User.getAttribute("sub");
            case "github" -> oAuth2User.getAttribute("login");
            default -> providerId;
        };
    }

    public RefreshToken generateRefreshToken(String token, String username){
        return refreshTokenService.createRefreshToken(username);
    }

    /** All refreshToken cookie values; a legacy-path cookie can be sent alongside the current one. */
    public List<String> extractRefreshTokensFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return List.of();
        return Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals("refreshToken") && !c.getValue().isBlank())
                .map(Cookie::getValue)
                .toList();
    }

    public boolean isTokenValid(String token) {
        try {
            String jwt = cleanToken(token);

            // 🔥 Prevent UUID / empty / garbage parsing
            if (!jwt.contains(".")) {
                log.warn("Not a JWT");
                return false;
            }

            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(jwt);

            return true;

        } catch (Exception e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }


    private String cleanToken(String token) {
        if (token == null) return "";
        if (token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }


    public String extractUserId(String token) {
        return (extractAllClaims(token).get("userId", String.class));
    }

    public Set<String> extractRoles(String token) {
        return new HashSet<>(extractAllClaims(token).get("roles", List.class));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
