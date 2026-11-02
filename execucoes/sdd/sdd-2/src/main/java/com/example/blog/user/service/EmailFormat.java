package com.example.blog.user.service;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import org.springframework.stereotype.Component;

/**
 * Verifica o formato de e-mail com a mesma regra do {@code @Email} do Bean Validation usado no cadastro,
 * para que cadastro e atualização aceitem exatamente os mesmos endereços.
 */
@Component
public class EmailFormat {

    private final Validator validator;

    public EmailFormat(Validator validator) {
        this.validator = validator;
    }

    public boolean isValido(String email) {
        return validator.validateValue(EmailCandidato.class, "valor", email).isEmpty();
    }

    private record EmailCandidato(@Email String valor) {
    }
}
