package com.example.blog.shared.seguranca;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

/** Define {@code conduit.jwt.segredo} com valor aleatório gerado em tempo de execução; nenhum segredo fica versionado. */
@TestConfiguration(proxyBeanMethods = false)
public class SegredoJwtAleatorioConfiguration {

    private static final int BYTES_DO_SEGREDO = 48;

    @Bean
    DynamicPropertyRegistrar segredoJwtAleatorio() {
        byte[] bytes = new byte[BYTES_DO_SEGREDO];
        new SecureRandom().nextBytes(bytes);
        String segredo = Base64.getEncoder().encodeToString(bytes);
        return registry -> registry.add("conduit.jwt.segredo", () -> segredo);
    }
}
