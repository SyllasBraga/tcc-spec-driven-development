package com.example.blog.shared.erro;

/**
 * Chaves e mensagens de erro fixadas pelo contrato da API ({@code contracts/erros.md}).
 */
public final class MensagensErro {

    public static final String NAO_PODE_SER_VAZIO = "can't be blank";
    public static final String CORPO_VAZIO = "can't be empty";
    public static final String INVALIDO = "is invalid";
    public static final String CREDENCIAIS_INVALIDAS = "invalid";
    public static final String JA_EM_USO = "has already been taken";
    public static final String AUSENTE = "is missing";
    public static final String NAO_ENCONTRADO = "not found";
    public static final String NAO_PODE_SEGUIR_A_SI_MESMO = "cannot follow yourself";
    public static final String PROIBIDO = "forbidden";
    public static final String ERRO_INTERNO = "internal error";

    public static final String CHAVE_BODY = "body";
    public static final String CHAVE_USER = "user";
    public static final String CHAVE_TOKEN = "token";
    public static final String CHAVE_PROFILE = "profile";
    public static final String CHAVE_CREDENCIAIS = "credentials";
    public static final String CHAVE_AUTORIZACAO = "authorization";
    public static final String CHAVE_SERVIDOR = "server";
    public static final String CHAVE_REQUISICAO = "request";

    private MensagensErro() {
    }
}
