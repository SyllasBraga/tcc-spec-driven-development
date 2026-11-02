package com.example.blog.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.blog.ApoioDeTeste;
import com.example.blog.RespostaHttp;
import com.example.blog.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, ApoioDeTeste.class})
@ActiveProfiles("test")
class LoginIT {

    private static final String LOGIN = "/api/users/login";
    private static final String CREDENCIAIS_INVALIDAS = """
            {"errors":{"credentials":["invalid"]}}""";

    private final ApoioDeTeste apoio;

    LoginIT(@Autowired ApoioDeTeste apoio) {
        this.apoio = apoio;
    }

    @BeforeEach
    void preparar() {
        apoio.limparBanco();
        apoio.criarUsuario("jake", "jake@jake.jake", "jakejake");
    }

    @Test
    void deveAutenticarComCredenciaisCorretas() {
        RespostaHttp resposta = apoio.post(LOGIN, """
                {"user":{"email":"jake@jake.jake","password":"jakejake"}}
                """).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(200);
        var user = resposta.corpo().get("user");
        assertThat(user.propertyNames()).containsExactlyInAnyOrder("email", "token", "username", "bio", "image");
        assertThat(user.get("username").asString()).isEqualTo("jake");
        assertThat(user.get("token").asString()).isNotBlank();
    }

    @Test
    void deveAutenticarComEmailEmMaiusculas() {
        RespostaHttp resposta = apoio.post(LOGIN, """
                {"user":{"email":"JAKE@Jake.jake","password":"jakejake"}}
                """).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(200);
    }

    @Test
    void deveRecusarSenhaErrada() {
        RespostaHttp resposta = apoio.post(LOGIN, """
                {"user":{"email":"jake@jake.jake","password":"errada"}}
                """).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(401);
        assertThat(resposta.corpo()).isEqualTo(apoio.json(CREDENCIAIS_INVALIDAS));
    }

    @Test
    void deveRecusarEmailInexistenteComOMesmoCorpo() {
        RespostaHttp resposta = apoio.post(LOGIN, """
                {"user":{"email":"ninguem@jake.jake","password":"jakejake"}}
                """).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(401);
        assertThat(resposta.corpo()).isEqualTo(apoio.json(CREDENCIAIS_INVALIDAS));
    }

    @Test
    void deveExigirEmail() {
        RespostaHttp resposta = apoio.post(LOGIN, """
                {"user":{"password":"jakejake"}}
                """).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(422);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"email":["can't be blank"]}}"""));
    }

    @Test
    void deveRejeitarEmailESenhaVazios() {
        RespostaHttp resposta = apoio.post(LOGIN, """
                {"user":{"email":"","password":""}}
                """).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(422);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"email":["can't be blank"],"password":["can't be blank"]}}"""));
    }

    @Test
    void deveRejeitarCorpoSemUser() {
        RespostaHttp resposta = apoio.post(LOGIN, "{}").comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(422);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"body":["can't be empty"]}}"""));
    }
}
