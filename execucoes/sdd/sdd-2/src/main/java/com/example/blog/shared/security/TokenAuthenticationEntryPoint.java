package com.example.blog.shared.security;

import com.example.blog.shared.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Responde {@code 401} no formato do contrato: {@code token: is invalid} quando um token foi enviado
 * e rejeitado (assinatura, formato, expiração, emissor), e {@code token: is missing} nos demais casos.
 */
@Component
public class TokenAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ErrorResponse TOKEN_INVALIDO = ErrorResponse.of("token", "is invalid");
    private static final ErrorResponse TOKEN_AUSENTE = ErrorResponse.of("token", "is missing");

    private final JsonMapper jsonMapper;

    public TokenAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        ErrorResponse corpo = authException instanceof OAuth2AuthenticationException ? TOKEN_INVALIDO : TOKEN_AUSENTE;
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), corpo);
    }
}
