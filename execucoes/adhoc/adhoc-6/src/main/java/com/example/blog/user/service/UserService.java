package com.example.blog.user.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.shared.error.ConflictException;
import com.example.blog.shared.error.ValidationException;
import com.example.blog.shared.error.ValidationMessages;
import com.example.blog.shared.security.TokenService;
import com.example.blog.user.persistence.UserAccount;
import com.example.blog.user.persistence.UserAccountRepository;

/**
 * Registration, login and maintenance of the authenticated user.
 */
@Service
public class UserService {

    /** BCrypt only uses the first 72 bytes of a password; longer ones are rejected instead of truncated. */
    static final int MAX_PASSWORD_BYTES = 72;

    private static final String EMAIL_UNIQUE_CONSTRAINT = "uq_user_account_email";
    private static final String USERNAME_UNIQUE_CONSTRAINT = "uq_user_account_username";

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;
    private final String dummyPasswordHash;

    public UserService(UserAccountRepository users, PasswordEncoder passwordEncoder, TokenService tokenService,
            Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
        // Used to spend the same time on logins with unknown emails, so they can't be told apart by timing.
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-unknown-emails");
    }

    /**
     * Registers a new user and issues a token for it.
     *
     * @throws ValidationException if the password is too long
     * @throws ConflictException if the username or the email is already taken
     */
    @Transactional
    public AuthResult register(NewUser newUser) {
        String email = normalizeEmail(newUser.email());
        validatePassword(newUser.password());
        Map<String, List<String>> conflicts = new LinkedHashMap<>();
        if (users.existsByUsername(newUser.username())) {
            conflicts.put("username", List.of(ValidationMessages.TAKEN));
        }
        if (users.existsByEmail(email)) {
            conflicts.put("email", List.of(ValidationMessages.TAKEN));
        }
        if (!conflicts.isEmpty()) {
            throw new ConflictException(conflicts);
        }
        UserAccount user = new UserAccount(newUser.username(), email,
                passwordEncoder.encode(newUser.password()), clock.instant());
        saveAndFlush(user);
        return new AuthResult(UserData.from(user), tokenService.issueFor(user.getId()));
    }

    /**
     * Checks the credentials and issues a token.
     *
     * @throws InvalidCredentialsException if the email is unknown or the password does not match
     */
    @Transactional(readOnly = true)
    public AuthResult login(String email, String password) {
        Optional<UserAccount> found = users.findByEmail(normalizeEmail(email));
        if (found.isEmpty()) {
            passwordEncoder.matches(password, dummyPasswordHash);
            throw new InvalidCredentialsException();
        }
        UserAccount user = found.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return new AuthResult(UserData.from(user), tokenService.issueFor(user.getId()));
    }

    /**
     * @throws CurrentUserNotFoundException if the user of the token no longer exists
     */
    @Transactional(readOnly = true)
    public UserData getCurrentUser(long userId) {
        return UserData.from(findCurrentUser(userId));
    }

    /**
     * Applies a partial update to the current user.
     *
     * @throws ValidationException if no field was sent or the password is too long
     * @throws ConflictException if the new username or email belongs to another user
     * @throws CurrentUserNotFoundException if the user of the token no longer exists
     */
    @Transactional
    public UserData updateCurrentUser(long userId, UserChanges changes) {
        if (changes.isEmpty()) {
            throw new ValidationException("user", ValidationMessages.EMPTY);
        }
        UserAccount user = findCurrentUser(userId);
        String newEmail = changes.email() == null ? null : normalizeEmail(changes.email());
        // Check every conflict before changing the entity: a query issued after a change would flush it first.
        Map<String, List<String>> conflicts = new LinkedHashMap<>();
        if (changes.username() != null && !changes.username().equals(user.getUsername())
                && users.existsByUsername(changes.username())) {
            conflicts.put("username", List.of(ValidationMessages.TAKEN));
        }
        if (newEmail != null && !newEmail.equals(user.getEmail()) && users.existsByEmail(newEmail)) {
            conflicts.put("email", List.of(ValidationMessages.TAKEN));
        }
        if (!conflicts.isEmpty()) {
            throw new ConflictException(conflicts);
        }
        if (changes.password() != null) {
            validatePassword(changes.password());
            user.changePasswordHash(passwordEncoder.encode(changes.password()));
        }
        if (changes.username() != null) {
            user.changeUsername(changes.username());
        }
        if (newEmail != null) {
            user.changeEmail(newEmail);
        }
        if (changes.bioProvided()) {
            user.changeBio(blankToNull(changes.bio()));
        }
        if (changes.imageProvided()) {
            user.changeImage(blankToNull(changes.image()));
        }
        user.markUpdated(clock.instant());
        saveAndFlush(user);
        return UserData.from(user);
    }

    private UserAccount findCurrentUser(long userId) {
        return users.findById(userId).orElseThrow(CurrentUserNotFoundException::new);
    }

    /** Flushes so that a unique violation from a concurrent request becomes a 409 instead of a 500. */
    private void saveAndFlush(UserAccount user) {
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            String detail = String.valueOf(exception.getMostSpecificCause().getMessage());
            if (detail.contains(USERNAME_UNIQUE_CONSTRAINT)) {
                throw new ConflictException(Map.of("username", List.of(ValidationMessages.TAKEN)));
            }
            if (detail.contains(EMAIL_UNIQUE_CONSTRAINT)) {
                throw new ConflictException(Map.of("email", List.of(ValidationMessages.TAKEN)));
            }
            throw exception;
        }
    }

    private static void validatePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ValidationException("password",
                    ValidationMessages.TOO_LONG + " (maximum is " + MAX_PASSWORD_BYTES + " bytes)");
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
