package com.example.blog.usuario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.usuario.service.AtualizacaoUsuario;
import com.example.blog.usuario.service.CampoAtualizado;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class AtualizacaoUsuarioMapperTest {

    private static ValidatorFactory fabrica;
    private static AtualizacaoUsuarioMapper mapper;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @BeforeAll
    static void criarMapper() {
        fabrica = Validation.buildDefaultValidatorFactory();
        mapper = new AtualizacaoUsuarioMapper(fabrica.getValidator());
    }

    @AfterAll
    static void fecharFabrica() {
        fabrica.close();
    }

    @Test
    void deveMarcarCamposAusentes() {
        AtualizacaoUsuario atualizacao = converter("""
                {"bio":"Oi"}""");

        assertThat(atualizacao.bio()).isEqualTo(CampoAtualizado.de("Oi"));
        assertThat(atualizacao.email()).isEqualTo(CampoAtualizado.ausente());
        assertThat(atualizacao.username()).isEqualTo(CampoAtualizado.ausente());
        assertThat(atualizacao.senha()).isEqualTo(CampoAtualizado.ausente());
        assertThat(atualizacao.imagem()).isEqualTo(CampoAtualizado.ausente());
    }

    @Test
    void deveAceitarBioNulaComoPresente() {
        AtualizacaoUsuario atualizacao = converter("""
                {"bio":null,"image":"https://img/x.png"}""");

        assertThat(atualizacao.bio()).isEqualTo(CampoAtualizado.de(null));
        assertThat(atualizacao.imagem()).isEqualTo(CampoAtualizado.de("https://img/x.png"));
    }

    @Test
    void deveAparEmailEUsernameEManterSenha() {
        AtualizacaoUsuario atualizacao = converter("""
                {"email":" novo@jake.jake ","username":" novo ","password":" s e n h a "}""");

        assertThat(atualizacao.email()).isEqualTo(CampoAtualizado.de("novo@jake.jake"));
        assertThat(atualizacao.username()).isEqualTo(CampoAtualizado.de("novo"));
        assertThat(atualizacao.senha()).isEqualTo(CampoAtualizado.de(" s e n h a "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"desconhecido\":1}"})
    void deveExigirAoMenosUmCampoAceito(String user) {
        assertErros(user, Map.of("user", List.of("can't be blank")));
    }

    @Test
    void deveRejeitarObrigatoriosNulosVaziosOuSoEspacos() {
        assertErros("""
                {"email":"","username":null,"password":"  "}""",
                Map.of("email", List.of("can't be blank"),
                        "username", List.of("can't be blank"),
                        "password", List.of("can't be blank")));
    }

    @Test
    void deveRejeitarEmailSemFormatoValido() {
        assertErros("""
                {"email":"nao-e-email"}""", Map.of("email", List.of("is invalid")));
    }

    @Test
    void deveRejeitarBioEImageNaoTextuais() {
        assertErros("""
                {"bio":1,"image":{"a":1}}""", Map.of("bio", List.of("is invalid"), "image", List.of("is invalid")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"texto\"", "[1]"})
    void deveRejeitarUserQueNaoEhObjeto(String user) {
        assertErros(user, Map.of("body", List.of("can't be empty")));
    }

    @Test
    void deveRejeitarRequisicaoSemUser() {
        assertThatThrownBy(() -> mapper.converter(new AtualizacaoRequest(null)))
                .isInstanceOfSatisfying(ValidacaoException.class, excecao -> assertThat(excecao.getErros())
                        .isEqualTo(Map.of("body", List.of("can't be empty"))));
    }

    @Test
    void deveAcumularVariosErros() {
        assertErros("""
                {"email":"x","username":"","bio":false}""",
                Map.of("email", List.of("is invalid"),
                        "username", List.of("can't be blank"),
                        "bio", List.of("is invalid")));
    }

    private AtualizacaoUsuario converter(String user) {
        return mapper.converter(new AtualizacaoRequest(jsonMapper.readTree(user)));
    }

    private void assertErros(String user, Map<String, List<String>> esperados) {
        assertThatThrownBy(() -> converter(user))
                .isInstanceOfSatisfying(ValidacaoException.class,
                        excecao -> assertThat(excecao.getErros()).isEqualTo(esperados));
    }
}
