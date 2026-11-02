package com.example.blog.shared.seguranca;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.example.blog.shared.erro.ErrosResponse;

import tools.jackson.databind.json.JsonMapper;

/**
 * Responde às falhas de segurança no formato de erro do contrato.
 * <ul>
 * <li>{@code 401} {@code token: ["is missing"]} quando a rota exige autenticação e não há credencial;</li>
 * <li>{@code 401} {@code token: ["is invalid"]} quando o token é malformado, expirado, com assinatura ou emissor
 * inválidos, ou com prefixo diferente de {@code Token};</li>
 * <li>{@code 403} {@code authorization: ["forbidden"]} quando o acesso é negado.</li>
 * </ul>
 */
@Component
public class ErroSegurancaHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private static final String CHAVE_TOKEN = "token";

	private final JsonMapper jsonMapper;

	public ErroSegurancaHandler(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException excecao) throws IOException {
		String mensagem = isFalhaDeToken(excecao) ? "is invalid" : "is missing";
		escrever(response, HttpStatus.UNAUTHORIZED, ErrosResponse.de(CHAVE_TOKEN, mensagem));
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException excecao)
			throws IOException {
		escrever(response, HttpStatus.FORBIDDEN, ErrosResponse.de("authorization", "forbidden"));
	}

	private static boolean isFalhaDeToken(Throwable excecao) {
		for (Throwable atual = excecao; atual != null; atual = atual.getCause()) {
			if (atual instanceof OAuth2AuthenticationException) {
				return true;
			}
		}
		return false;
	}

	private void escrever(HttpServletResponse response, HttpStatus status, ErrosResponse corpo) throws IOException {
		response.setStatus(status.value());
		response.setCharacterEncoding("UTF-8");
		response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
		jsonMapper.writeValue(response.getOutputStream(), corpo);
	}

}
