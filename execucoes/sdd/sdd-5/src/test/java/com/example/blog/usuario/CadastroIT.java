package com.example.blog.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.blog.ApoioDeTeste;
import com.example.blog.RespostaHttp;
import com.example.blog.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, ApoioDeTeste.class})
@ActiveProfiles("test")
class CadastroIT {

    private static final String USUARIOS = "/api/users";

    private final ApoioDeTeste apoio;

    CadastroIT(@Autowired ApoioDeTeste apoio) {
        this.apoio = apoio;
    }

    @BeforeEach
    void limpar() {
        apoio.limparBanco();
    }

    @Test
    void deveCadastrarUsuarioValido() {
        RespostaHttp resposta = apoio.post(USUARIOS, """
                {"user":{"username":"jake","email":"jake@jake.jake","password":"jakejake"}}
                """).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(201);
        var user = resposta.corpo().get("user");
        assertThat(user.propertyNames()).containsExactlyInAnyOrder("email", "token", "username", "bio", "image");
        assertThat(user.get("email").asString()).isEqualTo("jake@jake.jake");
        assertThat(user.get("username").asString()).isEqualTo("jake");
        assertThat(user.get("token").asString()).isNotBlank();
        assertThat(user.get("bio").isNull()).isTrue();
        assertThat(user.get("image").isNull()).isTrue();
        assertThat(resposta.corpo().toString()).doesNotContain("jakejake").doesNotContain("bcrypt");
    }

    @Test
    void deveRejeitarUsernameJaExistente() {
        apoio.criarUsuario("jake", "outro@jake.jake", "senha123");

        RespostaHttp resposta = apoio.post(USUARIOS, """
                {"user":{"username":"jake","email":"jake@jake.jake","password":"jakejake"}}
                """).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(409);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"username":["has already been taken"]}}"""));
    }

    @Test
    void deveRejeitarEmailJaExistenteSemDiferenciarMaiusculas() {
        apoio.criarUsuario("outro", "jake@jake.jake", "senha123");

        RespostaHttp resposta = apoio.post(USUARIOS, """
                {"user":{"username":"jake","email":"JAKE@jake.jake","password":"jakejake"}}
                """).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(409);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"email":["has already been taken"]}}"""));
    }

    @Test
    void deveDevolverAsDuasChavesQuandoAmbosDuplicados() {
        apoio.criarUsuario("jake", "jake@jake.jake", "senha123");

        RespostaHttp resposta = apoio.post(USUARIOS, """
                {"user":{"username":"jake","email":"jake@jake.jake","password":"jakejake"}}
                """).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(409);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"email":["has already been taken"],"username":["has already been taken"]}}"""));
    }

    @Test
    void deveExigirSenha() {
        RespostaHttp resposta = apoio.post(USUARIOS, """
                {"user":{"username":"jake","email":"jake@jake.jake"}}
                """).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(422);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"password":["can't be blank"]}}"""));
    }

    @Test
    void deveRejeitarCamposVaziosOuSoEspacos() {
        RespostaHttp resposta = apoio.post(USUARIOS, """
                {"user":{"username":"  ","email":"","password":"   "}}
                """).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(422);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"username":["can't be blank"],"email":["can't be blank"],"password":["can't be blank"]}}"""));
    }

    @Test
    void deveRejeitarEmailInvalido() {
        RespostaHttp resposta = apoio.post(USUARIOS, """
                {"user":{"username":"jake","email":"nao-e-email","password":"jakejake"}}
                """).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(422);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"email":["is invalid"]}}"""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{\"user\":", "{}", "{\"usuario\":{}}", "{\"user\":null}", "{\"user\":\"x\"}"})
    void deveRejeitarCorpoAusenteOuIlegivel(String corpo) {
        RespostaHttp resposta = apoio.post(USUARIOS, corpo).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(422);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"body":["can't be empty"]}}"""));
    }
}
