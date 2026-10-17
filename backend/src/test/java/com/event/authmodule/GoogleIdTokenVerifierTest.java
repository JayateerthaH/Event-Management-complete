package com.event.authmodule;

import com.event.authmodule.security.GoogleIdTokenVerifier;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Google's key endpoint is replaced by a local server publishing our own test key. */
class GoogleIdTokenVerifierTest {

    private static final String CLIENT_ID = "test-client.apps.googleusercontent.com";
    private static RSAKey googleKey;
    private static RSAKey otherKey;
    private static HttpServer jwks;
    private static GoogleIdTokenVerifier verifier;

    @BeforeAll
    static void start() throws Exception {
        googleKey = new RSAKeyGenerator(2048).keyID("google").generate();
        otherKey = new RSAKeyGenerator(2048).keyID("google").generate();   // same kid, different key
        byte[] body = new JWKSet(googleKey.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        jwks = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwks.createContext("/certs", ex -> {
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        jwks.start();
        verifier = new GoogleIdTokenVerifier(CLIENT_ID, "http://127.0.0.1:" + jwks.getAddress().getPort() + "/certs");
    }

    @AfterAll
    static void stop() {
        jwks.stop(0);
    }

    private static String token(RSAKey key, String issuer, String audience, long expiresInSeconds) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer).audience(audience).subject("1234567890")
                .claim("email", "ada@example.com").claim("email_verified", true)
                .issueTime(new Date()).expirationTime(new Date(System.currentTimeMillis() + expiresInSeconds * 1000))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    @Test
    void acceptsATokenGoogleIssuedForThisApp() throws Exception {
        Jwt jwt = verifier.verify(token(googleKey, "https://accounts.google.com", CLIENT_ID, 300));
        assertThat(jwt.getSubject()).isEqualTo("1234567890");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("ada@example.com");
        assertThat(verifier.verify(token(googleKey, "accounts.google.com", CLIENT_ID, 300))).isNotNull();
    }

    @Test
    void rejectsForgedExpiredOrForeignTokens() throws Exception {
        assertThatThrownBy(() -> verifier.verify(token(otherKey, "https://accounts.google.com", CLIENT_ID, 300)))
                .isInstanceOf(BadCredentialsException.class);                            // not signed by Google
        assertThatThrownBy(() -> verifier.verify(token(googleKey, "https://accounts.google.com", "another-app", 300)))
                .isInstanceOf(BadCredentialsException.class);                            // issued for another app
        assertThatThrownBy(() -> verifier.verify(token(googleKey, "https://evil.example.com", CLIENT_ID, 300)))
                .isInstanceOf(BadCredentialsException.class);                            // wrong issuer
        assertThatThrownBy(() -> verifier.verify(token(googleKey, "https://accounts.google.com", CLIENT_ID, -3600)))
                .isInstanceOf(BadCredentialsException.class);                            // expired
        assertThatThrownBy(() -> verifier.verify("not-a-token")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void knowsWhetherItIsConfigured() {
        assertThat(verifier.isConfigured()).isTrue();
        assertThat(new GoogleIdTokenVerifier("not-configured", "http://unused").isConfigured()).isFalse();
    }
}
