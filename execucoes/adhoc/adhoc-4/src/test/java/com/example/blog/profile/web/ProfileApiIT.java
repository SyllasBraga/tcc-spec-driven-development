package com.example.blog.profile.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.json.JsonCompareMode;

import com.example.blog.AbstractIntegrationTest;

class ProfileApiIT extends AbstractIntegrationTest {

    @Test
    void deveRetornarPerfilSemAutenticacaoComFollowingFalso() throws Exception {
        registerUser("celeb", "celeb@exemplo.test", "senha-celeb");

        mockMvc.perform(get("/api/profiles/celeb"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8"))
                .andExpect(content().json("""
                        {"profile": {"username": "celeb", "bio": null, "image": null, "following": false}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveRetornar404ParaPerfilInexistente() throws Exception {
        mockMvc.perform(get("/api/profiles/ninguem"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.profile[0]").value("not found"));
    }

    @Test
    void deveRetornar401AoConsultarPerfilComTokenInvalido() throws Exception {
        registerUser("celeb", "celeb@exemplo.test", "senha-celeb");

        mockMvc.perform(get("/api/profiles/celeb").header(HttpHeaders.AUTHORIZATION, "Token invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveSeguirEDeixarDeSeguirUsuario() throws Exception {
        registerUser("celeb", "celeb@exemplo.test", "senha-celeb");
        String fanToken = registerUser("fan", "fan@exemplo.test", "senha-fan");

        mockMvc.perform(post("/api/profiles/celeb/follow").header(HttpHeaders.AUTHORIZATION, tokenHeader(fanToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("celeb"))
                .andExpect(jsonPath("$.profile.following").value(true));
        mockMvc.perform(get("/api/profiles/celeb").header(HttpHeaders.AUTHORIZATION, tokenHeader(fanToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(true));
        mockMvc.perform(get("/api/profiles/celeb"))
                .andExpect(jsonPath("$.profile.following").value(false));

        mockMvc.perform(delete("/api/profiles/celeb/follow").header(HttpHeaders.AUTHORIZATION, tokenHeader(fanToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(false));
        mockMvc.perform(get("/api/profiles/celeb").header(HttpHeaders.AUTHORIZATION, tokenHeader(fanToken)))
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void deveSerIdempotenteAoSeguirDuasVezes() throws Exception {
        registerUser("celeb", "celeb@exemplo.test", "senha-celeb");
        String fanToken = registerUser("fan", "fan@exemplo.test", "senha-fan");

        mockMvc.perform(post("/api/profiles/celeb/follow").header(HttpHeaders.AUTHORIZATION, tokenHeader(fanToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/profiles/celeb/follow").header(HttpHeaders.AUTHORIZATION, tokenHeader(fanToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(true));
    }

    @Test
    void deveRetornar401AoSeguirSemToken() throws Exception {
        registerUser("celeb", "celeb@exemplo.test", "senha-celeb");

        mockMvc.perform(post("/api/profiles/celeb/follow"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
        mockMvc.perform(delete("/api/profiles/celeb/follow"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar404AoSeguirUsuarioInexistente() throws Exception {
        String fanToken = registerUser("fan", "fan@exemplo.test", "senha-fan");

        mockMvc.perform(post("/api/profiles/ninguem/follow").header(HttpHeaders.AUTHORIZATION, tokenHeader(fanToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.profile[0]").value("not found"));
    }

    @Test
    void deveRetornar422AoSeguirASiMesmo() throws Exception {
        String fanToken = registerUser("fan", "fan@exemplo.test", "senha-fan");

        mockMvc.perform(post("/api/profiles/fan/follow").header(HttpHeaders.AUTHORIZATION, tokenHeader(fanToken)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.profile[0]").value("can't follow yourself"));
    }
}
