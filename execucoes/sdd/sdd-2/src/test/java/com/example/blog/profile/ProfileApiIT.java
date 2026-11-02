package com.example.blog.profile;

import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.blog.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProfileApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    private record Conta(String username, String token) {
        String authorization() {
            return "Token " + token;
        }
    }

    private Conta a;
    private Conta b;

    @BeforeEach
    void cadastrarUsuarios() throws Exception {
        a = cadastrar();
        b = cadastrar();
    }

    private Conta cadastrar() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String username = "perfil-" + sufixo;
        MvcResult resultado = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"user":{"username":"%s","email":"%s@exemplo.test","password":"senha-ficticia"}}"""
                                .formatted(username, username)))
                .andExpect(status().isCreated())
                .andReturn();
        String token = jsonMapper.readTree(resultado.getResponse().getContentAsString())
                .get("user").get("token").stringValue();
        return new Conta(username, token);
    }

    private static String perfil(String username, boolean following) {
        return """
                {"profile":{"username":"%s","bio":null,"image":null,"following":%s}}""".formatted(username, following);
    }

    @Nested
    class Consulta {

        @Test
        void deveRetornarPerfilAnonimo() throws Exception {
            mockMvc.perform(get("/api/profiles/{username}", b.username()))
                    .andExpect(status().isOk())
                    .andExpect(content().json(perfil(b.username(), false), STRICT));
        }

        @Test
        void deveRetornarFollowingFalsoParaQuemNaoSegue() throws Exception {
            mockMvc.perform(get("/api/profiles/{username}", b.username()).header("Authorization", a.authorization()))
                    .andExpect(status().isOk())
                    .andExpect(content().json(perfil(b.username(), false), STRICT));
        }

        @Test
        void deveResponder401IsInvalidComTokenInvalido() throws Exception {
            mockMvc.perform(get("/api/profiles/{username}", b.username()).header("Authorization", "Token abc.def.ghi"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("""
                            {"errors":{"token":["is invalid"]}}""", STRICT));
        }

        @Test
        void deveTratarEsquemaBearerComoAnonimo() throws Exception {
            mockMvc.perform(post("/api/profiles/{username}/follow", b.username()).header("Authorization", a.authorization()))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/profiles/{username}", b.username()).header("Authorization", "Bearer " + a.token()))
                    .andExpect(status().isOk())
                    .andExpect(content().json(perfil(b.username(), false), STRICT));
        }

        @Test
        void deveResponder404ParaNomeInexistente() throws Exception {
            mockMvc.perform(get("/api/profiles/{username}", "ninguem-" + UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(content().json("""
                            {"errors":{"profile":["not found"]}}""", STRICT));
        }
    }

    @Nested
    class SeguirEDeixarDeSeguir {

        private ResultActions aSegueB() throws Exception {
            return mockMvc.perform(post("/api/profiles/{username}/follow", b.username())
                    .header("Authorization", a.authorization()));
        }

        private ResultActions aDeixaDeSeguirB() throws Exception {
            return mockMvc.perform(delete("/api/profiles/{username}/follow", b.username())
                    .header("Authorization", a.authorization()));
        }

        @Test
        void deveSeguirDeFormaIdempotente() throws Exception {
            aSegueB().andExpect(status().isOk())
                    .andExpect(content().json(perfil(b.username(), true), STRICT));
            aSegueB().andExpect(status().isOk())
                    .andExpect(content().json(perfil(b.username(), true), STRICT));

            mockMvc.perform(get("/api/profiles/{username}", b.username()).header("Authorization", a.authorization()))
                    .andExpect(status().isOk())
                    .andExpect(content().json(perfil(b.username(), true), STRICT));
        }

        @Test
        void deveDeixarDeSeguirDeFormaIdempotente() throws Exception {
            aSegueB().andExpect(status().isOk());

            aDeixaDeSeguirB().andExpect(status().isOk())
                    .andExpect(content().json(perfil(b.username(), false), STRICT));
            aDeixaDeSeguirB().andExpect(status().isOk())
                    .andExpect(content().json(perfil(b.username(), false), STRICT));

            mockMvc.perform(get("/api/profiles/{username}", b.username()).header("Authorization", a.authorization()))
                    .andExpect(content().json(perfil(b.username(), false), STRICT));
        }

        @Test
        void deveResponder422AoSeguirASiMesmo() throws Exception {
            mockMvc.perform(post("/api/profiles/{username}/follow", a.username())
                            .header("Authorization", a.authorization()))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(content().json("""
                            {"errors":{"profile":["cannot follow yourself"]}}""", STRICT));
        }

        @Test
        void deveResponder401IsMissingSemTokenAoSeguir() throws Exception {
            mockMvc.perform(post("/api/profiles/{username}/follow", b.username()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("""
                            {"errors":{"token":["is missing"]}}""", STRICT));
        }

        @Test
        void deveResponder401IsMissingSemTokenAoDeixarDeSeguir() throws Exception {
            mockMvc.perform(delete("/api/profiles/{username}/follow", b.username()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("""
                            {"errors":{"token":["is missing"]}}""", STRICT));
        }

        @Test
        void deveResponder404AoSeguirInexistente() throws Exception {
            mockMvc.perform(post("/api/profiles/{username}/follow", "inexistente-" + UUID.randomUUID())
                            .header("Authorization", a.authorization()))
                    .andExpect(status().isNotFound())
                    .andExpect(content().json("""
                            {"errors":{"profile":["not found"]}}""", STRICT));
        }
    }
}
