package com.example.blog.usuario;

import static com.example.blog.ClienteApiDeTeste.corpoLogin;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.blog.ClienteApiDeTeste;
import com.example.blog.ClienteApiDeTeste.Conta;
import com.example.blog.TestcontainersConfiguration;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UsuarioAtualIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	private Conta conta;

	@BeforeEach
	void cadastrarConta() throws Exception {
		conta = new ClienteApiDeTeste(mockMvc, jsonMapper).cadastrar("atual");
	}

	@Test
	void deveConsultarUsuarioAtual() throws Exception {
		mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, conta.autorizacao()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.user.email").value(conta.email()))
			.andExpect(jsonPath("$.user.username").value(conta.username()))
			.andExpect(jsonPath("$.user.token").isNotEmpty());
	}

	@Test
	void deveAtualizarBioEImageMantendoOsDemaisCampos() throws Exception {
		mockMvc.perform(atualizar("""
				{"user":{"bio":"I like to skateboard","image":"https://i.stack.imgur.com/xHWG8.jpg"}}
				"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.user.bio").value("I like to skateboard"))
			.andExpect(jsonPath("$.user.image").value("https://i.stack.imgur.com/xHWG8.jpg"))
			.andExpect(jsonPath("$.user.email").value(conta.email()))
			.andExpect(jsonPath("$.user.username").value(conta.username()));
	}

	@Test
	void deveLimparBioComNulo() throws Exception {
		mockMvc.perform(atualizar("{\"user\":{\"bio\":\"temporaria\"}}")).andExpect(status().isOk());

		mockMvc.perform(atualizar("{\"user\":{\"bio\":null}}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.user.bio").value(nullValue()));
	}

	@Test
	void deveTrocarASenha() throws Exception {
		mockMvc.perform(atualizar("{\"user\":{\"password\":\"nova-senha-ficticia\"}}")).andExpect(status().isOk());

		mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
			.content(corpoLogin(conta.email(), "nova-senha-ficticia")))
			.andExpect(status().isOk());
		mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
			.content(corpoLogin(conta.email(), conta.senha())))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void deveRecusarEmailDeOutroUsuario() throws Exception {
		Conta outra = new ClienteApiDeTeste(mockMvc, jsonMapper).cadastrar("outra");

		mockMvc.perform(atualizar("{\"user\":{\"email\":\"" + outra.email() + "\"}}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errors.email[0]").value("has already been taken"));
	}

	@Test
	void deveRecusarEmailSemPontoNoDominio() throws Exception {
		mockMvc.perform(atualizar("{\"user\":{\"email\":\"a@b\"}}"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errors.email[0]").value("is invalid"));
	}

	@Test
	void deveRecusarObjetoVazio() throws Exception {
		mockMvc.perform(atualizar("{\"user\":{}}"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errors.body[0]").value("at least one field is required"));
	}

	private MockHttpServletRequestBuilder atualizar(String corpo) {
		return put("/api/user").header(HttpHeaders.AUTHORIZATION, conta.autorizacao())
			.contentType(MediaType.APPLICATION_JSON)
			.content(corpo);
	}

}
