package com.example.blog.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class TokenServiceTest {

    private static final String SECRET = "unit-test-secret-key-with-at-least-32-bytes";

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    void issuesTokenWithSubjectIssuerAndExpiration() {
        JwtProperties properties = new JwtProperties(SECRET, "conduit", Duration.ofHours(1));
        Instant now = Instant.now();
        TokenService tokenService = new TokenService(jwtConfig.jwtEncoder(properties), properties,
                Clock.fixed(now, ZoneOffset.UTC));

        Jwt jwt = jwtConfig.jwtDecoder(properties).decode(tokenService.issueFor(42));

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("conduit");
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofHours(1)).truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        JwtProperties issuerA = new JwtProperties(SECRET, "another", Duration.ofHours(1));
        JwtProperties issuerB = new JwtProperties(SECRET, "conduit", Duration.ofHours(1));
        String token = new TokenService(jwtConfig.jwtEncoder(issuerA), issuerA, Clock.systemUTC()).issueFor(1);

        assertThatThrownBy(() -> jwtConfig.jwtDecoder(issuerB).decode(token))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtProperties properties = new JwtProperties(SECRET, "conduit", Duration.ofMinutes(1));
        Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        String token = new TokenService(jwtConfig.jwtEncoder(properties), properties, twoHoursAgo).issueFor(1);

        assertThatThrownBy(() -> jwtConfig.jwtDecoder(properties).decode(token))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtProperties("short", "conduit", Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
