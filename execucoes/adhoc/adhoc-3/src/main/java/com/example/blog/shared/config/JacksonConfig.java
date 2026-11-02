package com.example.blog.shared.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * Leitura estrita de JSON: campos de texto só aceitam strings.
 *
 * <p>Por padrão o Jackson converte números e booleanos em texto ({@code "username": 123} vira
 * {@code "123"}). O contrato define esses campos como {@code string}, então a conversão é recusada
 * e a requisição cai no tratamento de corpo inválido (HTTP 422).
 */
@Configuration
class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer strictTextualCoercion() {
        return builder -> builder.withCoercionConfig(LogicalType.Textual, config -> config
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }
}
