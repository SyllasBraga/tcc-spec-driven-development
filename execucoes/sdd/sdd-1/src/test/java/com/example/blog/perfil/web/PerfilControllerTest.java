package com.example.blog.perfil.web;

import static org.hamcrest.Matchers.hasKey;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.blog.perfil.service.Perfil;
import com.example.blog.perfil.service.PerfilService;
import com.example.blog.shared.config.ClockConfig;
import com.example.blog.shared.security.SecurityConfig;
import com.example.blog.shared.security.TokenService;
import com.example.blog.shared.web.RecursoNaoEncontradoException;
import com.example.blog.shared.web.ValidacaoException;

@WebMvcTest(PerfilController.class)
@Import({SecurityConfig.class, ClockConfig.class, TokenService.class})
@ActiveProfiles("test")
class PerfilControllerTest {

    private static final Long ID_ANA = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private PerfilService perfilService;

    @Test
    void deveConsultarPerfilAnonimamente() throws Exception {
        when(perfilService.consultar("bia")).thenReturn(new Perfil("bia", null, null, false));

        mockMvc.perform(get("/api/profiles/bia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("bia"))
                .andExpect(jsonPath("$.profile", hasKey("bio")))
                .andExpect(jsonPath("$.profile", hasKey("image")))
                .andExpect(jsonPath("$.profile.following").value(false));
        verify(perfilService).consultar("bia");
    }

    @Test
    void deveConsultarPerfilComUsuarioAutenticado() throws Exception {
        when(perfilService.consultar("bia", ID_ANA)).thenReturn(new Perfil("bia", null, null, true));

        mockMvc.perform(get("/api/profiles/bia").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(true));
    }

    @Test
    void deveResponder401ComTokenInvalidoNaConsulta() throws Exception {
        mockMvc.perform(get("/api/profiles/bia").header(HttpHeaders.AUTHORIZATION, "Token abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
        verifyNoInteractions(perfilService);
    }

    @Test
    void deveResponder401IsMissingAoSeguirSemToken() throws Exception {
        mockMvc.perform(post("/api/profiles/bia/follow"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }

    @Test
    void deveSeguir() throws Exception {
        when(perfilService.seguir(ID_ANA, "bia")).thenReturn(new Perfil("bia", null, null, true));

        mockMvc.perform(post("/api/profiles/bia/follow").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(true));
    }

    @Test
    void deveDeixarDeSeguir() throws Exception {
        when(perfilService.deixarDeSeguir(ID_ANA, "bia")).thenReturn(new Perfil("bia", null, null, false));

        mockMvc.perform(delete("/api/profiles/bia/follow").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void deveResponder404ParaPerfilInexistente() throws Exception {
        when(perfilService.consultar("ninguem")).thenThrow(new RecursoNaoEncontradoException("profile"));

        mockMvc.perform(get("/api/profiles/ninguem"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.profile[0]").value("not found"));
    }

    @Test
    void deveResponder422AoSeguirASiMesmo() throws Exception {
        when(perfilService.seguir(ID_ANA, "ana"))
                .thenThrow(new ValidacaoException("profile", "cannot follow yourself"));

        mockMvc.perform(post("/api/profiles/ana/follow").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.profile[0]").value("cannot follow yourself"));
    }

    private String token() {
        return "Token " + tokenService.emitir(ID_ANA);
    }
}
