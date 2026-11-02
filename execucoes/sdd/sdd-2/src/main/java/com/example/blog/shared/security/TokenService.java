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
 * Emite os tokens JWT (HS256) que identificam o usuário nas requisições {@code Authorization: Token <jwt>}.
 */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Emite um token para o usuário.
     *
     * @param userId id do usuário, gravado no claim {@code sub}
     * @return o JWT assinado e serializado, válido por {@code app.jwt.ttl} a partir de agora
     */
    public String emitir(Long userId) {
        Objects.requireNonNull(userId, "userId");
        Instant emitidoEm = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .issuer(properties.issuer())
                .issuedAt(emitidoEm)
                .expiresAt(emitidoEm.plus(properties.ttl()))
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
    }
}
