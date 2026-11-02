package com.example.blog.shared.seguranca;

import java.time.Clock;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Configuração de segurança da API: autenticação sem estado por JWT (HS256) no cabeçalho
 * {@code Authorization: Token <jwt>}, senhas com {@link PasswordEncoder} delegante (BCrypt) e rotas públicas
 * explícitas. Todas as demais rotas exigem autenticação.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class SegurancaConfig {

	private static final String ALGORITMO_HMAC = "HmacSHA256";

	private static final int TAMANHO_MINIMO_CHAVE_BYTES = 32;

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	SecretKey chaveJwt(JwtProperties propriedades) {
		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(propriedades.secret());
		}
		catch (IllegalArgumentException excecao) {
			throw new IllegalStateException("conduit.jwt.secret must be Base64 encoded", excecao);
		}
		if (bytes.length < TAMANHO_MINIMO_CHAVE_BYTES) {
			throw new IllegalStateException(
					"conduit.jwt.secret must have at least " + TAMANHO_MINIMO_CHAVE_BYTES + " bytes");
		}
		return new SecretKeySpec(bytes, ALGORITMO_HMAC);
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey chaveJwt) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey chaveJwt, JwtProperties propriedades) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(propriedades.issuer()));
		return decoder;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ConduitTokenResolver tokenResolver,
			ErroSegurancaHandler erroSegurancaHandler) throws Exception {
		http.csrf(csrf -> csrf.disable())
			.sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(rotas -> rotas
				.requestMatchers(HttpMethod.POST, "/api/users", "/api/users/login").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/profiles/*").permitAll()
				.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
				.requestMatchers("/error").permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer(recurso -> recurso.bearerTokenResolver(tokenResolver)
				.authenticationEntryPoint(erroSegurancaHandler)
				.accessDeniedHandler(erroSegurancaHandler)
				.jwt(Customizer.withDefaults()))
			.exceptionHandling(excecoes -> excecoes.authenticationEntryPoint(erroSegurancaHandler)
				.accessDeniedHandler(erroSegurancaHandler));
		return http.build();
	}

}
