package com.example.blog.usuario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.usuario.service.AlteracaoUsuario;
import com.example.blog.usuario.service.AlteracaoUsuario.Campo;

import tools.jackson.databind.json.JsonMapper;

class AlteracaoUsuarioMapperTest {

    private static ValidatorFactory fabrica;
    private static AlteracaoUsuarioMapper mapper;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @BeforeAll
    static void criarMapper() {
        fabrica = Validation.buildDefaultValidatorFactory();
        mapper = new AlteracaoUsuarioMapper(fabrica.getValidator());
    }

    @AfterAll
    static void fecharFabrica() {
        fabrica.close();
    }

    @Test
    void deveMarcarCamposAusentesComoNaoPresentes() {
        AlteracaoUsuario alteracao = converter("""
                {"user":{"bio":"Olá"}}""");

        assertThat(alteracao.bio()).isEqualTo(Campo.com("Olá"));
        assertThat(alteracao.email()).isEqualTo(Campo.AUSENTE);
        assertThat(alteracao.username()).isEqualTo(Campo.AUSENTE);
        assertThat(alteracao.password()).isEqualTo(Campo.AUSENTE);
        assertThat(alteracao.image()).isEqualTo(Campo.AUSENTE);
    }

    @Test
    void deveMarcarBioNulaComoPresenteComValorNulo() {
        AlteracaoUsuario alteracao = converter("""
                {"user":{"bio":null,"image":null}}""");

        assertThat(alteracao.bio()).isEqualTo(Campo.com(null));
        assertThat(alteracao.image()).isEqualTo(Campo.com(null));
    }

    @Test
    void deveTratarBioEImageEmBrancoComoNulos() {
        AlteracaoUsuario alteracao = converter("""
                {"user":{"bio":"","image":"  "}}""");

        assertThat(alteracao.bio()).isEqualTo(Campo.com(null));
        assertThat(alteracao.image()).isEqualTo(Campo.com(null));
    }

    @Test
    void deveAceitarTodosOsCampos() {
        AlteracaoUsuario alteracao = converter("""
                {"user":{"email":"a@exemplo.test","username":"a","password":"p","bio":"b","image":"i"}}""");

        assertThat(alteracao).isEqualTo(new AlteracaoUsuario(Campo.com("a@exemplo.test"), Campo.com("a"),
                Campo.com("p"), Campo.com("b"), Campo.com("i")));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "{\"user\":{\"email\":null}}       | email",
        "{\"user\":{\"email\":\"  \"}}     | email",
        "{\"user\":{\"username\":null}}    | username",
        "{\"user\":{\"username\":\"\"}}    | username",
        "{\"user\":{\"password\":null}}    | password",
        "{\"user\":{\"password\":\" \"}}   | password"
    })
    void deveRecusarCampoObrigatorioNuloOuEmBranco(String json, String campo) {
        assertErro(json, campo, "can't be blank");
    }

    @Test
    void deveRecusarEmailSemFormato() {
        assertErro("""
                {"user":{"email":"nao-e-email"}}""", "email", "is invalid");
    }

    @Test
    void deveRecusarCampoQueNaoETexto() {
        assertErro("""
                {"user":{"bio":123}}""", "bio", "is invalid");
    }

    @Test
    void deveRecusarObjetoUserVazio() {
        assertErro("""
                {"user":{}}""", "user", "must have at least one field");
    }

    @Test
    void deveRecusarObjetoUserSomenteComCamposDesconhecidos() {
        assertErro("""
                {"user":{"apelido":"x"}}""", "user", "must have at least one field");
    }

    @Test
    void deveRecusarCorpoSemObjetoUser() {
        assertErro("""
                {"bio":"x"}""", "user", "can't be blank");
    }

    private AlteracaoUsuario converter(String json) {
        return mapper.converter(jsonMapper.readTree(json));
    }

    private void assertErro(String json, String chave, String mensagem) {
        assertThatThrownBy(() -> converter(json))
                .isInstanceOfSatisfying(ValidacaoException.class, erro -> {
                    assertThat(erro.getChave()).isEqualTo(chave);
                    assertThat(erro.getMensagem()).isEqualTo(mensagem);
                });
    }
}
