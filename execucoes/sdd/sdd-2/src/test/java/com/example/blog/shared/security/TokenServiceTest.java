package com.example.blog.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class TokenServiceTest {

    private static final Instant AGORA = Instant.parse("2026-01-10T12:00:00Z");
    private static final JwtProperties PROPRIEDADES = new JwtProperties(
            "segredo-ficticio-de-teste-unitario-32+", "conduit-blog", Duration.ofHours(24));

    private final JwtConfig config = new JwtConfig();
    private final SecretKey chave = config.jwtSecretKey(PROPRIEDADES);
    private final TokenService tokenService =
            new TokenService(config.jwtEncoder(chave), PROPRIEDADES, Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void deveEmitirTokenAceitoPeloDecoderComSubjectEEmissor() {
        String token = tokenService.emitir(42L);

        Jwt jwt = decoderEm(AGORA.plusSeconds(60), chave).decode(token);

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("conduit-blog");
        assertThat(jwt.getExpiresAt()).isEqualTo(AGORA.plus(Duration.ofHours(24)));
    }

    @Test
    void deveRejeitarTokenExpirado() {
        String token = tokenService.emitir(42L);

        JwtDecoder decoder = decoderEm(AGORA.plus(Duration.ofHours(25)), chave);

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenAssinadoComOutroSegredo() {
        String token = tokenService.emitir(42L);
        JwtProperties outras = new JwtProperties(
                "outro-segredo-ficticio-totalmente-diferente", "conduit-blog", Duration.ofHours(24));

        JwtDecoder decoder = decoderEm(AGORA, config.jwtSecretKey(outras));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenDeOutroEmissor() {
        JwtProperties outroEmissor = new JwtProperties(PROPRIEDADES.secret(), "outro-emissor", Duration.ofHours(24));
        String token = new TokenService(config.jwtEncoder(chave), outroEmissor, Clock.fixed(AGORA, ZoneOffset.UTC))
                .emitir(42L);

        JwtDecoder decoder = decoderEm(AGORA, chave);

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private JwtDecoder decoderEm(Instant instante, SecretKey chaveDoDecoder) {
        return config.jwtDecoder(chaveDoDecoder, PROPRIEDADES, Clock.fixed(instante, ZoneOffset.UTC));
    }
}
