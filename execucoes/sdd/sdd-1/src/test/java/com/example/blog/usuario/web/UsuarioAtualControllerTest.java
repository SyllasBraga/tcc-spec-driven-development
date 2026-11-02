package com.example.blog.usuario.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.blog.shared.config.ClockConfig;
import com.example.blog.shared.security.SecurityConfig;
import com.example.blog.shared.security.TokenInvalidoException;
import com.example.blog.shared.security.TokenService;
import com.example.blog.usuario.service.AtualizacaoUsuario;
import com.example.blog.usuario.service.UsuarioAutenticado;
import com.example.blog.usuario.service.UsuarioService;

@WebMvcTest(UsuarioAtualController.class)
@Import({SecurityConfig.class, ClockConfig.class, TokenService.class, AtualizacaoUsuarioMapper.class})
@ActiveProfiles("test")
class UsuarioAtualControllerTest {

    private static final Long ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void deveResponder401IsMissingSemAuthorization() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }

    @Test
    void deveTratarEsquemaBearerComoTokenAusente() throws Exception {
        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.emitir(ID)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }

    @Test
    void deveResponder401IsInvalidComTokenMalformado() throws Exception {
        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Token abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    @Test
    void deveRetornarUsuarioAtualComOMesmoToken() throws Exception {
        String token = tokenService.emitir(ID);
        when(usuarioService.buscarAtual(ID, token))
                .thenReturn(new UsuarioAutenticado("jake@x.com", token, "jake", null, null));

        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.token").value(token))
                .andExpect(jsonPath("$.user.username").value("jake"));
    }

    @Test
    void deveResponder401QuandoUsuarioDoTokenNaoExiste() throws Exception {
        String token = tokenService.emitir(ID);
        when(usuarioService.buscarAtual(ID, token)).thenThrow(new TokenInvalidoException());

        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Token " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    @Test
    void deveAtualizarBio() throws Exception {
        String token = tokenService.emitir(ID);
        when(usuarioService.atualizar(eq(ID), eq(token), any(AtualizacaoUsuario.class)))
                .thenReturn(new UsuarioAutenticado("jake@x.com", token, "jake", "Olá", null));

        mockMvc.perform(put("/api/user").header(HttpHeaders.AUTHORIZATION, "Token " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"user\":{\"bio\":\"Olá\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio").value("Olá"));
    }

    @Test
    void deveRecusarAtualizacaoSemCampos() throws Exception {
        mockMvc.perform(put("/api/user").header(HttpHeaders.AUTHORIZATION, "Token " + tokenService.emitir(ID))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"user\":{}}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.user[0]").value("at least one field is required"));
    }

    @Test
    void deveRecusarAtualizacaoSemObjetoUser() throws Exception {
        mockMvc.perform(put("/api/user").header(HttpHeaders.AUTHORIZATION, "Token " + tokenService.emitir(ID))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.body").exists());
    }
}
