package com.example.blog.user.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.example.blog.shared.error.ErrorMessages;
import com.example.blog.user.service.UserChanges;
import com.example.blog.user.service.ValueChange;

/**
 * Corpo de atualização do usuário. É uma classe com setters, e não um record, porque precisa distinguir
 * {@code "bio": null} (apagar a bio) de {@code bio} ausente (manter a bio): o Jackson só chama o setter
 * dos campos presentes no JSON. Campos ausentes de e-mail, nome e senha são ignorados; enviados com
 * {@code null}, são tratados como texto vazio e recusados pela validação de campo em branco.
 */
public class UpdateUser {

    private static final String NOT_BLANK_REGEX = "(?s).*\\S.*";

    @Pattern(regexp = NOT_BLANK_REGEX, message = ErrorMessages.BLANK)
    @Email(message = ErrorMessages.INVALID)
    @Size(max = UserLimits.EMAIL_MAX, message = ErrorMessages.TOO_LONG)
    private String email;

    @Pattern(regexp = NOT_BLANK_REGEX, message = ErrorMessages.BLANK)
    @Size(max = UserLimits.USERNAME_MAX, message = ErrorMessages.TOO_LONG)
    private String username;

    @Pattern(regexp = NOT_BLANK_REGEX, message = ErrorMessages.BLANK)
    @Size(min = UserLimits.PASSWORD_MIN, message = ErrorMessages.TOO_SHORT)
    @Size(max = UserLimits.PASSWORD_MAX, message = ErrorMessages.TOO_LONG)
    private String password;

    @Size(max = UserLimits.BIO_MAX, message = ErrorMessages.TOO_LONG)
    private String bio;

    @Size(max = UserLimits.IMAGE_MAX, message = ErrorMessages.TOO_LONG)
    private String image;

    private boolean bioPresent;
    private boolean imagePresent;

    public void setEmail(String email) {
        this.email = nullToEmpty(email);
    }

    public void setUsername(String username) {
        this.username = nullToEmpty(username);
    }

    public void setPassword(String password) {
        this.password = nullToEmpty(password);
    }

    public void setBio(String bio) {
        this.bio = bio;
        this.bioPresent = true;
    }

    public void setImage(String image) {
        this.image = image;
        this.imagePresent = true;
    }

    UserChanges toChanges() {
        return new UserChanges(
                email,
                username,
                password,
                bioPresent ? new ValueChange(bio) : null,
                imagePresent ? new ValueChange(image) : null);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
