package com.example.blog.perfil;

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
class SeguimentoIT {

    private static final String SEGUIR_JAKE = "/api/profiles/jake/follow";
    private static final String NAO_ENCONTRADO = """
            {"errors":{"profile":["not found"]}}""";

    private final ApoioDeTeste apoio;
    private Usuario ana;
    private String autorizacaoAna;

    SeguimentoIT(@Autowired ApoioDeTeste apoio) {
        this.apoio = apoio;
    }

    @BeforeEach
    void preparar() {
        apoio.limparBanco();
        apoio.criarUsuario("jake", "jake@jake.jake", "jakejake");
        ana = apoio.criarUsuario("ana", "ana@jake.jake", "anaana12");
        autorizacaoAna = apoio.cabecalhoToken(apoio.tokenDe(ana));
    }

    @Test
    void deveSeguirDeFormaIdempotente() {
        RespostaHttp primeira = apoio.post(SEGUIR_JAKE, null, autorizacaoAna).comJsonUtf8();

        assertThat(primeira.status()).isEqualTo(200);
        assertThat(primeira.corpo()).isEqualTo(apoio.json("""
                {"profile":{"username":"jake","bio":null,"image":null,"following":true}}"""));
        assertThat(apoio.contarLinhas("seguimento")).isEqualTo(1);

        RespostaHttp segunda = apoio.post(SEGUIR_JAKE, null, autorizacaoAna).comJsonUtf8();

        assertThat(segunda.status()).isEqualTo(200);
        assertThat(segunda.corpo().get("profile").get("following").asBoolean()).isTrue();
        assertThat(apoio.contarLinhas("seguimento")).isEqualTo(1);
        assertThat(apoio.get("/api/profiles/jake", autorizacaoAna).corpo().get("profile").get("following")
                .asBoolean()).isTrue();
    }

    @Test
    void deveDeixarDeSeguirDeFormaIdempotente() {
        apoio.post(SEGUIR_JAKE, null, autorizacaoAna);

        RespostaHttp primeira = apoio.delete(SEGUIR_JAKE, autorizacaoAna).comJsonUtf8();

        assertThat(primeira.status()).isEqualTo(200);
        assertThat(primeira.corpo().get("profile").get("following").asBoolean()).isFalse();
        assertThat(apoio.contarLinhas("seguimento")).isZero();

        RespostaHttp segunda = apoio.delete(SEGUIR_JAKE, autorizacaoAna).comJsonUtf8();

        assertThat(segunda.status()).isEqualTo(200);
        assertThat(segunda.corpo().get("profile").get("following").asBoolean()).isFalse();
    }

    @Test
    void deveDeixarDeSeguirSemTerSeguidoAntes() {
        RespostaHttp resposta = apoio.delete(SEGUIR_JAKE, autorizacaoAna).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().get("profile").get("following").asBoolean()).isFalse();
    }

    @Test
    void deveDevolver404ParaAlvoInexistente() {
        RespostaHttp seguir = apoio.post("/api/profiles/ninguem/follow", null, autorizacaoAna).comJsonUtf8();
        RespostaHttp deixar = apoio.delete("/api/profiles/ninguem/follow", autorizacaoAna).comJsonUtf8();

        assertThat(seguir.status()).isEqualTo(404);
        assertThat(seguir.corpo()).isEqualTo(apoio.json(NAO_ENCONTRADO));
        assertThat(deixar.status()).isEqualTo(404);
        assertThat(deixar.corpo()).isEqualTo(apoio.json(NAO_ENCONTRADO));
    }

    @Test
    void deveImpedirSeguirASiMesmo() {
        RespostaHttp resposta = apoio.post("/api/profiles/ana/follow", null, autorizacaoAna).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(422);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"profile":["cannot follow yourself"]}}"""));
        assertThat(apoio.contarLinhas("seguimento")).isZero();
    }

    @Test
    void deveExigirToken() {
        RespostaHttp seguir = apoio.post(SEGUIR_JAKE, null, null).comJsonUtf8();
        RespostaHttp deixar = apoio.delete(SEGUIR_JAKE, null).comJsonUtf8();

        assertThat(seguir.status()).isEqualTo(401);
        assertThat(seguir.corpo()).isEqualTo(apoio.json("""
                {"errors":{"token":["is missing"]}}"""));
        assertThat(deixar.status()).isEqualTo(401);
    }

    @Test
    void deveRecusarTokenInvalido() {
        RespostaHttp resposta = apoio.post(SEGUIR_JAKE, null, "Token abc.def.ghi").comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(401);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"token":["is invalid"]}}"""));
    }
}
