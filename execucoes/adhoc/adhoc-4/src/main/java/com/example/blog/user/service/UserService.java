package com.example.blog.user.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.function.Function;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.shared.error.ApiException;
import com.example.blog.shared.error.ConflictException;
import com.example.blog.shared.error.UnauthorizedException;
import com.example.blog.shared.error.UnprocessableEntityException;
import com.example.blog.shared.security.AuthenticatedUser;
import com.example.blog.shared.security.TokenService;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;

/** Cadastro, login, consulta e atualização do usuário autenticado. */
@Service
public class UserService {

    /** Limite do BCrypt: bytes além deste são ignorados ou rejeitados pelo algoritmo. */
    static final int MAX_PASSWORD_BYTES = 72;

    private static final String INVALID_CREDENTIALS_FIELD = "credentials";
    private static final String INVALID_CREDENTIALS_DETAIL = "invalid";
    private static final String UQ_USERNAME = "uq_users_username";
    private static final String UQ_EMAIL = "uq_users_email";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;
    /** Hash usado quando o e-mail não existe, para o login levar o mesmo tempo nos dois casos. */
    private final String dummyPasswordHash;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
            Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("conduit-dummy-password");
    }

    /**
     * Cadastra um usuário e devolve seus dados com um token novo.
     *
     * @throws ConflictException se o nome de usuário ou o e-mail já estiverem em uso
     * @throws UnprocessableEntityException se a senha exceder o limite suportado
     */
    @Transactional
    public UserResult register(RegisterUserCommand command) {
        String username = command.username().strip();
        String email = normalizeEmail(command.email());
        requireSupportedPassword(command.password());
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("username");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("email");
        }
        User user = new User(username, email, passwordEncoder.encode(command.password()), clock.instant());
        saveTranslatingUniqueViolations(user, ConflictException::new);
        return toResult(user, tokenService.issueToken(user.getId()));
    }

    /**
     * Autentica por e-mail e senha.
     *
     * @throws UnauthorizedException se as credenciais não conferirem
     */
    @Transactional(readOnly = true)
    public UserResult login(String email, String password) {
        User user = userRepository.findByEmail(normalizeEmail(email)).orElse(null);
        String hash = user == null ? dummyPasswordHash : user.getPasswordHash();
        boolean matches = passwordEncoder.matches(password, hash);
        if (user == null || !matches) {
            throw new UnauthorizedException(INVALID_CREDENTIALS_FIELD, INVALID_CREDENTIALS_DETAIL);
        }
        return toResult(user, tokenService.issueToken(user.getId()));
    }

    /** Devolve o usuário dono do token, repetindo o token apresentado. */
    @Transactional(readOnly = true)
    public UserResult getCurrentUser(AuthenticatedUser authenticated) {
        return toResult(loadAuthenticated(authenticated), authenticated.token());
    }

    /**
     * Atualiza parcialmente o usuário dono do token.
     *
     * @throws UnprocessableEntityException se nenhum campo for informado ou se o nome de usuário ou o e-mail
     *         já pertencerem a outra conta
     */
    @Transactional
    public UserResult updateCurrentUser(AuthenticatedUser authenticated, UpdateUserCommand command) {
        if (command.isEmpty()) {
            throw new UnprocessableEntityException("user", "must have at least one field");
        }
        User user = loadAuthenticated(authenticated);
        Instant now = clock.instant();
        if (command.username() != null) {
            String username = command.username().strip();
            if (!username.equals(user.getUsername())) {
                if (userRepository.existsByUsername(username)) {
                    throw new UnprocessableEntityException("username", ConflictException.ALREADY_TAKEN);
                }
                user.changeUsername(username, now);
            }
        }
        if (command.email() != null) {
            String email = normalizeEmail(command.email());
            if (!email.equals(user.getEmail())) {
                if (userRepository.existsByEmail(email)) {
                    throw new UnprocessableEntityException("email", ConflictException.ALREADY_TAKEN);
                }
                user.changeEmail(email, now);
            }
        }
        if (command.password() != null) {
            requireSupportedPassword(command.password());
            user.changePasswordHash(passwordEncoder.encode(command.password()), now);
        }
        if (command.bio() != null) {
            user.changeBio(emptyToNull(command.bio()), now);
        }
        if (command.image() != null) {
            user.changeImage(emptyToNull(command.image().strip()), now);
        }
        saveTranslatingUniqueViolations(user,
                field -> new UnprocessableEntityException(field, ConflictException.ALREADY_TAKEN));
        return toResult(user, authenticated.token());
    }

    private User loadAuthenticated(AuthenticatedUser authenticated) {
        // Token válido de um usuário que não existe mais (por exemplo, banco recriado).
        return userRepository.findById(authenticated.id())
                .orElseThrow(() -> new UnauthorizedException("token", "is invalid"));
    }

    /** Grava e converte violações de unicidade concorrentes (corrida entre a checagem e o insert). */
    private void saveTranslatingUniqueViolations(User user, Function<String, ApiException> errorForField) {
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            String constraint = exception.getCause() instanceof ConstraintViolationException violation
                    ? violation.getConstraintName()
                    : null;
            if (UQ_USERNAME.equalsIgnoreCase(constraint)) {
                throw errorForField.apply("username");
            }
            if (UQ_EMAIL.equalsIgnoreCase(constraint)) {
                throw errorForField.apply("email");
            }
            throw exception;
        }
    }

    private static void requireSupportedPassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new UnprocessableEntityException("password",
                    "is too long (maximum is " + MAX_PASSWORD_BYTES + " bytes)");
        }
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private static UserResult toResult(User user, String token) {
        return new UserResult(user.getEmail(), token, user.getUsername(), user.getBio(), user.getImage());
    }
}
