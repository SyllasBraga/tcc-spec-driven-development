package com.example.blog.shared.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class TokenServiceTest {

    private static final String SEGREDO = "segredo-ficticio-apenas-para-testes-0123456789ab";
    private static final SecretKey CHAVE = new SecretKeySpec(SEGREDO.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private final TokenProperties propriedades = new TokenProperties(SEGREDO, Duration.ofHours(24), "conduit");

    @Test
    void deveEmitirTokenComSubjectEmissorEValidadeDe24Horas() {
        Instant agora = Instant.now();
        TokenService servico = servicoCom(Clock.fixed(agora, ZoneOffset.UTC), propriedades);

        Jwt jwt = decoder("conduit").decode(servico.emitir(42L));

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("conduit");
        assertThat(jwt.getIssuedAt()).isEqualTo(agora.truncatedTo(ChronoUnit.SECONDS));
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(24));
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
    }

    @Test
    void deveRejeitarTokenExpirado() {
        Instant passado = Instant.now().minus(Duration.ofDays(3));
        String token = servicoCom(Clock.fixed(passado, ZoneOffset.UTC), propriedades).emitir(1L);

        assertThatThrownBy(() -> decoder("conduit").decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenDeOutroEmissor() {
        TokenProperties outroEmissor = new TokenProperties(SEGREDO, Duration.ofHours(24), "outro");
        String token = servicoCom(Clock.systemUTC(), outroEmissor).emitir(1L);

        assertThatThrownBy(() -> decoder("conduit").decode(token)).isInstanceOf(JwtException.class);
    }

    private static TokenService servicoCom(Clock clock, TokenProperties propriedades) {
        return new TokenService(NimbusJwtEncoder.withSecretKey(CHAVE).build(), clock, propriedades);
    }

    private static NimbusJwtDecoder decoder(String emissor) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(CHAVE).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(emissor));
        return decoder;
    }
}
