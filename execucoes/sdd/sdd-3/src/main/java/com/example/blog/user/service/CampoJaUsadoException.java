package com.example.blog.user.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;

import com.example.blog.shared.error.ApiException;

/**
 * Nome de usuário ou e-mail já usado por outra pessoa. Responde {@code 409} no cadastro e {@code 422} na
 * atualização, conforme o contrato.
 */
public class CampoJaUsadoException extends ApiException {

    static final String USERNAME = "username";
    static final String EMAIL = "email";
    private static final List<String> ORDEM = List.of(USERNAME, EMAIL);
    private static final String MENSAGEM = "has already been taken";

    private CampoJaUsadoException(HttpStatus status, Set<String> campos) {
        super(status, mensagens(campos), "Campos já usados: " + campos);
    }

    public static CampoJaUsadoException noCadastro(Set<String> campos) {
        return new CampoJaUsadoException(HttpStatus.CONFLICT, campos);
    }

    public static CampoJaUsadoException naAtualizacao(Set<String> campos) {
        return new CampoJaUsadoException(HttpStatus.UNPROCESSABLE_CONTENT, campos);
    }

    private static Map<String, List<String>> mensagens(Set<String> campos) {
        var erros = new LinkedHashMap<String, List<String>>();
        for (String campo : ORDEM) {
            if (campos.contains(campo)) {
                erros.put(campo, List.of(MENSAGEM));
            }
        }
        return erros;
    }
}
