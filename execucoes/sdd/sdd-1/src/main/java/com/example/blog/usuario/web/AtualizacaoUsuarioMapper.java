package com.example.blog.usuario.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Validator;

import org.springframework.stereotype.Component;

import com.example.blog.shared.web.ValidacaoException;
import com.example.blog.usuario.service.AtualizacaoUsuario;
import com.example.blog.usuario.service.AtualizacaoUsuario.Campo;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Converte o corpo de {@code PUT /api/user} no comando {@link AtualizacaoUsuario}, validando os campos.
 * Campos desconhecidos são ignorados.
 */
@Component
public class AtualizacaoUsuarioMapper {

    static final String MENSAGEM_EM_BRANCO = "can't be blank";
    static final String MENSAGEM_INVALIDO = "is invalid";
    static final String MENSAGEM_SEM_CAMPOS = "at least one field is required";
    static final String MENSAGEM_SENHA_CURTA = "is too short (minimum is 8 characters)";

    /** Política NIST SP 800-63B do contrato para {@code PUT /api/user}: no mínimo 8 caracteres. */
    static final int TAMANHO_MINIMO_SENHA = 8;

    private static final String EMAIL = "email";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String BIO = "bio";
    private static final String IMAGE = "image";

    private final Validator validator;

    public AtualizacaoUsuarioMapper(Validator validator) {
        this.validator = validator;
    }

    /**
     * @throws ValidacaoException com todos os campos inválidos, ou {@code user} sem nenhum campo aceito
     */
    public AtualizacaoUsuario converter(ObjectNode user) {
        if (user == null) {
            throw new ValidacaoException("body", MENSAGEM_INVALIDO);
        }
        Map<String, List<String>> erros = new LinkedHashMap<>();
        Campo email = campoObrigatorioSePresente(user, EMAIL, erros);
        Campo username = campoObrigatorioSePresente(user, USERNAME, erros);
        Campo password = campoObrigatorioSePresente(user, PASSWORD, erros);
        if (password.valor() != null && isSenhaCurta(password.valor())) {
            adicionar(erros, PASSWORD, MENSAGEM_SENHA_CURTA);
        }
        Campo bio = campoAnulavel(user, BIO, erros);
        Campo image = campoAnulavel(user, IMAGE, erros);
        if (email.isInformado() && email.valor() != null && !isEmailValido(email.valor())) {
            adicionar(erros, EMAIL, MENSAGEM_INVALIDO);
        }
        AtualizacaoUsuario comando = new AtualizacaoUsuario(email, username, password, bio, image);
        if (erros.isEmpty() && !comando.hasAlgumCampo()) {
            adicionar(erros, "user", MENSAGEM_SEM_CAMPOS);
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
        return comando;
    }

    private static Campo campoObrigatorioSePresente(ObjectNode user, String nome, Map<String, List<String>> erros) {
        if (!user.has(nome)) {
            return Campo.ausente();
        }
        JsonNode valor = user.get(nome);
        if (!valor.isString() || valor.stringValue().isBlank()) {
            adicionar(erros, nome, MENSAGEM_EM_BRANCO);
            return Campo.informado(null);
        }
        return Campo.informado(valor.stringValue());
    }

    private static Campo campoAnulavel(ObjectNode user, String nome, Map<String, List<String>> erros) {
        if (!user.has(nome)) {
            return Campo.ausente();
        }
        JsonNode valor = user.get(nome);
        if (valor.isNull()) {
            return Campo.informado(null);
        }
        if (!valor.isString()) {
            adicionar(erros, nome, MENSAGEM_INVALIDO);
            return Campo.informado(null);
        }
        // Texto em branco equivale a null: o contrato devolve o campo limpo como null, nunca "".
        if (valor.stringValue().isBlank()) {
            return Campo.informado(null);
        }
        return Campo.informado(valor.stringValue());
    }

    /** Conta caracteres Unicode (code points), não unidades UTF-16. */
    private static boolean isSenhaCurta(String senha) {
        return senha.codePointCount(0, senha.length()) < TAMANHO_MINIMO_SENHA;
    }

    /** Reaproveita a mesma regra {@code @Email} do cadastro. */
    private boolean isEmailValido(String email) {
        return validator.validateValue(RegistroRequest.UsuarioNovo.class, EMAIL, email.trim()).isEmpty();
    }

    private static void adicionar(Map<String, List<String>> erros, String campo, String mensagem) {
        erros.computeIfAbsent(campo, chave -> new ArrayList<>()).add(mensagem);
    }
}
