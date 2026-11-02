package com.example.blog.user.web;

import static com.example.blog.TestUsers.PASSWORD;
import static com.example.blog.TestUsers.authorization;
import static com.example.blog.TestUsers.register;
import static com.example.blog.TestUsers.uniqueUsername;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.blog.ApiIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@ApiIntegrationTest
class UserApiIT {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveCadastrarUsuarioERetornar201ComToken() throws Exception {
        String username = uniqueUsername();

        postJson("/api/users", """
                {"user": {"username": "%s", "email": "%s@Example.TEST", "password": "%s"}}
                """.formatted(username, username, PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.email").value(username + "@example.test"))
                .andExpect(jsonPath("$.user.token").value(not(emptyString())))
                .andExpect(jsonPath("$.user.bio").value(nullValue()))
                .andExpect(jsonPath("$.user.image").value(nullValue()))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void deveRetornar409QuandoUsernameJaExiste() throws Exception {
        String username = uniqueUsername();
        register(mockMvc, username);

        postJson("/api/users", """
                {"user": {"username": "%s", "email": "outro-%s@example.test", "password": "%s"}}
                """.formatted(username, username, PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.username[0]").value("has already been taken"));
    }

    @Test
    void deveRetornar409QuandoEmailJaExisteIgnorandoMaiusculas() throws Exception {
        String username = uniqueUsername();
        register(mockMvc, username);

        postJson("/api/users", """
                {"user": {"username": "outro%s", "email": "%s@EXAMPLE.test", "password": "%s"}}
                """.formatted(username, username.toUpperCase(), PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.email[0]").value("has already been taken"));
    }

    @Test
    void deveRetornar422QuandoCamposObrigatoriosDoCadastroFaltam() throws Exception {
        postJson("/api/users", """
                {"user": {"username": "", "password": "  "}}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.errors.username[0]").value("can't be blank"))
                .andExpect(jsonPath("$.errors.email[0]").value("can't be blank"))
                .andExpect(jsonPath("$.errors.password[0]").value("can't be blank"));
    }

    @Test
    void deveRetornar422QuandoEmailDoCadastroEInvalido() throws Exception {
        postJson("/api/users", """
                {"user": {"username": "%s", "email": "sem-arroba", "password": "%s"}}
                """.formatted(uniqueUsername(), PASSWORD))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("is invalid"));
    }

    @Test
    void deveRetornar422QuandoCorpoNaoTemObjetoUser() throws Exception {
        postJson("/api/users", "{}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.user[0]").value("can't be blank"));
    }

    @Test
    void deveRetornar422QuandoJsonEMalformado() throws Exception {
        postJson("/api/users", "{\"user\": ")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.body[0]").value("is malformed"));
    }

    @Test
    void deveFazerLoginComCredenciaisValidas() throws Exception {
        String username = uniqueUsername();
        register(mockMvc, username);

        postJson("/api/users/login", """
                {"user": {"email": "%s@EXAMPLE.test", "password": "%s"}}
                """.formatted(username, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.token").value(not(emptyString())));
    }

    @Test
    void deveRetornar401QuandoSenhaDoLoginEstaIncorreta() throws Exception {
        String username = uniqueUsername();
        register(mockMvc, username);

        postJson("/api/users/login", """
                {"user": {"email": "%s@example.test", "password": "senha-errada"}}
                """.formatted(username))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.credentials[0]").value("invalid"));
    }

    @Test
    void deveRetornar401QuandoEmailDoLoginNaoExiste() throws Exception {
        postJson("/api/users/login", """
                {"user": {"email": "%s@example.test", "password": "%s"}}
                """.formatted(uniqueUsername(), PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.credentials[0]").value("invalid"));
    }

    @Test
    void deveRetornar422QuandoCamposDoLoginFaltam() throws Exception {
        postJson("/api/users/login", """
                {"user": {}}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("can't be blank"))
                .andExpect(jsonPath("$.errors.password[0]").value("can't be blank"));
    }

    @Test
    void deveRetornarUsuarioAtualComOMesmoToken() throws Exception {
        String username = uniqueUsername();
        String token = register(mockMvc, username);

        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, authorization(token)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.email").value(username + "@example.test"))
                .andExpect(jsonPath("$.user.token").value(token));
    }

    @Test
    void deveRetornar401QuandoTokenEstaAusente() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Token"))
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }

    @Test
    void deveRetornar401QuandoTokenEInvalido() throws Exception {
        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Token nao.e.um.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    @Test
    void deveRetornar401QuandoEsquemaDoCabecalhoNaoEToken() throws Exception {
        String token = register(mockMvc, uniqueUsername());

        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }

    @Test
    void deveAtualizarSomenteOsCamposInformados() throws Exception {
        String username = uniqueUsername();
        String token = register(mockMvc, username);

        putUser(token, """
                {"user": {"bio": "Gosto de andar de skate", "image": "https://example.test/avatar.png"}}
                """)
                .andExpect(status().isOk())
                .andExpect(content().contentType(JSON_UTF8))
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.email").value(username + "@example.test"))
                .andExpect(jsonPath("$.user.bio").value("Gosto de andar de skate"))
                .andExpect(jsonPath("$.user.image").value("https://example.test/avatar.png"))
                .andExpect(jsonPath("$.user.token").value(token));

        putUser(token, """
                {"user": {"bio": null}}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio").value(nullValue()))
                .andExpect(jsonPath("$.user.image").value("https://example.test/avatar.png"));
    }

    @Test
    void deveGravarBioEImageVaziasComoNulas() throws Exception {
        String token = register(mockMvc, uniqueUsername());

        putUser(token, """
                {"user": {"bio": "", "image": ""}}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio").value(nullValue()))
                .andExpect(jsonPath("$.user.image").value(nullValue()));

        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, authorization(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio").value(nullValue()))
                .andExpect(jsonPath("$.user.image").value(nullValue()));
    }

    @Test
    void deveAtualizarEmailUsernameESenhaEPermitirLoginComOsNovosDados() throws Exception {
        String token = register(mockMvc, uniqueUsername());
        String newUsername = uniqueUsername();

        putUser(token, """
                {"user": {"email": "%s@example.test", "username": "%s", "password": "nova-senha-ficticia"}}
                """.formatted(newUsername, newUsername))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value(newUsername))
                .andExpect(jsonPath("$.user.email").value(newUsername + "@example.test"));

        postJson("/api/users/login", """
                {"user": {"email": "%s@example.test", "password": "nova-senha-ficticia"}}
                """.formatted(newUsername))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value(newUsername));
    }

    @Test
    void deveRetornar409AoAtualizarParaUsernameDeOutroUsuario() throws Exception {
        String existingUsername = uniqueUsername();
        register(mockMvc, existingUsername);
        String token = register(mockMvc, uniqueUsername());

        putUser(token, """
                {"user": {"username": "%s"}}
                """.formatted(existingUsername))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.username[0]").value("has already been taken"));
    }

    @Test
    void deveRetornar422AoAtualizarSemNenhumCampo() throws Exception {
        String token = register(mockMvc, uniqueUsername());

        putUser(token, """
                {"user": {}}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.user[0]").value("must have at least one field"));
    }

    @Test
    void deveRetornar422AoAtualizarComCampoEmBranco() throws Exception {
        String token = register(mockMvc, uniqueUsername());

        putUser(token, """
                {"user": {"username": "   "}}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.username[0]").value("can't be blank"));
    }

    @Test
    void deveRetornar422AoAtualizarEmailComNull() throws Exception {
        String token = register(mockMvc, uniqueUsername());

        putUser(token, """
                {"user": {"email": null, "bio": "outra bio"}}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("can't be blank"));
    }

    @Test
    void deveRetornar422AoAtualizarUsernameComNull() throws Exception {
        String token = register(mockMvc, uniqueUsername());

        putUser(token, """
                {"user": {"username": null}}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.username[0]").value("can't be blank"));
    }

    @Test
    void deveRetornar422AoAtualizarComEmailInvalido() throws Exception {
        String token = register(mockMvc, uniqueUsername());

        putUser(token, """
                {"user": {"email": "sem-arroba"}}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("is invalid"));
    }

    @Test
    void deveRetornar401AoAtualizarSemToken() throws Exception {
        mockMvc.perform(put("/api/user").contentType(MediaType.APPLICATION_JSON).content("""
                        {"user": {"bio": "sem token"}}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions putUser(String token, String body) throws Exception {
        return mockMvc.perform(put("/api/user")
                .header(HttpHeaders.AUTHORIZATION, authorization(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
