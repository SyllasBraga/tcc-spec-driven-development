package com.example.blog.usuario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.example.blog.shared.web.ValidacaoException;
import com.example.blog.usuario.service.AtualizacaoUsuario;
import com.example.blog.usuario.service.AtualizacaoUsuario.Campo;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class AtualizacaoUsuarioMapperTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static ValidatorFactory validatorFactory;
    private static AtualizacaoUsuarioMapper mapper;

    @BeforeAll
    static void criarMapper() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        mapper = new AtualizacaoUsuarioMapper(validatorFactory.getValidator());
    }

    @AfterAll
    static void fecharValidador() {
        validatorFactory.close();
    }

    @Test
    void deveRecusarObjetoSemCampos() {
        assertErros("{}", Map.of("user", List.of("at least one field is required")));
    }

    @Test
    void deveRecusarObjetoSoComCamposDesconhecidos() {
        assertErros("{\"token\": \"x\", \"following\": true}", Map.of("user", List.of("at least one field is required")));
    }

    @Test
    void deveDistinguirBioNulaDeBioAusente() {
        AtualizacaoUsuario comando = mapper.converter(objeto("{\"bio\": null}"));

        assertThat(comando.bio()).isEqualTo(Campo.informado(null));
        assertThat(comando.image()).isEqualTo(Campo.ausente());
        assertThat(comando.email()).isEqualTo(Campo.ausente());
    }

    @Test
    void deveTratarBioEImageEmBrancoComoNulas() {
        AtualizacaoUsuario comando = mapper.converter(objeto("{\"bio\": \"\", \"image\": \"   \"}"));

        assertThat(comando.bio()).isEqualTo(Campo.informado(null));
        assertThat(comando.image()).isEqualTo(Campo.informado(null));
    }

    @Test
    void deveConverterCamposInformados() {
        AtualizacaoUsuario comando = mapper.converter(objeto(
                "{\"email\": \"novo@x.com\", \"username\": \"novo\", \"password\": \"senha-nova\", "
                        + "\"bio\": \"Olá\", \"image\": \"https://exemplo.test/a.png\"}"));

        assertThat(comando).isEqualTo(new AtualizacaoUsuario(Campo.informado("novo@x.com"), Campo.informado("novo"),
                Campo.informado("senha-nova"), Campo.informado("Olá"), Campo.informado("https://exemplo.test/a.png")));
    }

    @Test
    void deveRecusarEmailEmBranco() {
        assertErros("{\"email\": \"\"}", Map.of("email", List.of("can't be blank")));
    }

    @Test
    void deveRecusarUsernameNulo() {
        assertErros("{\"username\": null}", Map.of("username", List.of("can't be blank")));
    }

    @Test
    void deveRecusarPasswordSoComEspacos() {
        assertErros("{\"password\": \"   \"}", Map.of("password", List.of("can't be blank")));
    }

    @Test
    void deveRecusarPasswordComMenosDeOitoCaracteres() {
        assertErros("{\"password\": \"short7c\"}",
                Map.of("password", List.of("is too short (minimum is 8 characters)")));
    }

    @Test
    void deveAceitarPasswordComOitoESessentaEQuatroCaracteres() {
        assertThat(mapper.converter(objeto("{\"password\": \"bonjour1\"}")).password())
                .isEqualTo(Campo.informado("bonjour1"));
        String senhaLonga = "a".repeat(64);
        assertThat(mapper.converter(objeto("{\"password\": \"" + senhaLonga + "\"}")).password())
                .isEqualTo(Campo.informado(senhaLonga));
    }

    @Test
    void deveRecusarEmailComFormatoInvalido() {
        assertErros("{\"email\": \"x\"}", Map.of("email", List.of("is invalid")));
    }

    @Test
    void deveAcumularErrosDeVariosCampos() {
        assertErros("{\"email\": \"x\", \"username\": \"\", \"password\": null}", Map.of(
                "username", List.of("can't be blank"),
                "password", List.of("can't be blank"),
                "email", List.of("is invalid")));
    }

    @Test
    void deveRecusarBioNaoTextual() {
        assertErros("{\"bio\": 1}", Map.of("bio", List.of("is invalid")));
    }

    @Test
    void deveRecusarUserAusente() {
        assertThatThrownBy(() -> mapper.converter(null))
                .isInstanceOfSatisfying(ValidacaoException.class,
                        ex -> assertThat(ex.getErros()).containsOnlyKeys("body"));
    }

    private static void assertErros(String json, Map<String, List<String>> esperado) {
        assertThatThrownBy(() -> mapper.converter(objeto(json)))
                .isInstanceOfSatisfying(ValidacaoException.class, ex -> assertThat(ex.getErros()).isEqualTo(esperado));
    }

    private static ObjectNode objeto(String json) {
        return (ObjectNode) JSON.readTree(json);
    }
}
