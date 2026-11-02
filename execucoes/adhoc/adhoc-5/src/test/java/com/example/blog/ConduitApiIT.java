package com.example.blog;

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Teste ponta a ponta do contrato da API sobre PostgreSQL descartável (Testcontainers), com todas as migrations
 * aplicadas e o mapeamento JPA validado ({@code ddl-auto: validate}). Cada teste usa usuários próprios.
 */
@SpringBootTest(properties = "app.jwt.secret=segredo-ficticio-de-teste-com-mais-de-32-bytes")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ConduitApiIT {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveCadastrarUsuarioERetornarTokenComStatus201() throws Exception {
        mockMvc.perform(json(post("/api/users"), """
                        {"user":{"username":"cadastro","email":"Cadastro@Example.com","password":"senha-cadastro"}}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonUtf8())
                .andExpect(jsonPath("$.user.username").value("cadastro"))
                .andExpect(jsonPath("$.user.email").value("cadastro@example.com"))
                .andExpect(jsonPath("$.user.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.user.bio", nullValue()))
                .andExpect(jsonPath("$.user.image", nullValue()));
    }

    @Test
    void deveRetornar409QuandoUsernameOuEmailJaExistem() throws Exception {
        register("duplicado", "duplicado@example.com");

        mockMvc.perform(json(post("/api/users"), """
                        {"user":{"username":"duplicado","email":"outro@example.com","password":"senha123"}}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonUtf8())
                .andExpect(jsonPath("$.errors.username[0]").value("has already been taken"));
        mockMvc.perform(json(post("/api/users"), """
                        {"user":{"username":"outro","email":"DUPLICADO@example.com","password":"senha123"}}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.email[0]").value("has already been taken"));
    }

    @Test
    void deveRetornar422ComErrosPorCampoQuandoCadastroInvalido() throws Exception {
        mockMvc.perform(json(post("/api/users"), """
                        {"user":{"username":"","email":"nao-e-email"}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonUtf8())
                .andExpect(jsonPath("$.errors.username[0]").value("can't be blank"))
                .andExpect(jsonPath("$.errors.email[0]").value("is invalid"))
                .andExpect(jsonPath("$.errors.password[0]").value("can't be blank"));
    }

    @Test
    void deveRetornar422QuandoCorpoMalformado() throws Exception {
        mockMvc.perform(json(post("/api/users"), "{nao e json"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.body[0]").exists());
    }

    @Test
    void deveFazerLoginComEmailEmQualquerCaixa() throws Exception {
        register("login", "login@example.com");

        mockMvc.perform(json(post("/api/users/login"), """
                        {"user":{"email":"LOGIN@example.com","password":"senha-login"}}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonUtf8())
                .andExpect(jsonPath("$.user.username").value("login"))
                .andExpect(jsonPath("$.user.token", not(emptyOrNullString())));
    }

    @Test
    void deveRetornar401QuandoSenhaIncorreta() throws Exception {
        register("senhaerrada", "senhaerrada@example.com");

        mockMvc.perform(json(post("/api/users/login"), """
                        {"user":{"email":"senhaerrada@example.com","password":"incorreta"}}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.credentials[0]").value("invalid"));
    }

    @Test
    void deveRetornar422QuandoLoginSemCampos() throws Exception {
        mockMvc.perform(json(post("/api/users/login"), """
                        {"user":{}}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("can't be blank"))
                .andExpect(jsonPath("$.errors.password[0]").value("can't be blank"));
    }

    @Test
    void deveRetornarUsuarioAtualComOMesmoToken() throws Exception {
        String token = register("atual", "atual@example.com");

        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonUtf8())
                .andExpect(jsonPath("$.user.username").value("atual"))
                .andExpect(jsonPath("$.user.token").value(token));
    }

    @Test
    void deveRetornar401SemTokenOuComTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonUtf8())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
        mockMvc.perform(get("/api/user").header(HttpHeaders.AUTHORIZATION, "Token abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is invalid"));
    }

    @Test
    void deveAtualizarUsuarioEPermitirLoginComNovaSenha() throws Exception {
        String token = register("atualiza", "atualiza@example.com");

        mockMvc.perform(authorized(json(put("/api/user"), """
                        {"user":{"email":"novo-atualiza@example.com","bio":"I like to skateboard",
                                 "image":"https://example.com/foto.jpg","password":"nova-senha"}}
                        """), token))
                .andExpect(status().isOk())
                .andExpect(jsonUtf8())
                .andExpect(jsonPath("$.user.email").value("novo-atualiza@example.com"))
                .andExpect(jsonPath("$.user.username").value("atualiza"))
                .andExpect(jsonPath("$.user.bio").value("I like to skateboard"))
                .andExpect(jsonPath("$.user.image").value("https://example.com/foto.jpg"));

        mockMvc.perform(json(post("/api/users/login"), """
                        {"user":{"email":"novo-atualiza@example.com","password":"nova-senha"}}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void deveRemoverBioQuandoTextoVazio() throws Exception {
        String token = register("limpabio", "limpabio@example.com");
        mockMvc.perform(authorized(json(put("/api/user"), """
                {"user":{"bio":"temporaria"}}
                """), token));

        mockMvc.perform(authorized(json(put("/api/user"), """
                        {"user":{"bio":""}}
                        """), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio", nullValue()));
    }

    @Test
    void deveRemoverBioEImagemQuandoNullExplicito() throws Exception {
        String token = register("limpanull", "limpanull@example.com");
        mockMvc.perform(authorized(json(put("/api/user"), """
                {"user":{"bio":"temporaria","image":"https://example.com/temp.jpg"}}
                """), token));

        mockMvc.perform(authorized(json(put("/api/user"), """
                        {"user":{"bio":null,"image":null}}
                        """), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.bio", nullValue()))
                .andExpect(jsonPath("$.user.image", nullValue()));
    }

    @Test
    void deveRetornar422QuandoEmailNullExplicito() throws Exception {
        String token = register("emailnull", "emailnull@example.com");

        mockMvc.perform(authorized(json(put("/api/user"), """
                        {"user":{"email":null,"bio":"qualquer"}}
                        """), token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("can't be blank"));
    }

    @Test
    void deveRetornar422QuandoCampoTextoRecebeNumero() throws Exception {
        String token = register("tiponumero", "tiponumero@example.com");

        mockMvc.perform(authorized(json(put("/api/user"), """
                        {"user":{"username":123}}
                        """), token))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void deveRetornar422QuandoEmailSemPontoNoDominio() throws Exception {
        String token = register("semponto", "semponto@example.com");

        mockMvc.perform(authorized(json(put("/api/user"), """
                        {"user":{"email":"a@b"}}
                        """), token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email[0]").value("is invalid"));
    }

    @Test
    void deveRetornar422QuandoAtualizacaoSemCampos() throws Exception {
        String token = register("semcampos", "semcampos@example.com");

        mockMvc.perform(authorized(json(put("/api/user"), """
                        {"user":{}}
                        """), token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.user[0]").value("must have at least one field"));
    }

    @Test
    void deveRetornar409QuandoAtualizaParaEmailDeOutroUsuario() throws Exception {
        register("donoemail", "donoemail@example.com");
        String token = register("invasor", "invasor@example.com");

        mockMvc.perform(authorized(json(put("/api/user"), """
                        {"user":{"email":"donoemail@example.com"}}
                        """), token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.email[0]").value("has already been taken"));
    }

    @Test
    void deveRetornarPerfilSemAutenticacao() throws Exception {
        register("publico", "publico@example.com");

        mockMvc.perform(get("/api/profiles/publico"))
                .andExpect(status().isOk())
                .andExpect(jsonUtf8())
                .andExpect(jsonPath("$.profile.username").value("publico"))
                .andExpect(jsonPath("$.profile.bio", nullValue()))
                .andExpect(jsonPath("$.profile.image", nullValue()))
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void deveRetornar404QuandoPerfilNaoExiste() throws Exception {
        mockMvc.perform(get("/api/profiles/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonUtf8())
                .andExpect(jsonPath("$.errors.profile[0]").value("not found"));
    }

    @Test
    void deveSeguirEDeixarDeSeguirDeFormaIdempotente() throws Exception {
        register("seguido", "seguido@example.com");
        String token = register("seguidor", "seguidor@example.com");

        mockMvc.perform(authorized(post("/api/profiles/seguido/follow"), token))
                .andExpect(status().isOk())
                .andExpect(jsonUtf8())
                .andExpect(jsonPath("$.profile.username").value("seguido"))
                .andExpect(jsonPath("$.profile.following").value(true));
        mockMvc.perform(authorized(post("/api/profiles/seguido/follow"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(true));
        mockMvc.perform(authorized(get("/api/profiles/seguido"), token))
                .andExpect(jsonPath("$.profile.following").value(true));
        mockMvc.perform(get("/api/profiles/seguido"))
                .andExpect(jsonPath("$.profile.following").value(false));

        mockMvc.perform(authorized(delete("/api/profiles/seguido/follow"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(false));
        mockMvc.perform(authorized(delete("/api/profiles/seguido/follow"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.following").value(false));
        mockMvc.perform(authorized(get("/api/profiles/seguido"), token))
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void deveExigirAutenticacaoParaSeguir() throws Exception {
        register("alvo", "alvo@example.com");

        mockMvc.perform(post("/api/profiles/alvo/follow"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.token[0]").value("is missing"));
        mockMvc.perform(delete("/api/profiles/alvo/follow"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar404AoSeguirPerfilInexistente() throws Exception {
        String token = register("solitario", "solitario@example.com");

        mockMvc.perform(authorized(post("/api/profiles/ninguem/follow"), token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.profile[0]").value("not found"));
    }

    @Test
    void deveRetornar422AoSeguirASiMesmo() throws Exception {
        String token = register("narciso", "narciso@example.com");

        mockMvc.perform(authorized(post("/api/profiles/narciso/follow"), token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.profile[0]").value("can't follow yourself"));
    }

    /** Cadastra um usuário com senha {@code senha-<username>} e devolve o token. */
    private String register(String username, String email) throws Exception {
        String body = """
                {"user":{"username":"%s","email":"%s","password":"senha-%s"}}
                """.formatted(username, email, username);
        String response = mockMvc.perform(json(post("/api/users"), body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.user.token");
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Token " + token);
    }

    private static ResultMatcher jsonUtf8() {
        return header().string(HttpHeaders.CONTENT_TYPE, equalToIgnoringCase(JSON_UTF8));
    }
}
