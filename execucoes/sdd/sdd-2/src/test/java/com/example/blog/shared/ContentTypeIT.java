package com.example.blog.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.blog.TestcontainersConfiguration;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Todas as respostas com corpo usam {@code application/json; charset=utf-8} (FR-027), inclusive as de erro.
 * Usa o servidor embutido de verdade: o MockMvc não reproduz o descarte do charset pelo contêiner.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ContentTypeIT {

    private static final String JSON_UTF8 = "(?i)application/json;\\s*charset=utf-8";

    @LocalServerPort
    private int porta;

    private final HttpClient cliente = HttpClient.newHttpClient();

    @AfterEach
    void fecharCliente() {
        cliente.close();
    }

    private HttpResponse<String> enviar(String metodo, String caminho, String corpo)
            throws IOException, InterruptedException {
        HttpRequest.Builder requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho));
        if (corpo == null) {
            requisicao.method(metodo, HttpRequest.BodyPublishers.noBody());
        } else {
            requisicao.header("Content-Type", "application/json")
                    .method(metodo, HttpRequest.BodyPublishers.ofString(corpo));
        }
        return cliente.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String corpoCadastro(String username) {
        return """
                {"user":{"username":"%s","email":"%s@exemplo.test","password":"senha-ficticia"}}"""
                .formatted(username, username);
    }

    private static String nomeUnico() {
        return "ct-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static void assertJsonUtf8(HttpResponse<String> resposta, int statusEsperado) {
        assertThat(resposta.statusCode()).isEqualTo(statusEsperado);
        assertThat(resposta.headers().firstValue("Content-Type")).hasValueSatisfying(
                tipo -> assertThat(tipo).matches(JSON_UTF8));
    }

    @Test
    void deveUsarJsonUtf8No201() throws Exception {
        assertJsonUtf8(enviar("POST", "/api/users", corpoCadastro(nomeUnico())), 201);
    }

    @Test
    void deveUsarJsonUtf8No200() throws Exception {
        String username = nomeUnico();
        enviar("POST", "/api/users", corpoCadastro(username));

        assertJsonUtf8(enviar("GET", "/api/profiles/" + username, null), 200);
    }

    @Test
    void deveUsarJsonUtf8No401DoEntryPoint() throws Exception {
        assertJsonUtf8(enviar("GET", "/api/user", null), 401);
    }

    @Test
    void deveUsarJsonUtf8No401DoHandlerDeErros() throws Exception {
        String corpo = """
                {"user":{"email":"ninguem-%s@exemplo.test","password":"x"}}""".formatted(UUID.randomUUID());

        assertJsonUtf8(enviar("POST", "/api/users/login", corpo), 401);
    }

    @Test
    void deveUsarJsonUtf8No404() throws Exception {
        assertJsonUtf8(enviar("GET", "/api/profiles/ninguem-" + UUID.randomUUID(), null), 404);
    }

    @Test
    void deveUsarJsonUtf8No409() throws Exception {
        String username = nomeUnico();
        enviar("POST", "/api/users", corpoCadastro(username));

        assertJsonUtf8(enviar("POST", "/api/users", corpoCadastro(username)), 409);
    }

    @Test
    void deveUsarJsonUtf8No422() throws Exception {
        assertJsonUtf8(enviar("POST", "/api/users", "{"), 422);
    }
}
