package com.example.blog.shared.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite tokens JWT assinados com HS256, com o id do usuário em {@code sub}.
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
     * @param usuarioId id do usuário, gravado em {@code sub}
     * @return token JWT compacto
     */
    public String emitir(Long usuarioId) {
        Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        Instant agora = Instant.now(clock);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuarioId.toString())
                .issuer(propriedades.issuer())
                .issuedAt(agora)
                .expiresAt(agora.plus(propriedades.validade()))
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
    }
}
