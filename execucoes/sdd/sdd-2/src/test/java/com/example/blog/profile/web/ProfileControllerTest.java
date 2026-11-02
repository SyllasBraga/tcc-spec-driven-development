package com.example.blog.profile.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.blog.profile.service.Profile;
import com.example.blog.profile.service.ProfileService;
import com.example.blog.shared.config.ClockConfig;
import com.example.blog.shared.error.GlobalExceptionHandler;
import com.example.blog.shared.error.NotFoundException;
import com.example.blog.shared.security.CurrentUserId;
import com.example.blog.shared.security.JwtConfig;
import com.example.blog.shared.security.SecurityConfig;
import com.example.blog.shared.security.TokenAuthenticationEntryPoint;
import com.example.blog.shared.security.TokenAuthorizationHeaderResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, JwtConfig.class, ClockConfig.class, TokenAuthorizationHeaderResolver.class,
        TokenAuthenticationEntryPoint.class, CurrentUserId.class, GlobalExceptionHandler.class})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @Test
    void deveResponder200ComPerfilAnonimo() throws Exception {
        when(profileService.buscarPerfil("jake", null)).thenReturn(new Profile("jake", null, null, false));

        mockMvc.perform(get("/api/profiles/jake"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"profile":{"username":"jake","bio":null,"image":null,"following":false}}""", STRICT));
    }

    @Test
    void deveResponder404QuandoPerfilNaoExiste() throws Exception {
        when(profileService.buscarPerfil("ninguem", null)).thenThrow(new NotFoundException("profile"));

        mockMvc.perform(get("/api/profiles/ninguem"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"errors":{"profile":["not found"]}}""", STRICT));
    }

    @Test
    void deveResponder200AoSeguirComJwt() throws Exception {
        when(profileService.seguir("jake", 3L)).thenReturn(new Profile("jake", "bio", null, true));

        mockMvc.perform(post("/api/profiles/jake/follow").with(jwt().jwt(token -> token.subject("3"))))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"profile":{"username":"jake","bio":"bio","image":null,"following":true}}""", STRICT));
    }

    @Test
    void deveResponder200AoDeixarDeSeguirComJwt() throws Exception {
        when(profileService.deixarDeSeguir("jake", 3L)).thenReturn(new Profile("jake", null, null, false));

        mockMvc.perform(delete("/api/profiles/jake/follow").with(jwt().jwt(token -> token.subject("3"))))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"profile":{"username":"jake","bio":null,"image":null,"following":false}}""", STRICT));
    }

    @Test
    void deveResponder401AoSeguirSemToken() throws Exception {
        mockMvc.perform(post("/api/profiles/jake/follow"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("""
                        {"errors":{"token":["is missing"]}}""", STRICT));
    }

    @Test
    void deveResponder401AoDeixarDeSeguirSemToken() throws Exception {
        mockMvc.perform(delete("/api/profiles/jake/follow"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("""
                        {"errors":{"token":["is missing"]}}""", STRICT));
    }
}
