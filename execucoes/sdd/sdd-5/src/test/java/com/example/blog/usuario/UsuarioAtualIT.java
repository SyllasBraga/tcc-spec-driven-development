package com.example.blog.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.blog.ApoioDeTeste;
import com.example.blog.RespostaHttp;
import com.example.blog.TestcontainersConfiguration;
import com.example.blog.usuario.persistence.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, ApoioDeTeste.class})
@ActiveProfiles("test")
class UsuarioAtualIT {

    private static final String USUARIO = "/api/user";
    private static final String TOKEN_AUSENTE = """
            {"errors":{"token":["is missing"]}}""";
    private static final String TOKEN_INVALIDO = """
            {"errors":{"token":["is invalid"]}}""";

    private final ApoioDeTeste apoio;
    private Usuario jake;
    private String autorizacao;

    UsuarioAtualIT(@Autowired ApoioDeTeste apoio) {
        this.apoio = apoio;
    }

    @BeforeEach
    void preparar() {
        apoio.limparBanco();
        jake = apoio.criarUsuario("jake", "jake@jake.jake", "jakejake");
        autorizacao = apoio.cabecalhoToken(apoio.tokenDe(jake));
    }

    @Test
    void deveDevolverUsuarioAtual() {
        RespostaHttp resposta = apoio.get(USUARIO, autorizacao).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(200);
        var user = resposta.corpo().get("user");
        assertThat(user.get("email").asString()).isEqualTo("jake@jake.jake");
        assertThat(user.get("username").asString()).isEqualTo("jake");
        assertThat(user.get("token").asString()).isNotBlank();
        assertThat(user.get("bio").isNull()).isTrue();
        assertThat(user.get("image").isNull()).isTrue();
    }

    @Test
    void deveExigirTokenNoGetENoPut() {
        RespostaHttp get = apoio.get(USUARIO, null).comJsonUtf8();
        RespostaHttp put = apoio.put(USUARIO, """
                {"user":{"bio":"x"}}""", null).comJsonUtf8();

        assertThat(get.status()).isEqualTo(401);
        assertThat(get.corpo()).isEqualTo(apoio.json(TOKEN_AUSENTE));
        assertThat(put.status()).isEqualTo(401);
        assertThat(put.corpo()).isEqualTo(apoio.json(TOKEN_AUSENTE));
    }

    @Test
    void deveRecusarEsquemaBearer() {
        assertTokenInvalido("Bearer " + apoio.tokenDe(jake));
    }

    @Test
    void deveRecusarTokenMalformado() {
        assertTokenInvalido("Token abc.def.ghi");
    }

    @Test
    void deveRecusarTokenExpirado() {
        assertTokenInvalido(apoio.cabecalhoToken(apoio.tokenExpirado(jake.getId())));
    }

    @Test
    void deveRecusarTokenComOutroSegredo() {
        assertTokenInvalido(apoio.cabecalhoToken(apoio.tokenComOutroSegredo(jake.getId())));
    }

    @Test
    void deveRecusarTokenDeUsuarioApagado() {
        apoio.limparBanco();

        assertTokenInvalido(autorizacao);
    }

    @Test
    void deveAtualizarBioEImagemMantendoDemaisCampos() {
        RespostaHttp resposta = apoio.put(USUARIO, """
                {"user":{"bio":"Eu trabalho na State Farm.","image":"https://i.stack.imgur.com/xHWG8.jpg"}}
                """, autorizacao).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(200);
        var user = resposta.corpo().get("user");
        assertThat(user.get("bio").asString()).isEqualTo("Eu trabalho na State Farm.");
        assertThat(user.get("image").asString()).isEqualTo("https://i.stack.imgur.com/xHWG8.jpg");
        assertThat(user.get("email").asString()).isEqualTo("jake@jake.jake");
        assertThat(user.get("username").asString()).isEqualTo("jake");

        var consultado = apoio.get(USUARIO, autorizacao).corpo().get("user");
        assertThat(consultado.get("bio").asString()).isEqualTo("Eu trabalho na State Farm.");
    }

    @Test
    void deveLimparBioComNull() {
        apoio.put(USUARIO, """
                {"user":{"bio":"temporaria"}}""", autorizacao);

        RespostaHttp resposta = apoio.put(USUARIO, """
                {"user":{"bio":null}}""", autorizacao).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().get("user").get("bio").isNull()).isTrue();
    }

    @Test
    void deveTrocarSenha() {
        RespostaHttp resposta = apoio.put(USUARIO, """
                {"user":{"password":"novasenha"}}""", autorizacao);

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(apoio.post("/api/users/login", """
                {"user":{"email":"jake@jake.jake","password":"novasenha"}}""").status()).isEqualTo(200);
        assertThat(apoio.post("/api/users/login", """
                {"user":{"email":"jake@jake.jake","password":"jakejake"}}""").status()).isEqualTo(401);
    }

    @Test
    void deveRecusarEmailEUsernameDeOutraConta() {
        apoio.criarUsuario("outro", "outro@jake.jake", "senha123");

        RespostaHttp resposta = apoio.put(USUARIO, """
                {"user":{"email":"OUTRO@jake.jake","username":"outro"}}""", autorizacao).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(422);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"email":["has already been taken"],"username":["has already been taken"]}}"""));
    }

    @Test
    void deveAceitarOsProprioEmailEUsername() {
        RespostaHttp resposta = apoio.put(USUARIO, """
                {"user":{"email":"jake@jake.jake","username":"jake"}}""", autorizacao).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(200);
    }

    @Test
    void deveExigirAoMenosUmCampo() {
        RespostaHttp resposta = apoio.put(USUARIO, """
                {"user":{}}""", autorizacao).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(422);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"user":["can't be blank"]}}"""));
    }

    @Test
    void deveRecusarObrigatoriosVazios() {
        RespostaHttp resposta = apoio.put(USUARIO, """
                {"user":{"email":"","username":null,"password":"  "}}""", autorizacao).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(422);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"email":["can't be blank"],"username":["can't be blank"],"password":["can't be blank"]}}"""));
    }

    @Test
    void deveRecusarEmailInvalido() {
        RespostaHttp resposta = apoio.put(USUARIO, """
                {"user":{"email":"nao-e-email"}}""", autorizacao).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(422);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"email":["is invalid"]}}"""));
    }

    @Test
    void deveRecusarPutSemCorpo() {
        RespostaHttp resposta = apoio.put(USUARIO, null, autorizacao).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(422);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"body":["can't be empty"]}}"""));
    }

    private void assertTokenInvalido(String cabecalho) {
        RespostaHttp get = apoio.get(USUARIO, cabecalho).comJsonUtf8();
        RespostaHttp put = apoio.put(USUARIO, """
                {"user":{"bio":"x"}}""", cabecalho).comJsonUtf8();

        assertThat(get.status()).isEqualTo(401);
        assertThat(get.corpo()).isEqualTo(apoio.json(TOKEN_INVALIDO));
        assertThat(put.status()).isEqualTo(401);
        assertThat(put.corpo()).isEqualTo(apoio.json(TOKEN_INVALIDO));
    }
}
