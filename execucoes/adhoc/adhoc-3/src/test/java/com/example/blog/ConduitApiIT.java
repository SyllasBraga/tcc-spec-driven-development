package com.example.blog;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Testes ponta a ponta do contrato Conduit (recorte A), com PostgreSQL em Testcontainers.
 * Subir o contexto já aplica todas as migrations em banco vazio e valida o mapeamento JPA.
 * Cada teste cria os próprios usuários, com nomes únicos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ConduitApiIT {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final String PASSWORD = "senha-ficticia";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void deveCadastrarUsuarioERetornarTokenUtilizavel() throws Exception {
        String username = uniqueUsername();

        String response = mockMvc.perform(json(post("/api/users"), """
                        {"user":{"username":"%s","email":"%s@example.test","password":"%s"}}
                        """.formatted(username, username, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/user"))
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.email").value(username + "@example.test"))
                .andExpect(jsonPath("$.user.bio").isEmpty())
                .andExpect(jsonPath("$.user.image").isEmpty())
                .andExpect(jsonPath("$.user.token").isString())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.user.token");

        mockMvc.perform(get("/api/user").header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.token").value(token));
    }

    @Test
    void deveRetornar422ComErrosPorCampoNoCadastroInvalido() throws Exception {
        mockMvc.perform(json(post("/api/users"), """
                        {"user":{"username":" ","email":"nao-e-email"}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.username[0]").value("can't be blank"))
                .andExpect(jsonPath("$.errors.email[0]").value("is invalid"))
                .andExpect(jsonPath("$.errors.password[0]").value("can't be blank"));
    }

    @Test
    void deveRetornar422QuandoFaltaObjetoUser() throws Exception {
        mockMvc.perform(json(post("/api/users"), "{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.user[0]").value("can't be blank"));
    }

    @Test
    void deveRetornar422ParaJsonMalformado() throws Exception {
        mockMvc.perform(json(post("/api/users"), "{nao json"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.body[0]").value("is invalid"));
    }

    @Test
    void deveRetornar409ParaUsernameOuEmailRepetido() throws Exception {
        String username = register();

        mockMvc.perform(json(post("/api/users"), """
                        {"user":{"username":"%s","email":"outro-%s@example.test","password":"%s"}}
                        """.formatted(username, username, PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.username[0]").value("has already been taken"));

        mockMvc.perform(json(post("/api/users"), """
                        {"user":{"username":"outro-%s","email":"%s@EXAMPLE.test","password":"%s"}}
                        """.formatted(username, username.toUpperCase(), PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.email[0]").value("has already been taken"));
    }

    @Test
    void deveFazerLoginERecusarSenhaErrada() throws Exception {
        String username = register();

        mockMvc.perform(json(post("/api/users/login"), """
                        {"user":{"email":"%s@example.test","password":"%s"}}
                        """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.token").isString());

        mockMvc.perform(json(post("/api/users/login"), """
                        {"user":{"email":"%s@example.test","password":"errada"}}
                        """.formatted(username)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.credentials[0]").value("invalid"));
    }

    @Test
    void deveRetornar422NoLoginSemCampos() throws Exception {
        mockMvc.perform(json(post("/api/users/login"), """
                        {"user":{}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("can't be blank"))
                .andExpect(jsonPath("$.errors.password[0]").value("can't be blank"));
    }

    @Test
    void deveExigirTokenNoUsuarioAtual() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));

        mockMvc.perform(get("/api/user").header("Authorization", "Token nao.e.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    @Test
    void deveRecusarTokenExpiradoOuDeOutroEmissor() throws Exception {
        String username = register();
        String token = login(username);
        String subject = subjectOf(token);
        Instant past = Instant.parse("2020-01-01T00:00:00Z");

        String expired = encode(JwtClaimsSet.builder().issuer("conduit").subject(subject)
                .issuedAt(past).expiresAt(past.plusSeconds(60)).build());
        String otherIssuer = encode(JwtClaimsSet.builder().issuer("outro").subject(subject)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build());

        mockMvc.perform(get("/api/user").header("Authorization", "Token " + expired))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/user").header("Authorization", "Token " + otherIssuer))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveAtualizarSomenteCamposInformados() throws Exception {
        String username = register();
        String token = login(username);
        String newUsername = uniqueUsername();

        mockMvc.perform(json(put("/api/user"), token, """
                        {"user":{"username":"%s","bio":"Gosto de skate","image":"https://img.example.test/a.png"}}
                        """.formatted(newUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value(newUsername))
                .andExpect(jsonPath("$.user.email").value(username + "@example.test"))
                .andExpect(jsonPath("$.user.bio").value("Gosto de skate"))
                .andExpect(jsonPath("$.user.image").value("https://img.example.test/a.png"))
                .andExpect(jsonPath("$.user.token").value(token));

        mockMvc.perform(json(put("/api/user"), token, """
                        {"user":{"image":null,"password":"nova-senha"}}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio").value("Gosto de skate"))
                .andExpect(jsonPath("$.user.image").isEmpty());

        mockMvc.perform(json(post("/api/users/login"), """
                        {"user":{"email":"%s@example.test","password":"nova-senha"}}
                        """.formatted(username)))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar422NaAtualizacaoInvalida() throws Exception {
        String token = login(register());

        mockMvc.perform(json(put("/api/user"), token, """
                        {"user":{}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.user[0]").value("must have at least one field"));

        mockMvc.perform(json(put("/api/user"), token, """
                        {"user":{"email":"","username":"  "}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email").isArray())
                .andExpect(jsonPath("$.errors.username[0]").value("can't be blank"));

        mockMvc.perform(json(put("/api/user"), token, """
                        {"user":{"email":null}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("can't be blank"));

        mockMvc.perform(json(put("/api/user"), token, """
                        {"user":{"username":123}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.body[0]").value("is invalid"));

        mockMvc.perform(json(put("/api/user"), token, """
                        {"user":{"bio":true}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.body[0]").value("is invalid"));
    }

    @Test
    void deveGravarBioEImagemVaziasComoNulas() throws Exception {
        String token = login(register());

        mockMvc.perform(json(put("/api/user"), token, """
                        {"user":{"bio":"","image":""}}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio").value(nullValue()))
                .andExpect(jsonPath("$.user.image").value(nullValue()));

        mockMvc.perform(get("/api/user").header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio").value(nullValue()))
                .andExpect(jsonPath("$.user.image").value(nullValue()));
    }

    @Test
    void deveExigirTokenNaAtualizacao() throws Exception {
        mockMvc.perform(json(put("/api/user"), """
                        {"user":{"bio":"x"}}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveConsultarPerfilComOuSemAutenticacao() throws Exception {
        String target = register();
        String viewerToken = login(register());

        mockMvc.perform(get("/api/profiles/{username}", target))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.profile.username").value(target))
                .andExpect(jsonPath("$.profile.bio").isEmpty())
                .andExpect(jsonPath("$.profile.image").isEmpty())
                .andExpect(jsonPath("$.profile.following").value(false));

        mockMvc.perform(get("/api/profiles/{username}", target).header("Authorization", "Token " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void deveRetornar404ParaPerfilInexistente() throws Exception {
        String token = login(register());

        mockMvc.perform(get("/api/profiles/{username}", "inexistente-" + uniqueUsername()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.profile[0]").value("not found"));
        mockMvc.perform(post("/api/profiles/{username}/follow", "inexistente-" + uniqueUsername())
                        .header("Authorization", "Token " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/profiles/{username}/follow", "inexistente-" + uniqueUsername())
                        .header("Authorization", "Token " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveSeguirEDeixarDeSeguir() throws Exception {
        String target = register();
        String token = login(register());

        mockMvc.perform(post("/api/profiles/{username}/follow", target).header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value(target))
                .andExpect(jsonPath("$.profile.following").value(true));
        // Seguir de novo é idempotente.
        mockMvc.perform(post("/api/profiles/{username}/follow", target).header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(true));
        mockMvc.perform(get("/api/profiles/{username}", target).header("Authorization", "Token " + token))
                .andExpect(jsonPath("$.profile.following").value(true));

        mockMvc.perform(delete("/api/profiles/{username}/follow", target).header("Authorization", "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(false));
        mockMvc.perform(get("/api/profiles/{username}", target).header("Authorization", "Token " + token))
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void deveExigirTokenParaSeguir() throws Exception {
        String target = register();

        mockMvc.perform(post("/api/profiles/{username}/follow", target))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
        mockMvc.perform(delete("/api/profiles/{username}/follow", target))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRecusarSeguirASiMesmo() throws Exception {
        String username = register();
        String token = login(username);

        mockMvc.perform(post("/api/profiles/{username}/follow", username).header("Authorization", "Token " + token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.profile[0]").value("cannot follow yourself"));
    }

    private String register() throws Exception {
        String username = uniqueUsername();
        mockMvc.perform(json(post("/api/users"), """
                        {"user":{"username":"%s","email":"%s@example.test","password":"%s"}}
                        """.formatted(username, username, PASSWORD)))
                .andExpect(status().isCreated());
        return username;
    }

    private String login(String username) throws Exception {
        String response = mockMvc.perform(json(post("/api/users/login"), """
                        {"user":{"email":"%s@example.test","password":"%s"}}
                        """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.user.token");
    }

    private static String subjectOf(String token) {
        // Reaproveita o subject de um token válido para montar tokens inválidos do mesmo usuário.
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        return JsonPath.read(payload, "$.sub");
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static String uniqueUsername() {
        return "user" + System.nanoTime() + "x" + SEQUENCE.incrementAndGet();
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String token,
            String body) {
        return json(builder, body).header("Authorization", "Token " + token);
    }
}
