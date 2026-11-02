package com.example.blog.perfil.web;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.blog.perfil.service.Perfil;
import com.example.blog.perfil.service.PerfilService;
import com.example.blog.shared.erro.RecursoNaoEncontradoException;
import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.shared.seguranca.SegredoJwtAleatorioConfiguration;
import com.example.blog.shared.seguranca.SegurancaConfig;
import com.example.blog.shared.seguranca.TokenService;

@WebMvcTest(PerfilController.class)
@Import({SegurancaConfig.class, TokenService.class, SegredoJwtAleatorioConfiguration.class})
class PerfilControllerTest {

    private static final long ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private PerfilService perfilService;

    // US3 — consulta

    @Test
    void deveDevolverPerfilParaAnonimoComNulosSerializados() throws Exception {
        when(perfilService.buscar("ana")).thenReturn(new Perfil("ana", null, null, false));

        mockMvc.perform(get("/api/profiles/ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("ana"))
                .andExpect(jsonPath("$.profile.bio").value((Object) null))
                .andExpect(jsonPath("$.profile.image").value((Object) null))
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void deveDevolverPerfilParaUsuarioAutenticado() throws Exception {
        when(perfilService.buscarComo(ID, "beto")).thenReturn(new Perfil("beto", "bio", null, true));

        mockMvc.perform(get("/api/profiles/beto").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(true));
    }

    @Test
    void deveResponder401ComTokenInvalidoMesmoComAutenticacaoOpcional() throws Exception {
        mockMvc.perform(get("/api/profiles/ana").header(HttpHeaders.AUTHORIZATION, "Token invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    @Test
    void deveResponder404QuandoPerfilNaoExiste() throws Exception {
        when(perfilService.buscar("ninguem")).thenThrow(new RecursoNaoEncontradoException("profile", "not found"));

        mockMvc.perform(get("/api/profiles/ninguem"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.profile[0]").value("not found"));
    }

    // US4 — seguir

    @Test
    void deveResponder401AoSeguirSemToken() throws Exception {
        mockMvc.perform(post("/api/profiles/beto/follow"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }

    @Test
    void deveResponder401AoDeixarDeSeguirSemToken() throws Exception {
        mockMvc.perform(delete("/api/profiles/beto/follow"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }

    @Test
    void deveSeguir() throws Exception {
        when(perfilService.seguir(ID, "beto")).thenReturn(new Perfil("beto", null, null, true));

        mockMvc.perform(post("/api/profiles/beto/follow").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(true));
    }

    @Test
    void deveDeixarDeSeguir() throws Exception {
        when(perfilService.deixarDeSeguir(ID, "beto")).thenReturn(new Perfil("beto", null, null, false));

        mockMvc.perform(delete("/api/profiles/beto/follow").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void deveResponder422AoSeguirASiMesmo() throws Exception {
        when(perfilService.seguir(ID, "ana")).thenThrow(new ValidacaoException("profile", "cannot follow yourself"));

        mockMvc.perform(post("/api/profiles/ana/follow").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.profile[0]").value("cannot follow yourself"));
    }

    private String token() {
        return "Token " + tokenService.emitir(ID);
    }
}
