package com.example.blog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Apoio aos testes de integração: cadastra usuários fictícios únicos pela própria API.
 */
public final class ClienteApiDeTeste {

	public static final String SENHA_PADRAO = "senha-ficticia-123";

	private final MockMvc mockMvc;

	private final JsonMapper jsonMapper;

	public ClienteApiDeTeste(MockMvc mockMvc, JsonMapper jsonMapper) {
		this.mockMvc = mockMvc;
		this.jsonMapper = jsonMapper;
	}

	/** Dados de uma conta criada no teste. */
	public record Conta(String username, String email, String senha, String token) {

		public String autorizacao() {
			return "Token " + token;
		}

	}

	public static String sufixoUnico() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	public Conta cadastrar(String prefixo) throws Exception {
		String sufixo = sufixoUnico();
		String username = prefixo + "-" + sufixo;
		String email = prefixo + "." + sufixo + "@example.com";
		String resposta = mockMvc
			.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
				.content(corpoCadastro(username, email, SENHA_PADRAO)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		JsonNode user = jsonMapper.readTree(resposta).get("user");
		return new Conta(username, email, SENHA_PADRAO, user.get("token").stringValue());
	}

	public static String corpoCadastro(String username, String email, String senha) {
		return """
				{"user":{"username":"%s","email":"%s","password":"%s"}}
				""".formatted(username, email, senha);
	}

	public static String corpoLogin(String email, String senha) {
		return """
				{"user":{"email":"%s","password":"%s"}}
				""".formatted(email, senha);
	}

}
