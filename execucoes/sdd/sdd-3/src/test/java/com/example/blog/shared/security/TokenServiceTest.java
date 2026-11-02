package com.example.blog.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

class TokenServiceTest {

    private static final String SEGREDO = "dGVzdGUtc29tZW50ZS1sb2NhbC1uYW8tdXNhci0zMmI=";
    private static final String EMISSOR = "conduit-blog-test";
    private static final Instant AGORA = Instant.parse("2026-01-10T12:00:00Z");

    private final JwtProperties properties = new JwtProperties(SEGREDO, EMISSOR, Duration.ofHours(24));
    private final TokenService tokenService = new TokenService(
            new NimbusJwtEncoder(new ImmutableSecret<>(properties.chave())), properties,
            Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void deveEmitirTokenComSubjectEmissorEExpiracao() {
        String token = tokenService.emitir(42L);

        NimbusJwtDecoder decoder = decoderComRelogio(Clock.fixed(AGORA.plusSeconds(60), ZoneOffset.UTC));
        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(EMISSOR);
        assertThat(jwt.getIssuedAt()).isEqualTo(AGORA);
        assertThat(jwt.getExpiresAt()).isEqualTo(AGORA.plus(Duration.ofHours(24)));
    }

    @Test
    void deveRecusarTokenExpirado() {
        String token = tokenService.emitir(42L);

        NimbusJwtDecoder decoder = decoderComRelogio(Clock.fixed(AGORA.plus(Duration.ofHours(25)), ZoneOffset.UTC));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void deveRecusarTokenDeOutroEmissorPeloValidadorPadrao() {
        String token = tokenService.emitir(42L);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(properties.chave())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("outro-emissor"));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    private NimbusJwtDecoder decoderComRelogio(Clock relogio) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(properties.chave())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator validadorDeTempo = new JwtTimestampValidator(Duration.ZERO);
        validadorDeTempo.setClock(relogio);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validadorDeTempo, new JwtIssuerValidator(EMISSOR)));
        return decoder;
    }
}
