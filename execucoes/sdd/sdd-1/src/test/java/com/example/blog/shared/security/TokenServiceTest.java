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

    private static final String SEGREDO = "segredo-de-teste-ficticio-com-mais-de-32-bytes";
    private static final String OUTRO_SEGREDO = "outro-segredo-de-teste-ficticio-com-32-bytes";
    private static final String EMISSOR = "conduit-blog";
    private static final Duration VALIDADE = Duration.ofHours(24);
    private static final Instant AGORA = Instant.parse("2026-10-31T12:00:00Z");

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void deveEmitirTokenComSubjectEmissorEExpiracao() {
        TokenService tokenService = tokenService(SEGREDO, EMISSOR, relogio(AGORA));

        String token = tokenService.emitir(42L);

        Jwt jwt = decoder(SEGREDO, EMISSOR, relogio(AGORA.plusSeconds(60))).decode(token);
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(EMISSOR);
        assertThat(jwt.getIssuedAt()).isEqualTo(AGORA);
        assertThat(jwt.getExpiresAt()).isEqualTo(AGORA.plus(VALIDADE));
    }

    @Test
    void deveRejeitarTokenExpirado() {
        String token = tokenService(SEGREDO, EMISSOR, relogio(AGORA)).emitir(42L);
        JwtDecoder decoderDepoisDaValidade = decoder(SEGREDO, EMISSOR, relogio(AGORA.plus(VALIDADE).plusSeconds(1)));

        assertThatThrownBy(() -> decoderDepoisDaValidade.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenComAssinaturaDeOutraChave() {
        String token = tokenService(OUTRO_SEGREDO, EMISSOR, relogio(AGORA)).emitir(42L);
        JwtDecoder decoder = decoder(SEGREDO, EMISSOR, relogio(AGORA));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenDeOutroEmissor() {
        String token = tokenService(SEGREDO, "outro-emissor", relogio(AGORA)).emitir(42L);
        JwtDecoder decoder = decoder(SEGREDO, EMISSOR, relogio(AGORA));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private TokenService tokenService(String segredo, String emissor, Clock relogio) {
        JwtProperties propriedades = new JwtProperties(segredo, emissor, VALIDADE);
        SecretKey chave = config.chaveJwt(propriedades);
        return new TokenService(config.jwtEncoder(chave), propriedades, relogio);
    }

    private JwtDecoder decoder(String segredo, String emissor, Clock relogio) {
        JwtProperties propriedades = new JwtProperties(segredo, emissor, VALIDADE);
        return config.jwtDecoder(config.chaveJwt(propriedades), propriedades, relogio);
    }

    private static Clock relogio(Instant instante) {
        return Clock.fixed(instante, ZoneOffset.UTC);
    }
}
