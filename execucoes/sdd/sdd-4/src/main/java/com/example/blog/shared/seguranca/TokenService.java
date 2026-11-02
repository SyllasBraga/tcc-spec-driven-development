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

import com.example.blog.shared.erro.TokenInvalidoException;

/**
 * Emite e interpreta os tokens JWT da API. O token identifica o usuário apenas pelo id (claim {@code sub}),
 * de modo que continua válido após troca de e-mail ou de nome de usuário.
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
     * Emite um token HS256 para o usuário.
     *
     * @param idUsuario id do usuário, gravado na claim {@code sub}
     * @return o token compactado, válido por {@code conduit.jwt.validade} a partir de agora
     */
    public String emitir(long idUsuario) {
        Instant agora = Instant.now(clock);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(Long.toString(idUsuario))
                .issuer(propriedades.emissor())
                .issuedAt(agora)
                .expiresAt(agora.plus(propriedades.validade()))
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
    }

    /**
     * Extrai o id do usuário de um token já validado.
     *
     * @throws TokenInvalidoException se a claim {@code sub} não for um id numérico
     */
    public long idDoUsuario(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException excecao) {
            throw new TokenInvalidoException();
        }
    }
}
