package com.event.authmodule.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Verifies the ID token ("credential") that Google Identity Services gives the browser: signature against
 * Google's published keys, issuer, audience (our client id) and expiry. Only a token Google issued for this
 * app passes, so the email in it can be trusted.
 */
@Component
public class GoogleIdTokenVerifier {

    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final String clientId;
    private final String jwkSetUri;
    private volatile NimbusJwtDecoder decoder;

    public GoogleIdTokenVerifier(@Value("${GOOGLE_CLIENT_ID:not-configured}") String clientId,
                                 @Value("${app.google.jwk-set-uri:https://www.googleapis.com/oauth2/v3/certs}") String jwkSetUri) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.jwkSetUri = jwkSetUri;
    }

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientId.equals("not-configured");
    }

    /** The verified token, or BadCredentialsException if Google didn't issue it for this app. */
    public Jwt verify(String idToken) {
        if (idToken == null || idToken.isBlank()) throw new BadCredentialsException("Missing Google credential");
        try {
            return decoder().decode(idToken);
        } catch (JwtException e) {
            throw new BadCredentialsException("Google sign-in could not be verified. Please try again.", e);
        }
    }

    private NimbusJwtDecoder decoder() {
        NimbusJwtDecoder d = decoder;
        if (d == null) {
            synchronized (this) {
                if (decoder == null) {
                    // Keys are fetched from Google once and cached by Nimbus
                    NimbusJwtDecoder created = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
                    created.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                            new JwtTimestampValidator(),
                            jwt -> ISSUERS.contains(jwt.getClaimAsString("iss")) ? OAuth2TokenValidatorResult.success()
                                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong issuer", null)),
                            jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId) ? OAuth2TokenValidatorResult.success()
                                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong audience", null))));
                    decoder = created;
                }
                d = decoder;
            }
        }
        return d;
    }
}
