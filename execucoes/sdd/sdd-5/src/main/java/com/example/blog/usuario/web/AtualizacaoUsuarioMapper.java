package com.example.blog.usuario.web;

import com.example.blog.shared.erro.MensagensErro;
import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.usuario.service.AtualizacaoUsuario;
import com.example.blog.usuario.service.CampoAtualizado;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Converte o corpo do {@code PUT /api/user} em {@link AtualizacaoUsuario}, acumulando todos os erros de
 * validação em uma única {@link ValidacaoException}.
 *
 * <p>Campos aceitos: {@code email}, {@code username}, {@code password}, {@code bio} e {@code image}. Campos
 * desconhecidos são ignorados.
 */
@Component
public class AtualizacaoUsuarioMapper {

    private static final String CAMPO_EMAIL = "email";
    private static final String CAMPO_USERNAME = "username";
    private static final String CAMPO_PASSWORD = "password";
    private static final String CAMPO_BIO = "bio";
    private static final String CAMPO_IMAGE = "image";
    private static final List<String> CAMPOS_ACEITOS =
            List.of(CAMPO_EMAIL, CAMPO_USERNAME, CAMPO_PASSWORD, CAMPO_BIO, CAMPO_IMAGE);

    private final Validator validator;

    public AtualizacaoUsuarioMapper(Validator validator) {
        this.validator = validator;
    }

    /**
     * @throws ValidacaoException com {@code body} se {@code user} não for um objeto, com {@code user} se nenhum
     *     campo aceito vier, ou com os erros de cada campo
     */
    public AtualizacaoUsuario converter(AtualizacaoRequest requisicao) {
        JsonNode user = requisicao == null ? null : requisicao.user();
        if (user == null || !user.isObject()) {
            throw ValidacaoException.de(MensagensErro.CHAVE_BODY, MensagensErro.CORPO_VAZIO);
        }
        if (CAMPOS_ACEITOS.stream().noneMatch(user::has)) {
            throw ValidacaoException.de(MensagensErro.CHAVE_USER, MensagensErro.NAO_PODE_SER_VAZIO);
        }
        Map<String, List<String>> erros = new TreeMap<>();
        CampoAtualizado<String> email = textoObrigatorio(user, CAMPO_EMAIL, erros);
        if (email.presente() && email.valor() != null && !isEmailValido(email.valor())) {
            erros.put(CAMPO_EMAIL, List.of(MensagensErro.INVALIDO));
        }
        CampoAtualizado<String> username = textoObrigatorio(user, CAMPO_USERNAME, erros);
        CampoAtualizado<String> senha = senha(user, erros);
        CampoAtualizado<String> bio = textoOpcional(user, CAMPO_BIO, erros);
        CampoAtualizado<String> imagem = textoOpcional(user, CAMPO_IMAGE, erros);
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
        return new AtualizacaoUsuario(email, username, senha, bio, imagem);
    }

    /** E-mail e username: se enviados, precisam ser texto não vazio; o valor é aparado. */
    private static CampoAtualizado<String> textoObrigatorio(JsonNode user, String campo,
            Map<String, List<String>> erros) {
        if (!user.has(campo)) {
            return CampoAtualizado.ausente();
        }
        String texto = textoNaoVazio(user.get(campo), campo, erros);
        return texto == null ? CampoAtualizado.ausente() : CampoAtualizado.de(texto.trim());
    }

    /** Senha: se enviada, precisa ser texto não vazio; o valor é mantido como enviado. */
    private static CampoAtualizado<String> senha(JsonNode user, Map<String, List<String>> erros) {
        if (!user.has(CAMPO_PASSWORD)) {
            return CampoAtualizado.ausente();
        }
        String texto = textoNaoVazio(user.get(CAMPO_PASSWORD), CAMPO_PASSWORD, erros);
        return texto == null ? CampoAtualizado.ausente() : CampoAtualizado.de(texto);
    }

    /** Bio e imagem: aceitam texto ou {@code null}; {@code null} e texto vazio ou só com espaços limpam o valor. */
    private static CampoAtualizado<String> textoOpcional(JsonNode user, String campo,
            Map<String, List<String>> erros) {
        if (!user.has(campo)) {
            return CampoAtualizado.ausente();
        }
        JsonNode valor = user.get(campo);
        if (valor.isNull()) {
            return CampoAtualizado.de(null);
        }
        if (!valor.isString()) {
            erros.put(campo, List.of(MensagensErro.INVALIDO));
            return CampoAtualizado.ausente();
        }
        String texto = valor.stringValue();
        return CampoAtualizado.de(texto.isBlank() ? null : texto);
    }

    /** Devolve o texto, ou {@code null} depois de registrar o erro do campo. */
    private static String textoNaoVazio(JsonNode valor, String campo, Map<String, List<String>> erros) {
        if (valor.isNull() || (valor.isString() && valor.stringValue().isBlank())) {
            erros.put(campo, List.of(MensagensErro.NAO_PODE_SER_VAZIO));
            return null;
        }
        if (!valor.isString()) {
            erros.put(campo, List.of(MensagensErro.INVALIDO));
            return null;
        }
        return valor.stringValue();
    }

    /** Mesma regra de formato do cadastro ({@code @Email} de {@link NovoUsuario#email()}). */
    private boolean isEmailValido(String email) {
        return validator.validateValue(NovoUsuario.class, CAMPO_EMAIL, email).isEmpty();
    }
}
