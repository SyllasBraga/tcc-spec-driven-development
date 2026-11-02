package com.example.blog.shared.config;

import java.time.Clock;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Beans de infraestrutura compartilhados. */
@Configuration
@ConfigurationPropertiesScan("com.example.blog")
public class ApplicationConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
