package com.example.blog.usuario.web;

import static com.example.blog.shared.erro.MensagensDeErro.INVALIDO;
import static com.example.blog.shared.erro.MensagensDeErro.NAO_PODE_FICAR_EM_BRANCO;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;

import org.springframework.stereotype.Component;

import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.usuario.service.AlteracaoUsuario;
import com.example.blog.usuario.service.AlteracaoUsuario.Campo;

import tools.jackson.databind.JsonNode;

/**
 * Converte o corpo de {@code PUT /api/user} em {@link AlteracaoUsuario}, distinguindo campo ausente de campo
 * presente com {@code null}, e aplica as regras de formato. Campos desconhecidos são ignorados.
 */
@Component
public class AlteracaoUsuarioMapper {

    private static final String RAIZ = "user";
    private static final String NENHUM_CAMPO = "must have at least one field";

    private final Validator validator;

    public AlteracaoUsuarioMapper(Validator validator) {
        this.validator = validator;
    }

    /**
     * @throws ValidacaoException se o objeto {@code user} faltar, se nenhum campo aceito for informado ou se algum
     *         campo violar as regras de formato
     */
    public AlteracaoUsuario converter(JsonNode corpo) {
        JsonNode user = corpo == null ? null : corpo.get(RAIZ);
        if (user == null || user.isNull()) {
            throw new ValidacaoException(RAIZ, NAO_PODE_FICAR_EM_BRANCO);
        }
        if (!user.isObject()) {
            throw new ValidacaoException(RAIZ, INVALIDO);
        }
        Campo email = campoObrigatorio(user, "email");
        if (email.presente() && !isEmailValido(email.valor())) {
            throw new ValidacaoException("email", INVALIDO);
        }
        AlteracaoUsuario alteracao = new AlteracaoUsuario(email, campoObrigatorio(user, "username"),
                campoObrigatorio(user, "password"), campoAnulavel(user, "bio"), campoAnulavel(user, "image"));
        if (alteracao.isVazia()) {
            throw new ValidacaoException(RAIZ, NENHUM_CAMPO);
        }
        return alteracao;
    }

    /** Campo que, se informado, não pode ser nulo nem em branco. */
    private static Campo campoObrigatorio(JsonNode user, String nome) {
        if (!user.has(nome)) {
            return Campo.AUSENTE;
        }
        JsonNode valor = user.get(nome);
        if (valor.isNull()) {
            throw new ValidacaoException(nome, NAO_PODE_FICAR_EM_BRANCO);
        }
        if (!valor.isString()) {
            throw new ValidacaoException(nome, INVALIDO);
        }
        if (valor.asString().isBlank()) {
            throw new ValidacaoException(nome, NAO_PODE_FICAR_EM_BRANCO);
        }
        return Campo.com(valor.asString());
    }

    /** Campo que aceita texto ou {@code null}. {@code null} ou texto em branco apagam o valor. */
    private static Campo campoAnulavel(JsonNode user, String nome) {
        if (!user.has(nome)) {
            return Campo.AUSENTE;
        }
        JsonNode valor = user.get(nome);
        if (valor.isNull()) {
            return Campo.com(null);
        }
        if (!valor.isString()) {
            throw new ValidacaoException(nome, INVALIDO);
        }
        String texto = valor.asString();
        return Campo.com(texto.isBlank() ? null : texto);
    }

    private boolean isEmailValido(String email) {
        return validator.validateValue(FormatoDeEmail.class, "email", email).isEmpty();
    }

    /** Usado só para aplicar a mesma regra de {@code @Email} do cadastro. */
    private record FormatoDeEmail(@Email String email) {
    }
}
