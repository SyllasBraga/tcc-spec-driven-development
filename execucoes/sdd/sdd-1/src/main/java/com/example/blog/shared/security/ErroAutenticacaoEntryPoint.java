package com.example.blog.shared.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Responde {@code 401} no formato de erro do contrato: token ausente ({@code is missing}) ou
 * inválido, expirado ou com assinatura errada ({@code is invalid}).
 */
public class ErroAutenticacaoEntryPoint implements AuthenticationEntryPoint {

    static final String CORPO_TOKEN_AUSENTE = "{\"errors\":{\"token\":[\"is missing\"]}}";
    static final String CORPO_TOKEN_INVALIDO = "{\"errors\":{\"token\":[\"is invalid\"]}}";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        String corpo = authException instanceof OAuth2AuthenticationException
                ? CORPO_TOKEN_INVALIDO
                : CORPO_TOKEN_AUSENTE;
        RespostaJson.escrever(response, HttpServletResponse.SC_UNAUTHORIZED, corpo);
    }

    /**
     * Escrita comum de respostas JSON dos handlers de segurança, fora do fluxo do Spring MVC.
     */
    static final class RespostaJson {

        private RespostaJson() {
        }

        static void escrever(HttpServletResponse response, int status, String corpo) throws IOException {
            response.setStatus(status);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(corpo);
        }
    }
}
