package com.example.blog.user.web;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Distingue, em atualização parcial, o {@code null} explícito (vira {@code ""}, "limpar") do campo ausente
 * (continua {@code null}, "não alterar"). Aceita somente texto: número, booleano, objeto ou lista são recusados, em
 * vez de convertidos em texto.
 *
 * <p>{@code @JsonSetter(nulls = Nulls.AS_EMPTY)} não serve aqui: em parâmetros de construtor de record, o Jackson
 * usa o mesmo provedor de nulos para campos ausentes, o que transformaria ausência em {@code ""}.
 */
public class NuloComoVazioDeserializer extends ValueDeserializer<String> {

    private static final String VAZIO = "";

    @Override
    public String deserialize(JsonParser parser, DeserializationContext contexto) {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            return parser.getString();
        }
        return (String) contexto.handleUnexpectedToken(String.class, parser);
    }

    @Override
    public Object getNullValue(DeserializationContext contexto) {
        return VAZIO;
    }

    @Override
    public Object getAbsentValue(DeserializationContext contexto) {
        return null;
    }
}
