package com.example.blog.shared.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Segurança da API: sem estado, JWT HS256 no cabeçalho {@code Authorization: Token <jwt>},
 * rotas públicas explícitas e autenticação exigida nas demais.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String ALGORITMO_CHAVE = "HmacSHA256";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        ErroAutenticacaoEntryPoint entryPoint = new ErroAutenticacaoEntryPoint();
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, "/api/users", "/api/users/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/profiles/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso
                        .bearerTokenResolver(new TokenAuthorizationResolver())
                        .authenticationEntryPoint(entryPoint)
                        .jwt(jwt -> jwt.decoder(jwtDecoder)))
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(new ErroAcessoNegadoHandler()));
        return http.build();
    }

    @Bean
    public SecretKey chaveJwt(JwtProperties propriedades) {
        return new SecretKeySpec(propriedades.secret().getBytes(StandardCharsets.UTF_8), ALGORITMO_CHAVE);
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    /**
     * Decodificador que valida assinatura HS256, expiração (sem tolerância) e emissor.
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey chaveJwt, JwtProperties propriedades, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveJwt)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator validadorDeTempo = new JwtTimestampValidator(Duration.ZERO);
        validadorDeTempo.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
                validadorDeTempo, new JwtIssuerValidator(propriedades.issuer())));
        return decoder;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
