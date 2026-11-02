package com.example.blog.profile.web;

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
class ProfileApiIT {

    @LocalServerPort
    private int port;

    private ApiTestClient api;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(port);
    }

    @Test
    void deveConsultarPerfilSemAutenticacao() throws Exception {
        String username = uniqueUsername();
        api.register(username);

        ApiResponse response = api.send("GET", "/api/profiles/" + username, null, null);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.contentType()).isEqualToIgnoringCase("application/json;charset=UTF-8");
        assertThat(response.json().path("profile").path("username").asString()).isEqualTo(username);
        assertThat(response.json().path("profile").get("bio").isNull()).isTrue();
        assertThat(response.json().path("profile").get("image").isNull()).isTrue();
        assertThat(response.json().path("profile").path("following").asBoolean()).isFalse();
    }

    @Test
    void deveRetornar404QuandoPerfilNaoExiste() throws Exception {
        ApiResponse response = api.send("GET", "/api/profiles/" + uniqueUsername(), null, null);

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.json().path("errors").path("profile").path(0).asString()).isEqualTo("not found");
    }

    @Test
    void deveSeguirUsuarioEMostrarFollowingNaConsulta() throws Exception {
        String seguido = uniqueUsername();
        api.register(seguido);
        String token = api.register(uniqueUsername());

        ApiResponse follow = api.send("POST", "/api/profiles/" + seguido + "/follow", token, null);
        ApiResponse profile = api.send("GET", "/api/profiles/" + seguido, token, null);

        assertThat(follow.status()).isEqualTo(200);
        assertThat(follow.json().path("profile").path("following").asBoolean()).isTrue();
        assertThat(profile.json().path("profile").path("following").asBoolean()).isTrue();
    }

    @Test
    void deveSeguirDuasVezesSemErro() throws Exception {
        String seguido = uniqueUsername();
        api.register(seguido);
        String token = api.register(uniqueUsername());
        api.send("POST", "/api/profiles/" + seguido + "/follow", token, null);

        ApiResponse response = api.send("POST", "/api/profiles/" + seguido + "/follow", token, null);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.json().path("profile").path("following").asBoolean()).isTrue();
    }

    @Test
    void deveDeixarDeSeguirUsuario() throws Exception {
        String seguido = uniqueUsername();
        api.register(seguido);
        String token = api.register(uniqueUsername());
        api.send("POST", "/api/profiles/" + seguido + "/follow", token, null);

        ApiResponse unfollow = api.send("DELETE", "/api/profiles/" + seguido + "/follow", token, null);
        ApiResponse profile = api.send("GET", "/api/profiles/" + seguido, token, null);

        assertThat(unfollow.status()).isEqualTo(200);
        assertThat(unfollow.json().path("profile").path("following").asBoolean()).isFalse();
        assertThat(profile.json().path("profile").path("following").asBoolean()).isFalse();
    }

    @Test
    void deveRetornar401AoSeguirSemToken() throws Exception {
        String seguido = uniqueUsername();
        api.register(seguido);

        ApiResponse response = api.send("POST", "/api/profiles/" + seguido + "/follow", null, null);

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.json().path("errors").path("token").path(0).asString()).isEqualTo("is missing");
    }

    @Test
    void deveRetornar404AoSeguirUsuarioInexistente() throws Exception {
        String token = api.register(uniqueUsername());

        ApiResponse response = api.send("POST", "/api/profiles/" + uniqueUsername() + "/follow", token, null);

        assertThat(response.status()).isEqualTo(404);
    }

    @Test
    void deveRetornar422AoSeguirASiMesmo() throws Exception {
        String username = uniqueUsername();
        String token = api.register(username);

        ApiResponse response = api.send("POST", "/api/profiles/" + username + "/follow", token, null);

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.json().path("errors").path("profile").path(0).asString())
                .isEqualTo("cannot follow yourself");
    }
}
