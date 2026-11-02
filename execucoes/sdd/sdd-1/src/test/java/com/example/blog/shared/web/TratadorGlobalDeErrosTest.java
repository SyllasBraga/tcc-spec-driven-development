package com.example.blog.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.blog.shared.security.TokenInvalidoException;

class TratadorGlobalDeErrosTest {

    private final TratadorGlobalDeErros tratador = new TratadorGlobalDeErros();

    @Test
    void deveConverterErrosDeCampoUsandoUltimoSegmentoDoCaminho() throws Exception {
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "registroRequest");
        resultado.addError(new FieldError("registroRequest", "user.email", "can't be blank"));
        resultado.addError(new FieldError("registroRequest", "user.username", "can't be blank"));

        ResponseEntity<ErroResponse> resposta = tratador.tratarValidacao(excecaoDeValidacao(resultado));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(resposta.getBody().errors()).containsExactly(
                entry("email", List.of("can't be blank")),
                entry("username", List.of("can't be blank")));
    }

    @Test
    void deveUsarChaveBodyQuandoObjetoUserForNulo() throws Exception {
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "registroRequest");
        resultado.addError(new FieldError("registroRequest", "user", "is invalid"));

        ResponseEntity<ErroResponse> resposta = tratador.tratarValidacao(excecaoDeValidacao(resultado));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(resposta.getBody().errors()).containsOnlyKeys("body");
    }

    @Test
    void deveConverterCorpoIlegivelEm422ComChaveBody() {
        HttpMessageNotReadableException excecao = new HttpMessageNotReadableException(
                "detalhe interno", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErroResponse> resposta = tratador.tratarCorpoIlegivel(excecao);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(resposta.getBody().errors()).isEqualTo(Map.of("body", List.of("is invalid")));
    }

    @Test
    void deveResponderJsonComCharsetUtf8() {
        ResponseEntity<ErroResponse> resposta = tratador.tratarConflito(
                new ConflitoException("email", "has already been taken"));

        assertThat(resposta.getHeaders().getContentType())
                .isEqualTo(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
    }

    @Test
    void deveConverterValidacaoDeDominioEm422() {
        ResponseEntity<ErroResponse> resposta = tratador.tratarValidacaoDeDominio(
                new ValidacaoException("profile", "cannot follow yourself"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(resposta.getBody().errors()).isEqualTo(Map.of("profile", List.of("cannot follow yourself")));
    }

    @Test
    void deveConverterConflitoEm409() {
        ResponseEntity<ErroResponse> resposta = tratador.tratarConflito(
                new ConflitoException("username", "has already been taken"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().errors()).isEqualTo(Map.of("username", List.of("has already been taken")));
    }

    @Test
    void deveConverterCredenciaisInvalidasEm401() {
        ResponseEntity<ErroResponse> resposta = tratador.tratarCredenciaisInvalidas(
                new CredenciaisInvalidasException());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody().errors()).isEqualTo(Map.of("credentials", List.of("invalid")));
    }

    @Test
    void deveConverterRecursoNaoEncontradoEm404() {
        ResponseEntity<ErroResponse> resposta = tratador.tratarNaoEncontrado(
                new RecursoNaoEncontradoException("profile"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody().errors()).isEqualTo(Map.of("profile", List.of("not found")));
    }

    @Test
    void deveConverterTokenInvalidoEm401() {
        ResponseEntity<ErroResponse> resposta = tratador.tratarTokenInvalido(new TokenInvalidoException());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody().errors()).isEqualTo(Map.of("token", List.of("is invalid")));
    }

    @Test
    void deveManterStatusDeExcecaoDoSpringComStatusProprio() {
        ResponseEntity<ErroResponse> resposta = tratador.tratarInesperado(
                new NoResourceFoundException(HttpMethod.GET, "/api/inexistente", "api/inexistente"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody().errors()).containsOnlyKeys("request");
    }

    @Test
    void deveConverterErroInesperadoEm500SemDetalhesInternos() {
        ResponseEntity<ErroResponse> resposta = tratador.tratarInesperado(
                new IllegalStateException("tabela usuario indisponível"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().errors()).isEqualTo(Map.of("server", List.of("unexpected error")));
    }

    private static MethodArgumentNotValidException excecaoDeValidacao(BeanPropertyBindingResult resultado)
            throws NoSuchMethodException {
        Method metodo = TratadorGlobalDeErrosTest.class.getDeclaredMethod("metodoDeReferencia", Object.class);
        return new MethodArgumentNotValidException(new MethodParameter(metodo, 0), resultado);
    }

    @SuppressWarnings("unused")
    private void metodoDeReferencia(Object corpo) {
        // Usado só como alvo do MethodParameter da exceção de validação.
    }
}
