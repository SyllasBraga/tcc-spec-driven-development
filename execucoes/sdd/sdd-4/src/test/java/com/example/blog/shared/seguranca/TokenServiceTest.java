package com.example.blog.shared.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import com.example.blog.shared.erro.TokenInvalidoException;

class TokenServiceTest {

    private static final String SEGREDO = "segredo-de-teste-ficticio-com-mais-de-32-bytes";
    private static final String OUTRO_SEGREDO = "outro-segredo-ficticio-com-mais-de-32-bytes!!";
    private static final Duration VALIDADE = Duration.ofHours(24);

    private final SegurancaConfig config = new SegurancaConfig();
    private final JwtProperties propriedades = new JwtProperties(SEGREDO, VALIDADE, "conduit");
    private final JwtDecoder decoder = config.jwtDecoder(propriedades);

    @Test
    void deveEmitirTokenQueODecoderAceita() {
        Instant agora = Instant.now();
        TokenService service = servicoCom(propriedades, Clock.fixed(agora, ZoneOffset.UTC));

        Jwt jwt = decoder.decode(service.emitir(42L));

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getIssuedAt()).isEqualTo(agora.truncatedTo(ChronoUnit.SECONDS));
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(VALIDADE);
        assertThat(service.idDoUsuario(jwt)).isEqualTo(42L);
    }

    @Test
    void deveRejeitarTokenExpirado() {
        Clock passado = Clock.fixed(Instant.now().minus(Duration.ofDays(2)), ZoneOffset.UTC);
        String token = servicoCom(propriedades, passado).emitir(1L);

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void deveRejeitarTokenAssinadoComOutroSegredo() {
        JwtProperties outras = new JwtProperties(OUTRO_SEGREDO, VALIDADE, "conduit");
        String token = servicoCom(outras, Clock.systemUTC()).emitir(1L);

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void deveRejeitarTokenDeOutroEmissor() {
        JwtProperties outras = new JwtProperties(SEGREDO, VALIDADE, "outro");
        String token = servicoCom(outras, Clock.systemUTC()).emitir(1L);

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void deveRecusarSegredoCurto() {
        assertThatThrownBy(() -> new JwtProperties("curto", VALIDADE, "conduit"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveLancarTokenInvalidoQuandoSubjectNaoENumerico() {
        TokenService service = servicoCom(propriedades, Clock.systemUTC());
        Jwt jwt = Jwt.withTokenValue("x").header("alg", "HS256").subject("abc").build();

        assertThatThrownBy(() -> service.idDoUsuario(jwt)).isInstanceOf(TokenInvalidoException.class);
    }

    private TokenService servicoCom(JwtProperties props, Clock clock) {
        return new TokenService(config.jwtEncoder(props), props, clock);
    }
}
