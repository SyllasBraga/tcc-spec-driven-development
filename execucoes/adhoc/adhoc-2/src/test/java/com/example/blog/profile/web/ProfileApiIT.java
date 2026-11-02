package com.example.blog.profile.web;

import static com.example.blog.TestUsers.authorization;
import static com.example.blog.TestUsers.register;
import static com.example.blog.TestUsers.uniqueUsername;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.blog.ApiIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@ApiIntegrationTest
class ProfileApiIT {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveRetornarPerfilParaVisitanteAnonimo() throws Exception {
        String username = uniqueUsername();
        register(mockMvc, username);

        mockMvc.perform(get("/api/profiles/{username}", username))
                .andExpect(status().isOk())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.profile.username").value(username))
                .andExpect(jsonPath("$.profile.bio").value(nullValue()))
                .andExpect(jsonPath("$.profile.image").value(nullValue()))
                .andExpect(jsonPath("$.profile.following").value(false))
                .andExpect(jsonPath("$.profile.email").doesNotExist());
    }

    @Test
    void deveRetornar404QuandoPerfilNaoExiste() throws Exception {
        mockMvc.perform(get("/api/profiles/{username}", uniqueUsername()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.errors.profile[0]").value("not found"));
    }

    @Test
    void deveRetornar401AoConsultarPerfilComTokenInvalido() throws Exception {
        String username = uniqueUsername();
        register(mockMvc, username);

        mockMvc.perform(get("/api/profiles/{username}", username)
                        .header(HttpHeaders.AUTHORIZATION, "Token token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    @Test
    void deveSeguirEDeixarDeSeguirUsuario() throws Exception {
        String followedUsername = uniqueUsername();
        register(mockMvc, followedUsername);
        String followerToken = register(mockMvc, uniqueUsername());

        mockMvc.perform(post("/api/profiles/{username}/follow", followedUsername)
                        .header(HttpHeaders.AUTHORIZATION, authorization(followerToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.profile.username").value(followedUsername))
                .andExpect(jsonPath("$.profile.following").value(true));

        mockMvc.perform(get("/api/profiles/{username}", followedUsername)
                        .header(HttpHeaders.AUTHORIZATION, authorization(followerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(true));

        mockMvc.perform(delete("/api/profiles/{username}/follow", followedUsername)
                        .header(HttpHeaders.AUTHORIZATION, authorization(followerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(false));

        mockMvc.perform(get("/api/profiles/{username}", followedUsername)
                        .header(HttpHeaders.AUTHORIZATION, authorization(followerToken)))
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void deveSerIdempotenteAoSeguirDuasVezes() throws Exception {
        String followedUsername = uniqueUsername();
        register(mockMvc, followedUsername);
        String followerToken = register(mockMvc, uniqueUsername());

        mockMvc.perform(post("/api/profiles/{username}/follow", followedUsername)
                        .header(HttpHeaders.AUTHORIZATION, authorization(followerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/profiles/{username}/follow", followedUsername)
                        .header(HttpHeaders.AUTHORIZATION, authorization(followerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(true));
    }

    @Test
    void deveSerIdempotenteAoDeixarDeSeguirQuemNaoESeguido() throws Exception {
        String followedUsername = uniqueUsername();
        register(mockMvc, followedUsername);
        String followerToken = register(mockMvc, uniqueUsername());

        mockMvc.perform(delete("/api/profiles/{username}/follow", followedUsername)
                        .header(HttpHeaders.AUTHORIZATION, authorization(followerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void deveManterFollowingFalsoParaVisitanteAnonimoMesmoComSeguidores() throws Exception {
        String followedUsername = uniqueUsername();
        register(mockMvc, followedUsername);
        String followerToken = register(mockMvc, uniqueUsername());
        mockMvc.perform(post("/api/profiles/{username}/follow", followedUsername)
                        .header(HttpHeaders.AUTHORIZATION, authorization(followerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/profiles/{username}", followedUsername))
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void deveRetornar401AoSeguirSemToken() throws Exception {
        String username = uniqueUsername();
        register(mockMvc, username);

        mockMvc.perform(post("/api/profiles/{username}/follow", username))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }

    @Test
    void deveRetornar401AoDeixarDeSeguirSemToken() throws Exception {
        String username = uniqueUsername();
        register(mockMvc, username);

        mockMvc.perform(delete("/api/profiles/{username}/follow", username))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }

    @Test
    void deveRetornar404AoSeguirUsuarioInexistente() throws Exception {
        String token = register(mockMvc, uniqueUsername());

        mockMvc.perform(post("/api/profiles/{username}/follow", uniqueUsername())
                        .header(HttpHeaders.AUTHORIZATION, authorization(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.profile[0]").value("not found"));
    }

    @Test
    void deveRetornar422AoTentarSeguirASiMesmo() throws Exception {
        String username = uniqueUsername();
        String token = register(mockMvc, username);

        mockMvc.perform(post("/api/profiles/{username}/follow", username)
                        .header(HttpHeaders.AUTHORIZATION, authorization(token)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.username[0]").value("cannot follow yourself"));
    }
}
