package com.example.blog.user.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.example.blog.shared.error.UnprocessableException;
import com.example.blog.shared.error.ValidationMessages;
import com.example.blog.user.service.UserChanges;

/**
 * Alterações do usuário recebidas na API. Todos os campos são opcionais.
 *
 * <p>É uma classe com setters, e não um record, porque {@code bio} e {@code image} precisam distinguir o campo
 * ausente (não alterar) do campo enviado com {@code null} (remover o valor). Já {@code email}, {@code username} e
 * {@code password} não aceitam {@code null}: podem ser omitidos, mas não anulados.
 */
public class UpdateUser {

    private static final String NOT_BLANK_REGEX = "(?s).*\\S.*";

    @Pattern(regexp = NOT_BLANK_REGEX, message = ValidationMessages.BLANK)
    @Email(message = ValidationMessages.INVALID)
    @Size(max = UserFieldLimits.EMAIL, message = ValidationMessages.TOO_LONG)
    private String email;

    @Pattern(regexp = NOT_BLANK_REGEX, message = ValidationMessages.BLANK)
    @Size(max = UserFieldLimits.USERNAME, message = ValidationMessages.TOO_LONG)
    private String username;

    @Pattern(regexp = NOT_BLANK_REGEX, message = ValidationMessages.BLANK)
    @Size(max = UserFieldLimits.PASSWORD, message = ValidationMessages.TOO_LONG)
    private String password;

    private String bio;
    private boolean bioPresent;

    private String image;
    private boolean imagePresent;

    /** Primeiro campo não anulável recebido com {@code null} explícito, se houver. */
    private String explicitNullField;

    public void setEmail(String email) {
        this.email = email;
        recordIfNull("email", email);
    }

    public void setUsername(String username) {
        this.username = username;
        recordIfNull("username", username);
    }

    public void setPassword(String password) {
        this.password = password;
        recordIfNull("password", password);
    }

    public void setBio(String bio) {
        this.bio = bio;
        this.bioPresent = true;
    }

    public void setImage(String image) {
        this.image = image;
        this.imagePresent = true;
    }

    /**
     * Converte para as alterações do serviço.
     *
     * @throws UnprocessableException se {@code email}, {@code username} ou {@code password} vierem com {@code null}
     */
    UserChanges toChanges() {
        if (explicitNullField != null) {
            throw new UnprocessableException(explicitNullField, ValidationMessages.BLANK);
        }
        return new UserChanges(email, username, password, bioPresent, bio, imagePresent, image);
    }

    private void recordIfNull(String field, String value) {
        if (value == null && explicitNullField == null) {
            explicitNullField = field;
        }
    }
}
