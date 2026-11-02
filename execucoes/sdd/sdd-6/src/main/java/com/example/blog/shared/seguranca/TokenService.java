package com.example.blog.shared.seguranca;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.example.blog.shared.erro.NaoAutenticadoException;

/**
 * Emite e interpreta os JWT da API (HS256, claims {@code sub}, {@code iss}, {@code iat} e {@code exp}).
 * <p>
 * A validação de assinatura, expiração e emissor fica a cargo do {@code JwtDecoder} do resource server.
 */
@Service
public class TokenService {

	private final JwtEncoder jwtEncoder;

	private final JwtProperties propriedades;

	private final Clock clock;

	public TokenService(JwtEncoder jwtEncoder, JwtProperties propriedades, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.propriedades = propriedades;
		this.clock = clock;
	}

	/**
	 * Emite um token para o usuário.
	 *
	 * @param usuarioId identificador do usuário, gravado na claim {@code sub}
	 * @return o JWT assinado, serializado
	 */
	public String emitir(Long usuarioId) {
		Instant agora = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(String.valueOf(usuarioId))
			.issuer(propriedades.issuer())
			.issuedAt(agora)
			.expiresAt(agora.plus(propriedades.ttl()))
			.build();
		JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
	}

	/**
	 * Lê o identificador do usuário da claim {@code sub} de um token já validado.
	 *
	 * @throws NaoAutenticadoException se {@code sub} estiver ausente ou não for numérico
	 */
	public Long usuarioId(Jwt jwt) {
		String sujeito = jwt.getSubject();
		if (sujeito == null) {
			throw NaoAutenticadoException.tokenInvalido();
		}
		try {
			return Long.valueOf(sujeito);
		}
		catch (NumberFormatException excecao) {
			throw NaoAutenticadoException.tokenInvalido();
		}
	}

}
