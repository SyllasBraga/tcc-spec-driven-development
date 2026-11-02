package com.example.blog.perfil;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.blog.ApoioDeTeste;
import com.example.blog.RespostaHttp;
import com.example.blog.TestcontainersConfiguration;
import com.example.blog.perfil.persistence.Seguimento;
import com.example.blog.perfil.persistence.SeguimentoId;
import com.example.blog.perfil.persistence.SeguimentoRepository;
import com.example.blog.usuario.persistence.Usuario;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, ApoioDeTeste.class})
@ActiveProfiles("test")
class PerfilIT {

    private static final String PERFIL_JAKE = "/api/profiles/jake";
    private static final String TOKEN_INVALIDO = """
            {"errors":{"token":["is invalid"]}}""";

    private final ApoioDeTeste apoio;
    private final SeguimentoRepository seguimentoRepository;
    private Usuario jake;
    private Usuario ana;
    private Usuario bia;

    PerfilIT(@Autowired ApoioDeTeste apoio, @Autowired SeguimentoRepository seguimentoRepository) {
        this.apoio = apoio;
        this.seguimentoRepository = seguimentoRepository;
    }

    @BeforeEach
    void preparar() {
        apoio.limparBanco();
        jake = apoio.criarUsuario("jake", "jake@jake.jake", "jakejake");
        ana = apoio.criarUsuario("ana", "ana@jake.jake", "anaana12");
        bia = apoio.criarUsuario("bia", "bia@jake.jake", "biabia12");
        seguimentoRepository.saveAndFlush(new Seguimento(new SeguimentoId(ana.getId(), jake.getId()), Instant.now()));
    }

    @Test
    void deveDevolverPerfilSemToken() {
        RespostaHttp resposta = apoio.get(PERFIL_JAKE, null).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"profile":{"username":"jake","bio":null,"image":null,"following":false}}"""));
    }

    @Test
    void deveIndicarQueSegueComTokenDeQuemSegue() {
        RespostaHttp resposta = apoio.get(PERFIL_JAKE, apoio.cabecalhoToken(apoio.tokenDe(ana))).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().get("profile").get("following").asBoolean()).isTrue();
    }

    @Test
    void deveIndicarQueNaoSegueComTokenDeQuemNaoSegue() {
        RespostaHttp resposta = apoio.get(PERFIL_JAKE, apoio.cabecalhoToken(apoio.tokenDe(bia))).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().get("profile").get("following").asBoolean()).isFalse();
    }

    @Test
    void deveDevolver404ParaUsuarioInexistente() {
        RespostaHttp resposta = apoio.get("/api/profiles/ninguem", null).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(404);
        assertThat(resposta.corpo()).isEqualTo(apoio.json("""
                {"errors":{"profile":["not found"]}}"""));
    }

    @Test
    void deveDiferenciarMaiusculasNoUsername() {
        RespostaHttp resposta = apoio.get("/api/profiles/Jake", null).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(404);
    }

    @Test
    void deveRecusarTokenMalformado() {
        RespostaHttp resposta = apoio.get(PERFIL_JAKE, "Token abc.def.ghi").comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(401);
        assertThat(resposta.corpo()).isEqualTo(apoio.json(TOKEN_INVALIDO));
    }

    @Test
    void deveRecusarEsquemaBearer() {
        RespostaHttp resposta = apoio.get(PERFIL_JAKE, "Bearer " + apoio.tokenDe(ana)).comJsonUtf8();

        assertThat(resposta.status()).isEqualTo(401);
        assertThat(resposta.corpo()).isEqualTo(apoio.json(TOKEN_INVALIDO));
    }
}
