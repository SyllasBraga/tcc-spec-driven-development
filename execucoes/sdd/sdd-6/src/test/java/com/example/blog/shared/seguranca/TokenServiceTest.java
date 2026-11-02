package com.example.blog.shared.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class TokenServiceTest {

	private static final String EMISSOR = "conduit";

	private static final Duration VALIDADE = Duration.ofHours(24);

	private final SegurancaConfig config = new SegurancaConfig();

	private final JwtProperties propriedades =
			new JwtProperties(SegredoJwtDeTesteConfiguration.gerarSegredo(), EMISSOR, VALIDADE);

	private final SecretKey chave = config.chaveJwt(propriedades);

	private final JwtDecoder decoder = config.jwtDecoder(chave, propriedades);

	@Test
	void deveEmitirTokenComSubIssEExpiracao() {
		Instant agora = Instant.now();
		TokenService service = servicoEm(agora);

		Jwt jwt = decoder.decode(service.emitir(42L));

		assertThat(jwt.getSubject()).isEqualTo("42");
		assertThat(jwt.getClaimAsString("iss")).isEqualTo(EMISSOR);
		assertThat(jwt.getIssuedAt()).isEqualTo(agora.truncatedTo(ChronoUnit.SECONDS));
		assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plus(VALIDADE));
		assertThat(service.usuarioId(jwt)).isEqualTo(42L);
	}

	@Test
	void deveRejeitarTokenExpirado() {
		TokenService service = servicoEm(Instant.now().minus(Duration.ofDays(2)));

		String token = service.emitir(7L);

		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
	}

	private TokenService servicoEm(Instant instante) {
		Clock clock = Clock.fixed(instante, ZoneOffset.UTC);
		return new TokenService(config.jwtEncoder(chave), propriedades, clock);
	}

}
