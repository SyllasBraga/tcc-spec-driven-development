package com.example.blog.perfil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.example.blog.ClienteApiDeTeste;
import com.example.blog.ClienteApiDeTeste.Conta;
import com.example.blog.TestcontainersConfiguration;
import com.example.blog.perfil.persistence.SeguimentoId;
import com.example.blog.perfil.persistence.SeguimentoRepository;
import com.example.blog.usuario.persistence.UsuarioRepository;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SeguirIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private SeguimentoRepository seguimentoRepository;

	private Conta a;

	private Conta b;

	@BeforeEach
	void cadastrarContas() throws Exception {
		ClienteApiDeTeste cliente = new ClienteApiDeTeste(mockMvc, jsonMapper);
		a = cliente.cadastrar("seguidor");
		b = cliente.cadastrar("seguido");
	}

	@Test
	void deveSeguirEDeixarDeSeguirDeFormaIdempotente() throws Exception {
		String rota = "/api/profiles/" + b.username() + "/follow";

		mockMvc.perform(post(rota).header(HttpHeaders.AUTHORIZATION, a.autorizacao()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.profile.username").value(b.username()))
			.andExpect(jsonPath("$.profile.following").value(true));
		mockMvc.perform(post(rota).header(HttpHeaders.AUTHORIZATION, a.autorizacao()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.profile.following").value(true));
		assertThat(seguimentoRepository.existsById(relacao())).isTrue();

		mockMvc.perform(get("/api/profiles/" + b.username()).header(HttpHeaders.AUTHORIZATION, a.autorizacao()))
			.andExpect(jsonPath("$.profile.following").value(true));

		mockMvc.perform(delete(rota).header(HttpHeaders.AUTHORIZATION, a.autorizacao()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.profile.following").value(false));
		mockMvc.perform(delete(rota).header(HttpHeaders.AUTHORIZATION, a.autorizacao()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.profile.following").value(false));
		assertThat(seguimentoRepository.existsById(relacao())).isFalse();
	}

	@Test
	void seguirSemTokenDeveResponder401() throws Exception {
		mockMvc.perform(post("/api/profiles/" + b.username() + "/follow"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errors.token[0]").value("is missing"));
	}

	@Test
	void deixarDeSeguirSemTokenDeveResponder401() throws Exception {
		mockMvc.perform(delete("/api/profiles/" + b.username() + "/follow"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errors.token[0]").value("is missing"));
	}

	@Test
	void seguirAlvoInexistenteDeveResponder404() throws Exception {
		mockMvc.perform(post("/api/profiles/ninguem-" + ClienteApiDeTeste.sufixoUnico() + "/follow")
			.header(HttpHeaders.AUTHORIZATION, a.autorizacao()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errors.profile[0]").value("not found"));
	}

	@Test
	void deixarDeSeguirAlvoInexistenteDeveResponder404() throws Exception {
		mockMvc.perform(delete("/api/profiles/ninguem-" + ClienteApiDeTeste.sufixoUnico() + "/follow")
			.header(HttpHeaders.AUTHORIZATION, a.autorizacao()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errors.profile[0]").value("not found"));
	}

	private SeguimentoId relacao() {
		return new SeguimentoId(idDe(a), idDe(b));
	}

	private Long idDe(Conta conta) {
		return usuarioRepository.findByUsername(conta.username()).orElseThrow().getId();
	}

}
