package com.example.blog.shared.seguranca;

import java.time.Clock;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.DispatcherType;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import tools.jackson.databind.json.JsonMapper;

/** Segurança da API: JWT HS256 no cabeçalho {@code Authorization: Token <jwt>}, sem sessão. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class SegurancaConfig {

    private static final String ALGORITMO_CHAVE = "HmacSHA256";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties propriedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave(propriedades)));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties propriedades) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave(propriedades))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(propriedades.emissor()));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper jsonMapper) {
        ErroAutenticacaoEntryPoint entryPoint = new ErroAutenticacaoEntryPoint(jsonMapper);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users", "/api/users/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/profiles/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso
                        .bearerTokenResolver(new TokenHeaderResolver())
                        .authenticationEntryPoint(entryPoint)
                        .jwt(jwt -> { }))
                .exceptionHandling(excecoes -> excecoes.authenticationEntryPoint(entryPoint));
        return http.build();
    }

    private static SecretKey chave(JwtProperties propriedades) {
        return new SecretKeySpec(propriedades.segredoEmBytes(), ALGORITMO_CHAVE);
    }
}
