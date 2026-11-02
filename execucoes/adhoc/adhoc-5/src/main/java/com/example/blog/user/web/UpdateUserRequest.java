package com.example.blog.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.example.blog.shared.error.UnprocessableException;
import com.example.blog.user.service.UserUpdate;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Corpo de {@code PUT /api/user}. Todos os campos são opcionais; campo ausente não é alterado.
 * Em {@code bio} e {@code image}, {@code null} ou texto vazio removem o valor atual. Em {@code email},
 * {@code username} e {@code password}, {@code null} explícito é rejeitado.
 */
public record UpdateUserRequest(@NotNull(message = ValidationMessages.BLANK) @Valid Changes user) {

    /** Representação do texto vazio que, para o serviço, significa "remover o valor". */
    private static final String REMOVER = "";

    /**
     * Campos enviados pelo cliente. Não é um record porque precisa distinguir campo ausente de {@code null}
     * explícito: os setters só são chamados pelo Jackson para as chaves presentes no JSON.
     */
    public static final class Changes {

        @Pattern(regexp = ValidationMessages.NOT_BLANK_PATTERN, message = ValidationMessages.BLANK)
        @Size(max = 254, message = ValidationMessages.EMAIL_TOO_LONG)
        @Email(regexp = ValidationMessages.EMAIL_DOMAIN_PATTERN, message = ValidationMessages.INVALID)
        private String email;
        private boolean hasEmail;

        @Pattern(regexp = ValidationMessages.NOT_BLANK_PATTERN, message = ValidationMessages.BLANK)
        @Size(max = 50, message = ValidationMessages.USERNAME_TOO_LONG)
        @Pattern(regexp = ValidationMessages.USERNAME_PATTERN, message = ValidationMessages.INVALID)
        private String username;
        private boolean hasUsername;

        @Pattern(regexp = ValidationMessages.NOT_BLANK_PATTERN, message = ValidationMessages.BLANK)
        private String password;
        private boolean hasPassword;

        private String bio;
        private boolean hasBio;

        private String image;
        private boolean hasImage;

        @JsonSetter("email")
        void setEmail(String email) {
            this.email = email;
            this.hasEmail = true;
        }

        @JsonSetter("username")
        void setUsername(String username) {
            this.username = username;
            this.hasUsername = true;
        }

        @JsonSetter("password")
        void setPassword(String password) {
            this.password = password;
            this.hasPassword = true;
        }

        @JsonSetter("bio")
        void setBio(String bio) {
            this.bio = bio;
            this.hasBio = true;
        }

        @JsonSetter("image")
        void setImage(String image) {
            this.image = image;
            this.hasImage = true;
        }

        /**
         * Converte para a alteração do serviço.
         *
         * @throws UnprocessableException se {@code email}, {@code username} ou {@code password} vierem como
         *                                {@code null} explícito
         */
        UserUpdate toUserUpdate() {
            rejectExplicitNull("email", hasEmail, email);
            rejectExplicitNull("username", hasUsername, username);
            rejectExplicitNull("password", hasPassword, password);
            return new UserUpdate(email, username, password, clearableValue(hasBio, bio),
                    clearableValue(hasImage, image));
        }

        private static void rejectExplicitNull(String field, boolean present, String value) {
            if (present && value == null) {
                throw new UnprocessableException(field, ValidationMessages.BLANK);
            }
        }

        private static String clearableValue(boolean present, String value) {
            if (!present) {
                return null;
            }
            return value == null ? REMOVER : value;
        }
    }
}
