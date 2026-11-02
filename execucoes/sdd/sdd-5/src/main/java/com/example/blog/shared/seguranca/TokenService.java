package com.example.blog.shared.seguranca;

import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite tokens JWT (HS256) que identificam o usuário pela claim {@code sub}.
 */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final TokenProperties propriedades;

    public TokenService(JwtEncoder jwtEncoder, Clock clock, TokenProperties propriedades) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.propriedades = propriedades;
    }

    /**
     * Emite um token novo para o usuário.
     *
     * @param usuarioId id do usuário, gravado em {@code sub}
     * @return token compacto, válido por {@link TokenProperties#validade()}
     */
    public String emitir(Long usuarioId) {
        Instant agora = Instant.now(clock);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(usuarioId))
                .issuer(propriedades.emissor())
                .issuedAt(agora)
                .expiresAt(agora.plus(propriedades.validade()))
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
    }
}
