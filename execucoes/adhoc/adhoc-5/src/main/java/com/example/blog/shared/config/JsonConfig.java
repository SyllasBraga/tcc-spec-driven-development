package com.example.blog.shared.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * Tipagem estrita do JSON de entrada: campos declarados como {@code string} no contrato não aceitam números,
 * booleanos, objetos nem listas. Sem isso o Jackson converteria {@code 123} em {@code "123"} silenciosamente.
 */
@Configuration(proxyBeanMethods = false)
public class JsonConfig {

    @Bean
    JsonMapperBuilderCustomizer strictTextCoercion() {
        return builder -> builder.withCoercionConfig(LogicalType.Textual, config -> config
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Object, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Array, CoercionAction.Fail));
    }
}
