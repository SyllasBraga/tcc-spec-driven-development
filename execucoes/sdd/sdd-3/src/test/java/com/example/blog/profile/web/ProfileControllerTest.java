package com.example.blog.profile.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import com.example.blog.profile.service.Perfil;
import com.example.blog.profile.service.PerfilNaoEncontradoException;
import com.example.blog.profile.service.ProfileService;
import com.example.blog.profile.service.SeguirASiMesmoException;
import com.example.blog.shared.error.GlobalExceptionHandler;
import com.example.blog.shared.security.ErrorAccessDeniedHandler;
import com.example.blog.shared.security.ErrorAuthenticationEntryPoint;
import com.example.blog.shared.security.JwtProperties;
import com.example.blog.shared.security.SecurityConfig;
import com.example.blog.shared.security.UsuarioAtual;
import com.example.blog.shared.time.ClockConfig;

@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ClockConfig.class, ErrorAuthenticationEntryPoint.class,
        ErrorAccessDeniedHandler.class})
@EnableConfigurationProperties(JwtProperties.class)
class ProfileControllerTest {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private UsuarioAtual usuarioAtual;

    // ---------- GET /api/profiles/{username} ----------

    @Test
    void deveBuscarPerfilAnonimo() throws Exception {
        when(usuarioAtual.idOpcional()).thenReturn(Optional.empty());
        when(profileService.buscar("jake", Optional.empty())).thenReturn(new Perfil("jake", null, null, false));

        mockMvc.perform(get("/api/profiles/jake"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"profile":{"username":"jake","bio":null,"image":null,"following":false}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveResponder404QuandoPerfilNaoExiste() throws Exception {
        when(usuarioAtual.idOpcional()).thenReturn(Optional.empty());
        when(profileService.buscar("ninguem", Optional.empty()))
                .thenThrow(new PerfilNaoEncontradoException("ninguem"));

        mockMvc.perform(get("/api/profiles/ninguem"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"errors":{"profile":["not found"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveResponder401ComTokenInvalidoEmRotaPublica() throws Exception {
        mockMvc.perform(get("/api/profiles/jake").header("Authorization", "Token invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"errors":{"token":["is invalid"]}}
                        """, JsonCompareMode.STRICT));
    }

    // ---------- POST / DELETE /api/profiles/{username}/follow ----------

    @Test
    void deveResponder401AoSeguirSemCabecalho() throws Exception {
        mockMvc.perform(post("/api/profiles/jake/follow"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("""
                        {"errors":{"token":["is missing"]}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveSeguir() throws Exception {
        when(usuarioAtual.idObrigatorio()).thenReturn(1L);
        when(profileService.seguir(1L, "jake")).thenReturn(new Perfil("jake", "bio", "img", true));

        mockMvc.perform(post("/api/profiles/jake/follow").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(content().json("""
                        {"profile":{"username":"jake","bio":"bio","image":"img","following":true}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveDeixarDeSeguir() throws Exception {
        when(usuarioAtual.idObrigatorio()).thenReturn(1L);
        when(profileService.deixarDeSeguir(1L, "jake")).thenReturn(new Perfil("jake", null, null, false));

        mockMvc.perform(delete("/api/profiles/jake/follow").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"profile":{"username":"jake","bio":null,"image":null,"following":false}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveResponder422AoSeguirASiMesmo() throws Exception {
        when(usuarioAtual.idObrigatorio()).thenReturn(1L);
        when(profileService.seguir(anyLong(), eq("jake"))).thenThrow(new SeguirASiMesmoException());

        mockMvc.perform(post("/api/profiles/jake/follow").with(jwt()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("""
                        {"errors":{"profile":["cannot follow yourself"]}}
                        """, JsonCompareMode.STRICT));
    }
}
