package com.example.blog.user.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import com.example.blog.shared.error.ConflictException;
import com.example.blog.shared.error.UnauthorizedException;
import com.example.blog.shared.error.UnprocessableException;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro, autenticação e manutenção dos dados do próprio usuário.
 */
@Service
public class UserService {

    /** Limite do BCrypt: bytes além de 72 seriam ignorados (ou rejeitados) pelo algoritmo. */
    private static final int MAXIMO_BYTES_SENHA = 72;
    private static final String RESTRICAO_EMAIL_UNICO = "uq_users_email";
    private static final String RESTRICAO_USERNAME_UNICO = "uq_users_username";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Cadastra um novo usuário.
     *
     * @throws ConflictException      se o e-mail ou o nome de usuário já estiverem em uso
     * @throws UnprocessableException se a senha exceder o limite suportado
     */
    @Transactional
    public UserData register(String username, String email, String password) {
        String normalizedUsername = username.strip();
        String normalizedEmail = normalizeEmail(email);
        validatePasswordLength(password);
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ConflictException("username");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("email");
        }
        User user = new User(normalizedUsername, normalizedEmail, passwordEncoder.encode(password), clock.instant());
        return UserData.from(saveAndFlush(user));
    }

    /**
     * Confere as credenciais e devolve o usuário correspondente.
     *
     * @throws UnauthorizedException se o e-mail não existir ou a senha não conferir
     */
    @Transactional(readOnly = true)
    public UserData login(String email, String password) {
        return userRepository.findByEmail(normalizeEmail(email))
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .map(UserData::from)
                .orElseThrow(() -> new UnauthorizedException("credentials", "invalid"));
    }

    /**
     * Busca o usuário autenticado.
     *
     * @throws UnauthorizedException se o usuário do token não existir mais
     */
    @Transactional(readOnly = true)
    public UserData getById(long id) {
        return UserData.from(findAuthenticated(id));
    }

    /**
     * Aplica uma alteração parcial ao usuário autenticado.
     *
     * @throws UnprocessableException se nenhum campo for informado ou a senha exceder o limite
     * @throws ConflictException      se o novo e-mail ou nome de usuário já pertencerem a outro usuário
     */
    @Transactional
    public UserData update(long id, UserUpdate update) {
        if (update.isEmpty()) {
            throw new UnprocessableException("user", "must have at least one field");
        }
        User user = findAuthenticated(id);
        Instant now = clock.instant();
        if (update.username() != null) {
            String newUsername = update.username().strip();
            if (!newUsername.equals(user.getUsername())) {
                if (userRepository.existsByUsername(newUsername)) {
                    throw new ConflictException("username");
                }
                user.changeUsername(newUsername, now);
            }
        }
        if (update.email() != null) {
            String newEmail = normalizeEmail(update.email());
            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new ConflictException("email");
                }
                user.changeEmail(newEmail, now);
            }
        }
        if (update.password() != null) {
            validatePasswordLength(update.password());
            user.changePasswordHash(passwordEncoder.encode(update.password()), now);
        }
        if (update.bio() != null) {
            user.changeBio(emptyToNull(update.bio()), now);
        }
        if (update.image() != null) {
            user.changeImage(emptyToNull(update.image()), now);
        }
        return UserData.from(saveAndFlush(user));
    }

    private User findAuthenticated(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException("token", "is invalid"));
    }

    /**
     * Grava e força o flush para que uma violação de unicidade concorrente (entre a verificação e a gravação)
     * vire 409 em vez de erro interno.
     */
    private User saveAndFlush(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            String detail = String.valueOf(NestedExceptionUtils.getMostSpecificCause(exception).getMessage());
            if (detail.contains(RESTRICAO_EMAIL_UNICO)) {
                throw new ConflictException("email");
            }
            if (detail.contains(RESTRICAO_USERNAME_UNICO)) {
                throw new ConflictException("username");
            }
            throw exception;
        }
    }

    private static void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES_SENHA) {
            throw new UnprocessableException("password", "is too long (maximum is 72 bytes)");
        }
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static String emptyToNull(String value) {
        return value.isBlank() ? null : value.strip();
    }
}
