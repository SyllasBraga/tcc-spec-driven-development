package com.example.blog.shared.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/** Ajustes do Jackson para a leitura das requisições. */
@Configuration
public class JacksonConfig {

    /**
     * Rejeita números e booleanos onde o contrato espera texto ({@code "bio": 123} ou {@code "username": true}).
     * Por padrão o Jackson os converteria em texto e a requisição seria aceita.
     */
    @Bean
    JsonMapperBuilderCustomizer strictTextualCoercion() {
        return builder -> builder.withCoercionConfig(LogicalType.Textual, config -> config
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }
}
