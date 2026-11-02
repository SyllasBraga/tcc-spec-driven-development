package com.example.blog.shared.seguranca;

import com.example.blog.shared.erro.ErrosResponse;
import com.example.blog.shared.erro.MensagensErro;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Escreve as respostas 401 e 403 da camada de segurança no formato de erro do contrato.
 */
@Component
public class ErroAutenticacaoHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public ErroAutenticacaoHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest requisicao, HttpServletResponse resposta,
            AuthenticationException excecao) throws IOException {
        String mensagem = excecao instanceof OAuth2AuthenticationException
                ? MensagensErro.INVALIDO
                : MensagensErro.AUSENTE;
        escrever(resposta, HttpServletResponse.SC_UNAUTHORIZED, ErrosResponse.de(MensagensErro.CHAVE_TOKEN, mensagem));
    }

    @Override
    public void handle(HttpServletRequest requisicao, HttpServletResponse resposta,
            AccessDeniedException excecao) throws IOException {
        escrever(resposta, HttpServletResponse.SC_FORBIDDEN,
                ErrosResponse.de(MensagensErro.CHAVE_AUTORIZACAO, MensagensErro.PROIBIDO));
    }

    private void escrever(HttpServletResponse resposta, int status, ErrosResponse corpo) throws IOException {
        resposta.setStatus(status);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(resposta.getOutputStream(), corpo);
    }
}
