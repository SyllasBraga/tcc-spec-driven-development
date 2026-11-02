package com.example.blog.shared.seguranca;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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

/**
 * Segurança da API: sem estado, autenticação por JWT (HS256) no cabeçalho {@code Authorization: Token <jwt>}.
 * Rotas públicas são listadas explicitamente; as demais exigem autenticação.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TokenProperties.class)
public class SegurancaConfig {

    private static final String ALGORITMO_HMAC = "HmacSHA256";

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(TokenProperties propriedades) {
        return NimbusJwtEncoder.withSecretKey(chaveSecreta(propriedades)).build();
    }

    @Bean
    JwtDecoder jwtDecoder(TokenProperties propriedades) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveSecreta(propriedades))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(propriedades.emissor()));
        return decoder;
    }

    @Bean
    TokenHeaderResolver tokenHeaderResolver() {
        return new TokenHeaderResolver();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, TokenHeaderResolver tokenHeaderResolver,
            ErroAutenticacaoHandler erroAutenticacaoHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, "/api/users", "/api/users/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/profiles/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(Customizer.withDefaults())
                        .bearerTokenResolver(tokenHeaderResolver)
                        .authenticationEntryPoint(erroAutenticacaoHandler))
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint(erroAutenticacaoHandler)
                        .accessDeniedHandler(erroAutenticacaoHandler));
        return http.build();
    }

    private static SecretKey chaveSecreta(TokenProperties propriedades) {
        return new SecretKeySpec(propriedades.segredo().getBytes(StandardCharsets.UTF_8), ALGORITMO_HMAC);
    }
}
