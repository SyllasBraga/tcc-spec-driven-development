package com.example.blog.perfil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

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
import com.example.blog.perfil.persistence.Seguimento;
import com.example.blog.perfil.persistence.SeguimentoId;
import com.example.blog.perfil.persistence.SeguimentoRepository;
import com.example.blog.usuario.persistence.UsuarioRepository;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PerfilIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private SeguimentoRepository seguimentoRepository;

	@Test
	void consultaAnonimaDeveResponderFollowingFalso() throws Exception {
		Conta alvo = cliente().cadastrar("perfil");

		mockMvc.perform(get("/api/profiles/" + alvo.username()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.profile.username").value(alvo.username()))
			.andExpect(jsonPath("$.profile.following").value(false));
	}

	@Test
	void consultaDeQuemSegueDeveResponderFollowingVerdadeiro() throws Exception {
		Conta alvo = cliente().cadastrar("seguido");
		Conta leitor = cliente().cadastrar("leitor");
		seguimentoRepository.save(new Seguimento(new SeguimentoId(idDe(leitor), idDe(alvo)), Instant.now()));

		mockMvc.perform(get("/api/profiles/" + alvo.username()).header(HttpHeaders.AUTHORIZATION, leitor.autorizacao()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.profile.following").value(true));
	}

	@Test
	void usernameInexistenteDeveResponder404() throws Exception {
		mockMvc.perform(get("/api/profiles/ninguem-" + ClienteApiDeTeste.sufixoUnico()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errors.profile[0]").value("not found"));
	}

	@Test
	void tokenInvalidoDeveResponder401() throws Exception {
		Conta alvo = cliente().cadastrar("alvo");

		mockMvc.perform(get("/api/profiles/" + alvo.username()).header(HttpHeaders.AUTHORIZATION, "Token invalido"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
	}

	private Long idDe(Conta conta) {
		return usuarioRepository.findByUsername(conta.username()).orElseThrow().getId();
	}

	private ClienteApiDeTeste cliente() {
		return new ClienteApiDeTeste(mockMvc, jsonMapper);
	}

}
