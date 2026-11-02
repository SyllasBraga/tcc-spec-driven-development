package com.example.blog.perfil;

import static com.example.blog.ApiDeTeste.autorizacao;
import static com.example.blog.ApiDeTeste.usernameUnico;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.blog.ApiDeTeste;
import com.example.blog.TesteDeIntegracao;

/**
 * US3 ponta a ponta: perfis públicos e seguir/deixar de seguir.
 */
@TesteDeIntegracao
class PerfilApiIT {

    @Autowired
    private MockMvc mockMvc;

    private String ana;
    private String tokenAna;
    private String bia;

    @BeforeEach
    void criarUsuarios() throws Exception {
        ApiDeTeste api = new ApiDeTeste(mockMvc);
        ana = usernameUnico("ana");
        tokenAna = api.registrarETomarToken(ana);
        bia = usernameUnico("bia");
        api.registrarETomarToken(bia);
    }

    @Test
    void deveConsultarPerfilSemTokenComFollowingFalso() throws Exception {
        mockMvc.perform(get("/api/profiles/" + bia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value(bia))
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void deveSeguirEDeixarDeSeguirDeFormaIdempotente() throws Exception {
        seguir(bia, tokenAna).andExpect(status().isOk()).andExpect(jsonPath("$.profile.following").value(true));
        seguir(bia, tokenAna).andExpect(status().isOk()).andExpect(jsonPath("$.profile.following").value(true));

        mockMvc.perform(get("/api/profiles/" + bia).header(HttpHeaders.AUTHORIZATION, autorizacao(tokenAna)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(true));

        deixarDeSeguir(bia, tokenAna)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(false));
        deixarDeSeguir(bia, tokenAna)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(false));

        mockMvc.perform(get("/api/profiles/" + bia).header(HttpHeaders.AUTHORIZATION, autorizacao(tokenAna)))
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void deveRecusarSeguirASiMesmo() throws Exception {
        seguir(ana, tokenAna)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.profile[0]").value("cannot follow yourself"));
    }

    @Test
    void deveResponder404ParaPerfilInexistente() throws Exception {
        String inexistente = usernameUnico("ninguem");

        mockMvc.perform(get("/api/profiles/" + inexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.profile[0]").value("not found"));
        seguir(inexistente, tokenAna).andExpect(status().isNotFound());
        deixarDeSeguir(inexistente, tokenAna).andExpect(status().isNotFound());
    }

    @Test
    void deveRecusarSeguirSemToken() throws Exception {
        mockMvc.perform(post("/api/profiles/" + bia + "/follow"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }

    @Test
    void deveRecusarConsultaComTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/profiles/" + bia).header(HttpHeaders.AUTHORIZATION, autorizacao("abc")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    private ResultActions seguir(String username, String token) throws Exception {
        return mockMvc.perform(post("/api/profiles/" + username + "/follow")
                .header(HttpHeaders.AUTHORIZATION, autorizacao(token)));
    }

    private ResultActions deixarDeSeguir(String username, String token) throws Exception {
        return mockMvc.perform(delete("/api/profiles/" + username + "/follow")
                .header(HttpHeaders.AUTHORIZATION, autorizacao(token)));
    }
}
