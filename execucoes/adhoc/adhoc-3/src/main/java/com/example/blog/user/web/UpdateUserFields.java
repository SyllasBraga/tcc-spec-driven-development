package com.example.blog.user.web;

import static com.example.blog.shared.error.ValidationMessages.BLANK;
import static com.example.blog.shared.error.ValidationMessages.INVALID;
import static com.example.blog.shared.error.ValidationMessages.NOT_BLANK_PATTERN;
import static com.example.blog.shared.error.ValidationMessages.TOO_LONG;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Campos aceitos em {@code PUT /api/user}, todos opcionais.
 *
 * <p>Não é um record porque precisa distinguir campo ausente de campo enviado como {@code null}:
 * {@code "bio": null} apaga a bio, enquanto omitir {@code bio} a mantém. Já {@code email},
 * {@code username} e {@code password} não aceitam {@code null}: omiti-los mantém o valor, e
 * enviá-los como {@code null} é recusado com 422.
 */
public final class UpdateUserFields {

    @Email(message = INVALID)
    @Pattern(regexp = NOT_BLANK_PATTERN, message = BLANK)
    @Size(max = 254, message = TOO_LONG)
    private String email;

    @Pattern(regexp = NOT_BLANK_PATTERN, message = BLANK)
    @Size(max = 64, message = TOO_LONG)
    private String username;

    @Pattern(regexp = NOT_BLANK_PATTERN, message = BLANK)
    private String password;

    private String bio;
    private boolean bioPresent;

    private String image;
    private boolean imagePresent;

    /** Campos não anuláveis que vieram explicitamente como {@code null}, na ordem de chegada. */
    private final List<String> explicitNullFields = new ArrayList<>();

    @JsonProperty("email")
    void setEmail(String email) {
        this.email = email;
        trackExplicitNull("email", email);
    }

    @JsonProperty("username")
    void setUsername(String username) {
        this.username = username;
        trackExplicitNull("username", username);
    }

    @JsonProperty("password")
    void setPassword(String password) {
        this.password = password;
        trackExplicitNull("password", password);
    }

    @JsonProperty("bio")
    void setBio(String bio) {
        this.bio = bio;
        this.bioPresent = true;
    }

    @JsonProperty("image")
    void setImage(String image) {
        this.image = image;
        this.imagePresent = true;
    }

    String getEmail() {
        return email;
    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }

    String getBio() {
        return bio;
    }

    boolean isBioPresent() {
        return bioPresent;
    }

    String getImage() {
        return image;
    }

    boolean isImagePresent() {
        return imagePresent;
    }

    List<String> explicitNullFields() {
        return List.copyOf(explicitNullFields);
    }

    private void trackExplicitNull(String field, String value) {
        if (value == null) {
            explicitNullFields.add(field);
        }
    }
}
