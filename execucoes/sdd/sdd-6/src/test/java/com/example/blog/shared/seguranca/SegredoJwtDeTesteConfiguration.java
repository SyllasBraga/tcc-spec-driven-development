package com.example.blog.shared.seguranca;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

/**
 * Gera em tempo de execução um segredo JWT aleatório para os testes, sem versionar nenhum valor.
 */
@TestConfiguration(proxyBeanMethods = false)
public class SegredoJwtDeTesteConfiguration {

	private static final int TAMANHO_SEGREDO_BYTES = 32;

	@Bean
	DynamicPropertyRegistrar segredoJwtDeTeste() {
		return registry -> registry.add("conduit.jwt.secret", SegredoJwtDeTesteConfiguration::gerarSegredo);
	}

	public static String gerarSegredo() {
		byte[] bytes = new byte[TAMANHO_SEGREDO_BYTES];
		new SecureRandom().nextBytes(bytes);
		return Base64.getEncoder().encodeToString(bytes);
	}

}
