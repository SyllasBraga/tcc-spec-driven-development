package com.example.blog.user.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alteração parcial do usuário (schema {@code UpdateUser}). Campos ausentes ficam {@code null} e não mudam.
 * <p>
 * Não é um record porque é preciso distinguir campo ausente de {@code null} explícito: o Jackson só chama os
 * setters para as chaves presentes no JSON, e cada setter converte o {@code null} explícito em texto vazio.
 * Assim, em {@code email}, {@code username} e {@code password} (que o contrato não permite nulos) o
 * {@code null} é rejeitado como "can't be blank"; em {@code bio} e {@code image} (que aceitam {@code null})
 * ele remove o valor atual.
 */
public final class UpdateUser {

    @Pattern(regexp = ValidationMessages.NOT_BLANK_REGEX, message = ValidationMessages.BLANK)
    @Email(message = ValidationMessages.INVALID)
    @Size(max = 254, message = ValidationMessages.TOO_LONG)
    private String email;

    @Pattern(regexp = ValidationMessages.NOT_BLANK_REGEX, message = ValidationMessages.BLANK)
    @Size(max = 100, message = ValidationMessages.TOO_LONG)
    private String username;

    @Pattern(regexp = ValidationMessages.NOT_BLANK_REGEX, message = ValidationMessages.BLANK)
    private String password;

    private String bio;

    @Size(max = 2048, message = ValidationMessages.TOO_LONG)
    private String image;

    public String email() {
        return email;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public String bio() {
        return bio;
    }

    public String image() {
        return image;
    }

    public void setEmail(String email) {
        this.email = presentValue(email);
    }

    public void setUsername(String username) {
        this.username = presentValue(username);
    }

    public void setPassword(String password) {
        this.password = presentValue(password);
    }

    public void setBio(String bio) {
        this.bio = presentValue(bio);
    }

    public void setImage(String image) {
        this.image = presentValue(image);
    }

    /** Campo presente no JSON: {@code null} explícito vira texto vazio, para não se confundir com ausente. */
    private static String presentValue(String value) {
        return value == null ? "" : value;
    }
}
