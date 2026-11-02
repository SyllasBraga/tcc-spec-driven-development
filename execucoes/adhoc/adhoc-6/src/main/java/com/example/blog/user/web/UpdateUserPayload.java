package com.example.blog.user.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.example.blog.shared.error.ValidationException;
import com.example.blog.shared.error.ValidationMessages;
import com.example.blog.user.service.UserChanges;

/**
 * Fields of {@code PUT /api/user}. All are optional. A JSON {@code null} is rejected for email, username and
 * password (not nullable in the contract), but clears bio and image, so this class records which keys were present.
 */
public class UpdateUserPayload {

    private static final String NOT_BLANK = "(?s).*\\S.*";

    @Email(message = ValidationMessages.INVALID)
    @Pattern(regexp = NOT_BLANK, message = ValidationMessages.BLANK)
    @Size(max = 255, message = ValidationMessages.TOO_LONG + " (maximum is {max} characters)")
    private String email;

    @Pattern(regexp = NOT_BLANK, message = ValidationMessages.BLANK)
    @Size(max = 100, message = ValidationMessages.TOO_LONG + " (maximum is {max} characters)")
    private String username;

    @Pattern(regexp = NOT_BLANK, message = ValidationMessages.BLANK)
    private String password;

    private String bio;
    private boolean bioProvided;
    private String image;
    private boolean imageProvided;

    /** Email, username and password are not nullable in the contract, so an explicit null is rejected. */
    private final Set<String> explicitNulls = new TreeSet<>();

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
        this.bioProvided = true;
    }

    public void setImage(String image) {
        this.image = image;
        this.imageProvided = true;
    }

    /**
     * @throws ValidationException if email, username or password was sent as {@code null}
     */
    UserChanges toChanges() {
        if (!explicitNulls.isEmpty()) {
            Map<String, List<String>> errors = new LinkedHashMap<>();
            explicitNulls.forEach(field -> errors.put(field, List.of(ValidationMessages.BLANK)));
            throw new ValidationException(errors);
        }
        return new UserChanges(email, username, password, bioProvided, bio, imageProvided, image);
    }

    @Override
    public String toString() {
        return new UserChanges(email, username, password, bioProvided, bio, imageProvided, image).toString();
    }

    private void recordIfNull(String field, String value) {
        if (value == null) {
            explicitNulls.add(field);
        } else {
            explicitNulls.remove(field);
        }
    }
}
