package com.example.blog.usuario.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.blog.shared.erro.TratadorGlobalDeErros;
import com.example.blog.shared.seguranca.ConduitTokenResolver;
import com.example.blog.shared.seguranca.ErroSegurancaHandler;
import com.example.blog.shared.seguranca.SegredoJwtDeTesteConfiguration;
import com.example.blog.shared.seguranca.SegurancaConfig;
import com.example.blog.shared.seguranca.TokenService;
import com.example.blog.usuario.service.UsuarioAutenticado;
import com.example.blog.usuario.service.UsuarioService;

@WebMvcTest(UsuarioController.class)
@Import({ SegurancaConfig.class, ErroSegurancaHandler.class, ConduitTokenResolver.class, TratadorGlobalDeErros.class,
		TokenService.class, AtualizacaoUsuarioMapper.class, SegredoJwtDeTesteConfiguration.class })
class UsuarioControllerTest {

	private static final UsuarioAutenticado JAKE =
			new UsuarioAutenticado("jake@jake.jake", "jwt.token.aqui", "jake", null, null);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UsuarioService usuarioService;

	@Test
	void cadastroValidoDeveResponder201ComUsuario() throws Exception {
		when(usuarioService.cadastrar("jake", "jake@jake.jake", "jakejake")).thenReturn(JAKE);

		mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"user":{"username":"jake","email":"jake@jake.jake","password":"jakejake"}}
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, "/api/user"))
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/json")))
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("charset=UTF-8")))
			.andExpect(jsonPath("$.user.email").value("jake@jake.jake"))
			.andExpect(jsonPath("$.user.token").value("jwt.token.aqui"))
			.andExpect(jsonPath("$.user.username").value("jake"))
			.andExpect(jsonPath("$.user", hasKey("bio")))
			.andExpect(jsonPath("$.user", hasKey("image")))
			.andExpect(jsonPath("$.user.bio").value(nullValue()))
			.andExpect(jsonPath("$.user", not(hasKey("password"))));
	}

	@Test
	void cadastroSemCamposDeveResponder422ComCadaCampo() throws Exception {
		mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{\"user\":{}}"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("charset=UTF-8")))
			.andExpect(jsonPath("$.errors.email[0]").value("can't be blank"))
			.andExpect(jsonPath("$.errors.username[0]").value("can't be blank"))
			.andExpect(jsonPath("$.errors.password[0]").value("can't be blank"));
		verifyNoInteractions(usuarioService);
	}

	@Test
	void cadastroComEmailInvalidoDeveResponder422() throws Exception {
		mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"user":{"username":"jake","email":"invalido","password":"jakejake"}}
					"""))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errors.email[0]").value("is invalid"));
	}

	@Test
	void jsonMalformadoDeveResponder422() throws Exception {
		mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{\"user\":"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errors.body[0]").value("is invalid"));
	}

	@Test
	void cadastroSemRaizUserDeveResponder422() throws Exception {
		mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errors.body[0]").value("is invalid"));
	}

	@Test
	void loginSemSenhaDeveResponder422() throws Exception {
		mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"user\":{\"email\":\"jake@jake.jake\"}}"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errors.password[0]").value("can't be blank"));
	}

	@Test
	void loginValidoDeveResponder200() throws Exception {
		when(usuarioService.autenticar("jake@jake.jake", "jakejake")).thenReturn(JAKE);

		mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"user\":{\"email\":\"jake@jake.jake\",\"password\":\"jakejake\"}}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.user.token").value("jwt.token.aqui"));
	}

	@Test
	void consultarUsuarioAtualComTokenDeveResponder200() throws Exception {
		when(usuarioService.consultarAtual(1L)).thenReturn(JAKE);

		mockMvc.perform(get("/api/user").with(jwt().jwt(token -> token.subject("1"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.user.username").value("jake"));
	}

	@Test
	void consultarUsuarioAtualSemTokenDeveResponder401() throws Exception {
		mockMvc.perform(get("/api/user"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("charset=UTF-8")))
			.andExpect(jsonPath("$.errors.token[0]").value("is missing"));
	}

	@Test
	void atualizarComObjetoVazioDeveResponder422() throws Exception {
		mockMvc.perform(put("/api/user").with(jwt().jwt(token -> token.subject("1")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"user\":{}}"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errors.body[0]").value("at least one field is required"));
		verifyNoInteractions(usuarioService);
	}

	@Test
	void atualizarSemRaizUserDeveResponder422() throws Exception {
		mockMvc.perform(put("/api/user").with(jwt().jwt(token -> token.subject("1")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.errors.body[0]").value("is invalid"));
	}

	@Test
	void atualizarComBioDeveResponder200() throws Exception {
		when(usuarioService.atualizar(eq(1L), any()))
			.thenReturn(new UsuarioAutenticado("jake@jake.jake", "novo", "jake", "nova bio", null));

		mockMvc.perform(put("/api/user").with(jwt().jwt(token -> token.subject("1")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"user\":{\"bio\":\"nova bio\"}}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.user.bio").value("nova bio"));
	}

}
