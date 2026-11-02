package com.example.blog.user.web;

import static com.example.blog.ApiTestClient.emailOf;
import static com.example.blog.ApiTestClient.uniqueUsername;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.example.blog.ApiTestClient;
import com.example.blog.ApiTestClient.ApiResponse;
import com.example.blog.TestcontainersConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class UserApiIT {

    @LocalServerPort
    private int port;

    private ApiTestClient api;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(port);
    }

    @Test
    void deveCadastrarUsuarioERetornar201ComToken() throws Exception {
        String username = uniqueUsername();

        ApiResponse response = api.send("POST", "/api/users", null, """
                {"user": {"username": "%s", "email": "%s", "password": "jakejake"}}
                """.formatted(username, emailOf(username).toUpperCase()));

        assertThat(response.status()).isEqualTo(201);
        assertThat(response.contentType()).isEqualToIgnoringCase("application/json;charset=UTF-8");
        assertThat(response.json().path("user").path("username").asString()).isEqualTo(username);
        assertThat(response.json().path("user").path("email").asString()).isEqualTo(emailOf(username));
        assertThat(response.json().path("user").path("token").asString()).isNotBlank();
        assertThat(response.json().path("user").get("bio").isNull()).isTrue();
        assertThat(response.json().path("user").get("image").isNull()).isTrue();
    }

    @Test
    void deveRetornar409QuandoNomeEEmailJaEstaoCadastrados() throws Exception {
        String username = uniqueUsername();
        api.register(username);

        ApiResponse response = api.send("POST", "/api/users", null, """
                {"user": {"username": "%s", "email": "%s", "password": "jakejake"}}
                """.formatted(username, emailOf(username)));

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.json().path("errors").path("username").path(0).asString())
                .isEqualTo("has already been taken");
        assertThat(response.json().path("errors").path("email").path(0).asString())
                .isEqualTo("has already been taken");
    }

    @Test
    void deveRetornar422ComErroPorCampoQuandoCadastroInvalido() throws Exception {
        ApiResponse response = api.send("POST", "/api/users", null, """
                {"user": {"username": "", "email": "nao-e-email"}}
                """);

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.json().path("errors").path("username").path(0).asString()).isEqualTo("can't be blank");
        assertThat(response.json().path("errors").path("email").path(0).asString()).isEqualTo("is invalid");
        assertThat(response.json().path("errors").path("password").path(0).asString()).isEqualTo("can't be blank");
    }

    @Test
    void deveRetornar422QuandoCorpoNaoTemObjetoUser() throws Exception {
        ApiResponse response = api.send("POST", "/api/users", null, "{}");

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.json().path("errors").path("user").path(0).asString()).isEqualTo("can't be blank");
    }

    @Test
    void deveFazerLoginComEmailESenhaCorretos() throws Exception {
        String username = uniqueUsername();
        api.register(username);

        ApiResponse response = api.send("POST", "/api/users/login", null, """
                {"user": {"email": "%s", "password": "password"}}
                """.formatted(emailOf(username)));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.json().path("user").path("username").asString()).isEqualTo(username);
        assertThat(response.json().path("user").path("token").asString()).isNotBlank();
    }

    @Test
    void deveRetornar401QuandoSenhaDoLoginEstaErrada() throws Exception {
        String username = uniqueUsername();
        api.register(username);

        ApiResponse response = api.send("POST", "/api/users/login", null, """
                {"user": {"email": "%s", "password": "senha-errada"}}
                """.formatted(emailOf(username)));

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.json().path("errors").path("credentials").path(0).asString())
                .isEqualTo("invalid");
    }

    @Test
    void deveRetornar401QuandoConsultaUsuarioAtualSemToken() throws Exception {
        ApiResponse response = api.send("GET", "/api/user", null, null);

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.contentType()).isEqualToIgnoringCase("application/json;charset=UTF-8");
        assertThat(response.json().path("errors").path("token").path(0).asString()).isEqualTo("is missing");
    }

    @Test
    void deveRetornar401QuandoTokenEInvalido() throws Exception {
        ApiResponse response = api.send("GET", "/api/user", "token.invalido.aqui", null);

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.json().path("errors").path("token").path(0).asString()).isEqualTo("is invalid");
    }

    @Test
    void deveRetornarUsuarioAtualComOMesmoToken() throws Exception {
        String username = uniqueUsername();
        String token = api.register(username);

        ApiResponse response = api.send("GET", "/api/user", token, null);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.json().path("user").path("username").asString()).isEqualTo(username);
        assertThat(response.json().path("user").path("token").asString()).isEqualTo(token);
    }

    @Test
    void deveAtualizarBioEApagarImagemQuandoEnviadaComoNula() throws Exception {
        String token = api.register(uniqueUsername());
        api.send("PUT", "/api/user", token, """
                {"user": {"image": "https://example.com/foto.png"}}
                """);

        ApiResponse response = api.send("PUT", "/api/user", token, """
                {"user": {"bio": "I like to skateboard", "image": null}}
                """);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.json().path("user").path("bio").asString()).isEqualTo("I like to skateboard");
        assertThat(response.json().path("user").get("image").isNull()).isTrue();
    }

    @Test
    void deveManterBioQuandoCampoNaoEnviado() throws Exception {
        String token = api.register(uniqueUsername());
        api.send("PUT", "/api/user", token, """
                {"user": {"bio": "bio original"}}
                """);

        ApiResponse response = api.send("PUT", "/api/user", token, """
                {"user": {"image": "https://example.com/foto.png"}}
                """);

        assertThat(response.json().path("user").path("bio").asString()).isEqualTo("bio original");
    }

    @Test
    void devePermitirLoginComNovaSenhaENovoEmailAposAtualizacao() throws Exception {
        String token = api.register(uniqueUsername());
        String novoEmail = emailOf(uniqueUsername());
        api.send("PUT", "/api/user", token, """
                {"user": {"email": "%s", "password": "nova-senha"}}
                """.formatted(novoEmail));

        ApiResponse response = api.send("POST", "/api/users/login", null, """
                {"user": {"email": "%s", "password": "nova-senha"}}
                """.formatted(novoEmail));

        assertThat(response.status()).isEqualTo(200);
    }

    @Test
    void deveGravarBioEImagemVaziasComoNulas() throws Exception {
        String token = api.register(uniqueUsername());
        api.send("PUT", "/api/user", token, """
                {"user": {"bio": "bio original", "image": "https://example.com/foto.png"}}
                """);

        ApiResponse response = api.send("PUT", "/api/user", token, """
                {"user": {"bio": "", "image": ""}}
                """);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.json().path("user").get("bio").isNull()).isTrue();
        assertThat(response.json().path("user").get("image").isNull()).isTrue();
    }

    @Test
    void deveRetornar422QuandoEmailEnviadoComoNulo() throws Exception {
        String token = api.register(uniqueUsername());

        ApiResponse response = api.send("PUT", "/api/user", token, """
                {"user": {"email": null, "bio": "x"}}
                """);

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.json().path("errors").path("email").path(0).asString()).isEqualTo("can't be blank");
    }

    @Test
    void deveRetornar422QuandoNovaSenhaTemMenosDeOitoCaracteres() throws Exception {
        String token = api.register(uniqueUsername());

        ApiResponse response = api.send("PUT", "/api/user", token, """
                {"user": {"password": "short7c"}}
                """);

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.json().path("errors").path("password").path(0).asString())
                .isEqualTo("is too short (minimum is 8 characters)");
    }

    @Test
    void deveRetornar422QuandoAtualizacaoNaoTemCampos() throws Exception {
        String token = api.register(uniqueUsername());

        ApiResponse response = api.send("PUT", "/api/user", token, """
                {"user": {}}
                """);

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.json().path("errors").path("user").path(0).asString())
                .isEqualTo("must have at least one field");
    }

    @Test
    void deveRetornar422QuandoAtualizaParaNomeDeOutroUsuario() throws Exception {
        String outroUsuario = uniqueUsername();
        api.register(outroUsuario);
        String token = api.register(uniqueUsername());

        ApiResponse response = api.send("PUT", "/api/user", token, """
                {"user": {"username": "%s"}}
                """.formatted(outroUsuario));

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.json().path("errors").path("username").path(0).asString())
                .isEqualTo("has already been taken");
    }

    @Test
    void deveRetornar401QuandoAtualizaSemToken() throws Exception {
        ApiResponse response = api.send("PUT", "/api/user", null, """
                {"user": {"bio": "x"}}
                """);

        assertThat(response.status()).isEqualTo(401);
    }
}
