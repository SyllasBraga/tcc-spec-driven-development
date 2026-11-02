package com.example.blog.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.example.blog.TestcontainersConfiguration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class UsuarioApiIT {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";

    @Autowired
    private RestTestClient cliente;

    @Autowired
    private JsonMapper jsonMapper;

    // US1 — cadastro e login

    @Test
    void deveCadastrarComStatus201() {
        String nome = nomeUnico();

        JsonNode corpo = enviar("POST", "/api/users", cadastro(nome, nome + "@exemplo.test", "senha1"), null, 201);

        assertThat(corpo.at("/user/username").asString()).isEqualTo(nome);
        assertThat(corpo.at("/user/email").asString()).isEqualTo(nome + "@exemplo.test");
        assertThat(corpo.at("/user/token").asString()).isNotBlank();
        assertThat(corpo.at("/user/bio").isNull()).isTrue();
        assertThat(corpo.at("/user/image").isNull()).isTrue();
        assertThat(corpo.at("/user").has("password")).isFalse();
    }

    @Test
    void deveResponder409AoRepetirUsername() {
        String nome = nomeUnico();
        cadastrar(nome);

        JsonNode corpo = enviar("POST", "/api/users", cadastro(nome, "outro-" + nome + "@exemplo.test", "senha1"),
                null, 409);

        assertThat(corpo.at("/errors/username/0").asString()).isEqualTo("has already been taken");
    }

    @Test
    void deveResponder409AoRepetirEmailComOutrasMaiusculas() {
        String nome = nomeUnico();
        cadastrar(nome);

        JsonNode corpo = enviar("POST", "/api/users",
                cadastro("outro" + nome, nome.toUpperCase() + "@EXEMPLO.test", "senha1"), null, 409);

        assertThat(corpo.at("/errors/email/0").asString()).isEqualTo("has already been taken");
    }

    @Test
    void deveResponder422ComEmailEmBranco() {
        JsonNode corpo = enviar("POST", "/api/users", cadastro(nomeUnico(), "", "senha1"), null, 422);

        assertThat(corpo.at("/errors/email/0").asString()).isEqualTo("can't be blank");
    }

    @Test
    void deveFazerLoginComCredenciaisCorretas() {
        String nome = nomeUnico();
        cadastrar(nome);

        JsonNode corpo = enviar("POST", "/api/users/login", login(nome + "@exemplo.test", "senha1"), null, 200);

        assertThat(corpo.at("/user/username").asString()).isEqualTo(nome);
        assertThat(corpo.at("/user/token").asString()).isNotBlank();
    }

    @Test
    void deveResponder401ComSenhaErrada() {
        String nome = nomeUnico();
        cadastrar(nome);

        JsonNode corpo = enviar("POST", "/api/users/login", login(nome + "@exemplo.test", "errada"), null, 401);

        assertThat(corpo.at("/errors/credentials/0").asString()).isEqualTo("invalid");
    }

    // US2 — usuário atual

    @Test
    void deveConsultarUsuarioAtualComOMesmoToken() {
        String nome = nomeUnico();
        String token = cadastrar(nome);

        JsonNode corpo = enviar("GET", "/api/user", null, token, 200);

        assertThat(corpo.at("/user/username").asString()).isEqualTo(nome);
        assertThat(corpo.at("/user/token").asString()).isEqualTo(token);
    }

    @Test
    void deveResponder401SemCabecalho() {
        JsonNode corpo = enviar("GET", "/api/user", null, null, 401);

        assertThat(corpo.at("/errors/token/0").asString()).isEqualTo("is missing");
    }

    @Test
    void deveResponder401ComTokenAdulterado() {
        String token = cadastrar(nomeUnico());
        String adulterado = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        JsonNode corpo = enviar("GET", "/api/user", null, adulterado, 401);

        assertThat(corpo.at("/errors/token/0").asString()).isEqualTo("is invalid");
    }

    @Test
    void deveResponder401ComEsquemaBearer() {
        String token = cadastrar(nomeUnico());

        String corpo = cliente.get().uri("/api/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(401)
                .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, JSON_UTF8)
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(ler(corpo).at("/errors/token/0").asString()).isEqualTo("is invalid");
    }

    @Test
    void deveAtualizarBioEImagemERefletirNoGet() {
        String token = cadastrar(nomeUnico());

        enviar("PUT", "/api/user", """
                {"user":{"bio":"Olá, mundo","image":"https://img.exemplo.test/a.png"}}""", token, 200);
        JsonNode corpo = enviar("GET", "/api/user", null, token, 200);

        assertThat(corpo.at("/user/bio").asString()).isEqualTo("Olá, mundo");
        assertThat(corpo.at("/user/image").asString()).isEqualTo("https://img.exemplo.test/a.png");
    }

    @Test
    void deveApagarImagemComNulo() {
        String token = cadastrar(nomeUnico());
        enviar("PUT", "/api/user", """
                {"user":{"image":"https://img.exemplo.test/a.png"}}""", token, 200);

        JsonNode corpo = enviar("PUT", "/api/user", """
                {"user":{"image":null}}""", token, 200);

        assertThat(corpo.at("/user/image").isNull()).isTrue();
    }

    @Test
    void deveAceitarSomenteANovaSenhaNoLoginAposTroca() {
        String nome = nomeUnico();
        String token = cadastrar(nome);

        enviar("PUT", "/api/user", """
                {"user":{"password":"nova-senha"}}""", token, 200);

        enviar("POST", "/api/users/login", login(nome + "@exemplo.test", "senha1"), null, 401);
        enviar("POST", "/api/users/login", login(nome + "@exemplo.test", "nova-senha"), null, 200);
    }

    @Test
    void deveResponder422AoUsarEmailDeOutroUsuario() {
        String outro = nomeUnico();
        cadastrar(outro);
        String token = cadastrar(nomeUnico());

        JsonNode corpo = enviar("PUT", "/api/user", """
                {"user":{"email":"%s@exemplo.test"}}""".formatted(outro), token, 422);

        assertThat(corpo.at("/errors/email/0").asString()).isEqualTo("has already been taken");
    }

    @Test
    void deveResponder422SemCamposNaAtualizacao() {
        String token = cadastrar(nomeUnico());

        enviar("PUT", "/api/user", """
                {"user":{}}""", token, 422);
    }

    // apoio

    private String cadastrar(String nome) {
        JsonNode corpo = enviar("POST", "/api/users", cadastro(nome, nome + "@exemplo.test", "senha1"), null, 201);
        return corpo.at("/user/token").asString();
    }

    private JsonNode enviar(String metodo, String rota, String corpo, String token, int statusEsperado) {
        RestTestClient.RequestBodySpec requisicao = cliente.method(HttpMethod.valueOf(metodo))
                .uri(rota);
        if (token != null) {
            requisicao.header(HttpHeaders.AUTHORIZATION, "Token " + token);
        }
        if (corpo != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).body(corpo);
        }
        String resposta = requisicao.exchange()
                .expectStatus().isEqualTo(statusEsperado)
                .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, JSON_UTF8)
                .expectBody(String.class).returnResult().getResponseBody();
        return ler(resposta);
    }

    private JsonNode ler(String json) {
        return jsonMapper.readTree(json);
    }

    private static String cadastro(String username, String email, String senha) {
        return """
                {"user":{"username":"%s","email":"%s","password":"%s"}}""".formatted(username, email, senha);
    }

    private static String login(String email, String senha) {
        return """
                {"user":{"email":"%s","password":"%s"}}""".formatted(email, senha);
    }

    private static String nomeUnico() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
