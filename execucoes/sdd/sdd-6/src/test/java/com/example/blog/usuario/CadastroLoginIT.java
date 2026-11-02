package com.example.blog.usuario;

import static com.example.blog.ClienteApiDeTeste.corpoCadastro;
import static com.example.blog.ClienteApiDeTeste.corpoLogin;
import static com.example.blog.ClienteApiDeTeste.sufixoUnico;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.blog.ClienteApiDeTeste;
import com.example.blog.ClienteApiDeTeste.Conta;
import com.example.blog.TestcontainersConfiguration;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CadastroLoginIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Test
	void deveCadastrarFazerLoginEUsarOToken() throws Exception {
		String sufixo = sufixoUnico();
		String username = "jake-" + sufixo;
		String email = "jake." + sufixo + "@example.com";

		mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
			.content(corpoCadastro(username, email, "jakejake")))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("charset=UTF-8")))
			.andExpect(jsonPath("$.user.email").value(email))
			.andExpect(jsonPath("$.user.username").value(username))
			.andExpect(jsonPath("$.user.token").isNotEmpty())
			.andExpect(jsonPath("$.user.bio").value(nullValue()))
			.andExpect(jsonPath("$.user.image").value(nullValue()))
			.andExpect(content().string(not(containsString("password"))))
			.andExpect(content().string(not(containsString("senha"))));

		String resposta = mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
			.content(corpoLogin(email, "jakejake")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.user.email").value(email))
			.andExpect(content().string(not(containsString("password"))))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String token = jsonMapper.readTree(resposta).get("user").get("token").stringValue();

		mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Token " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.user.username").value(username));
	}

	@Test
	void deveRecusarEmailRepetidoComOutraCaixa() throws Exception {
		Conta existente = novoCliente().cadastrar("caixa");

		mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
			.content(corpoCadastro("outro-" + sufixoUnico(), existente.email().toUpperCase(), "jakejake")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errors.email[0]").value("has already been taken"));
	}

	@Test
	void deveRecusarUsernameRepetido() throws Exception {
		Conta existente = novoCliente().cadastrar("repetido");

		mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
			.content(corpoCadastro(existente.username(), "novo." + sufixoUnico() + "@example.com", "jakejake")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errors.username[0]").value("has already been taken"));
	}

	@Test
	void deveRecusarSenhaErrada() throws Exception {
		Conta existente = novoCliente().cadastrar("senha");

		mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
			.content(corpoLogin(existente.email(), "senha-errada")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errors.credentials[0]").value("invalid"));
	}

	@Test
	void deveRecusarEmailInexistenteNoLogin() throws Exception {
		mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
			.content(corpoLogin("ninguem." + sufixoUnico() + "@example.com", "qualquer")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errors.credentials[0]").value("invalid"));
	}

	private ClienteApiDeTeste novoCliente() {
		return new ClienteApiDeTeste(mockMvc, jsonMapper);
	}

}
