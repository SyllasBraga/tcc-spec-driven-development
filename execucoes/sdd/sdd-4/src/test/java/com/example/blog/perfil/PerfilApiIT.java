package com.example.blog.perfil;

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
class PerfilApiIT {

    private static final String JSON_UTF8 = "application/json;charset=UTF-8";

    @Autowired
    private RestTestClient cliente;

    @Autowired
    private JsonMapper jsonMapper;

    // US3 — consulta

    @Test
    void deveConsultarPerfilAnonimamente() {
        String nome = nomeUnico();
        cadastrar(nome);

        JsonNode corpo = requisitar(HttpMethod.GET, "/api/profiles/" + nome, null, 200);

        assertThat(corpo.at("/profile/username").asString()).isEqualTo(nome);
        assertThat(corpo.at("/profile/bio").isNull()).isTrue();
        assertThat(corpo.at("/profile/image").isNull()).isTrue();
        assertThat(corpo.at("/profile/following").asBoolean()).isFalse();
    }

    @Test
    void deveResponder401ComTokenInvalidoNaConsultaDePerfil() {
        String nome = nomeUnico();
        cadastrar(nome);

        JsonNode corpo = requisitar(HttpMethod.GET, "/api/profiles/" + nome, "invalido", 401);

        assertThat(corpo.at("/errors/token/0").asString()).isEqualTo("is invalid");
    }

    @Test
    void deveResponder404ParaPerfilInexistente() {
        JsonNode corpo = requisitar(HttpMethod.GET, "/api/profiles/" + nomeUnico(), null, 404);

        assertThat(corpo.at("/errors/profile/0").asString()).isEqualTo("not found");
    }

    // US4 — seguir e deixar de seguir

    @Test
    void deveSeguirDeFormaIdempotenteERefletirNaConsulta() {
        String alvo = nomeUnico();
        cadastrar(alvo);
        String tokenSeguidor = cadastrar(nomeUnico());

        JsonNode primeira = requisitar(HttpMethod.POST, "/api/profiles/" + alvo + "/follow", tokenSeguidor, 200);
        JsonNode segunda = requisitar(HttpMethod.POST, "/api/profiles/" + alvo + "/follow", tokenSeguidor, 200);
        JsonNode consulta = requisitar(HttpMethod.GET, "/api/profiles/" + alvo, tokenSeguidor, 200);

        assertThat(primeira.at("/profile/following").asBoolean()).isTrue();
        assertThat(segunda.at("/profile/following").asBoolean()).isTrue();
        assertThat(consulta.at("/profile/following").asBoolean()).isTrue();
        assertThat(consulta.at("/profile/username").asString()).isEqualTo(alvo);
    }

    @Test
    void deveDeixarDeSeguirDeFormaIdempotente() {
        String alvo = nomeUnico();
        cadastrar(alvo);
        String tokenSeguidor = cadastrar(nomeUnico());
        requisitar(HttpMethod.POST, "/api/profiles/" + alvo + "/follow", tokenSeguidor, 200);

        JsonNode primeira = requisitar(HttpMethod.DELETE, "/api/profiles/" + alvo + "/follow", tokenSeguidor, 200);
        JsonNode segunda = requisitar(HttpMethod.DELETE, "/api/profiles/" + alvo + "/follow", tokenSeguidor, 200);
        JsonNode consulta = requisitar(HttpMethod.GET, "/api/profiles/" + alvo, tokenSeguidor, 200);

        assertThat(primeira.at("/profile/following").asBoolean()).isFalse();
        assertThat(segunda.at("/profile/following").asBoolean()).isFalse();
        assertThat(consulta.at("/profile/following").asBoolean()).isFalse();
    }

    @Test
    void deveResponder422AoSeguirASiMesmo() {
        String nome = nomeUnico();
        String token = cadastrar(nome);

        JsonNode corpo = requisitar(HttpMethod.POST, "/api/profiles/" + nome + "/follow", token, 422);

        assertThat(corpo.at("/errors/profile/0").asString()).isEqualTo("cannot follow yourself");
    }

    @Test
    void deveResponder404AoSeguirAlvoInexistente() {
        String token = cadastrar(nomeUnico());

        requisitar(HttpMethod.POST, "/api/profiles/" + nomeUnico() + "/follow", token, 404);
        requisitar(HttpMethod.DELETE, "/api/profiles/" + nomeUnico() + "/follow", token, 404);
    }

    @Test
    void deveResponder401AoSeguirSemToken() {
        String alvo = nomeUnico();
        cadastrar(alvo);

        JsonNode corpo = requisitar(HttpMethod.POST, "/api/profiles/" + alvo + "/follow", null, 401);
        requisitar(HttpMethod.DELETE, "/api/profiles/" + alvo + "/follow", null, 401);

        assertThat(corpo.at("/errors/token/0").asString()).isEqualTo("is missing");
    }

    // apoio

    private String cadastrar(String nome) {
        String resposta = cliente.post().uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"user":{"username":"%s","email":"%s@exemplo.test","password":"senha1"}}"""
                        .formatted(nome, nome))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody();
        return jsonMapper.readTree(resposta).at("/user/token").asString();
    }

    private JsonNode requisitar(HttpMethod metodo, String rota, String token, int statusEsperado) {
        RestTestClient.RequestBodySpec requisicao = cliente.method(metodo).uri(rota);
        if (token != null) {
            requisicao.header(HttpHeaders.AUTHORIZATION, "Token " + token);
        }
        String resposta = requisicao.exchange()
                .expectStatus().isEqualTo(statusEsperado)
                .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, JSON_UTF8)
                .expectBody(String.class).returnResult().getResponseBody();
        return jsonMapper.readTree(resposta);
    }

    private static String nomeUnico() {
        return "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
