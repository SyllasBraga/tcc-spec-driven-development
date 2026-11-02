package com.example.blog.perfil.web;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.blog.perfil.service.Perfil;
import com.example.blog.perfil.service.PerfilNaoEncontradoException;
import com.example.blog.perfil.service.PerfilService;
import com.example.blog.shared.erro.TratadorGlobalDeErros;
import com.example.blog.shared.seguranca.ConduitTokenResolver;
import com.example.blog.shared.seguranca.ErroSegurancaHandler;
import com.example.blog.shared.seguranca.SegredoJwtDeTesteConfiguration;
import com.example.blog.shared.seguranca.SegurancaConfig;
import com.example.blog.shared.seguranca.TokenService;

@WebMvcTest(PerfilController.class)
@Import({ SegurancaConfig.class, ErroSegurancaHandler.class, ConduitTokenResolver.class, TratadorGlobalDeErros.class,
		TokenService.class, SegredoJwtDeTesteConfiguration.class })
class PerfilControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PerfilService perfilService;

	@Test
	void consultaAnonimaDeveResponder200ComCamposNulos() throws Exception {
		when(perfilService.consultar("jake", null)).thenReturn(new Perfil("jake", null, null, false));

		mockMvc.perform(get("/api/profiles/jake"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.profile.username").value("jake"))
			.andExpect(jsonPath("$.profile", hasKey("bio")))
			.andExpect(jsonPath("$.profile", hasKey("image")))
			.andExpect(jsonPath("$.profile.bio").value(nullValue()))
			.andExpect(jsonPath("$.profile.following").value(false));
	}

	@Test
	void consultaComPrefixoBearerDeveResponder401() throws Exception {
		mockMvc.perform(get("/api/profiles/jake").header(HttpHeaders.AUTHORIZATION, "Bearer x"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
		verifyNoInteractions(perfilService);
	}

	@Test
	void perfilInexistenteDeveResponder404() throws Exception {
		when(perfilService.consultar("ninguem", null)).thenThrow(new PerfilNaoEncontradoException());

		mockMvc.perform(get("/api/profiles/ninguem"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errors.profile[0]").value("not found"));
	}

	@Test
	void seguirComTokenDeveResponder200() throws Exception {
		when(perfilService.seguir("jake", 1L)).thenReturn(new Perfil("jake", null, null, true));

		mockMvc.perform(post("/api/profiles/jake/follow").with(jwt().jwt(token -> token.subject("1"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.profile.following").value(true));
	}

	@Test
	void seguirSemTokenDeveResponder401() throws Exception {
		mockMvc.perform(post("/api/profiles/jake/follow"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.errors.token[0]").value("is missing"));
		verifyNoInteractions(perfilService);
	}

	@Test
	void deixarDeSeguirComTokenDeveResponder200() throws Exception {
		when(perfilService.deixarDeSeguir("jake", 1L)).thenReturn(new Perfil("jake", null, null, false));

		mockMvc.perform(delete("/api/profiles/jake/follow").with(jwt().jwt(token -> token.subject("1"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.profile.following").value(false));
	}

}
