package com.example.blog.user.service;

import java.util.ArrayList;
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
import com.example.blog.shared.error.ErrorMessages;
import com.example.blog.shared.error.UnauthorizedException;
import com.example.blog.shared.error.ValidationException;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;

/**
 * Cadastro, autenticação e atualização de usuários.
 */
@Service
public class UserService {

    private static final String DUMMY_PASSWORD = "senha-ficticia-para-tempo-constante";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(DUMMY_PASSWORD);
    }

    /**
     * Cadastra um usuário.
     *
     * @throws ConflictException se o nome de usuário ou o e-mail já estiverem em uso
     */
    @Transactional
    public UserData register(Registration registration) {
        String username = normalizeUsername(registration.username());
        String email = normalizeEmail(registration.email());

        Map<String, List<String>> errors = new LinkedHashMap<>();
        if (userRepository.existsByUsername(username)) {
            addError(errors, "username", ErrorMessages.TAKEN);
        }
        if (userRepository.existsByEmail(email)) {
            addError(errors, "email", ErrorMessages.TAKEN);
        }
        if (!errors.isEmpty()) {
            throw new ConflictException(errors);
        }

        User user = new User(username, email, passwordEncoder.encode(registration.password()));
        try {
            return UserData.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            // Outro cadastro concorrente com o mesmo nome ou e-mail passou pela verificação acima.
            throw new ConflictException("username or email", ErrorMessages.TAKEN);
        }
    }

    /**
     * Confere e-mail e senha.
     *
     * @throws UnauthorizedException se o e-mail não existir ou a senha não conferir
     */
    @Transactional(readOnly = true)
    public UserData authenticate(String email, String password) {
        Optional<User> user = userRepository.findByEmail(normalizeEmail(email));
        // Compara a senha mesmo sem usuário, para o tempo de resposta não revelar quais e-mails existem.
        String passwordHash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(password, passwordHash);
        if (user.isEmpty() || !passwordMatches) {
            throw new UnauthorizedException("credentials", ErrorMessages.INVALID_CREDENTIALS);
        }
        return UserData.from(user.get());
    }

    /**
     * Busca o usuário autenticado.
     *
     * @throws UnauthorizedException se o usuário do token não existir mais
     */
    @Transactional(readOnly = true)
    public UserData getAuthenticatedUser(long userId) {
        return UserData.from(findAuthenticatedUser(userId));
    }

    /**
     * Aplica as alterações enviadas ao usuário autenticado.
     *
     * @throws ValidationException se nenhuma alteração for enviada ou se o nome ou o e-mail já estiverem em uso
     */
    @Transactional
    public UserData update(long userId, UserChanges changes) {
        if (changes.isEmpty()) {
            throw new ValidationException("user", "must have at least one field");
        }
        User user = findAuthenticatedUser(userId);

        String username = changes.username() == null ? null : normalizeUsername(changes.username());
        String email = changes.email() == null ? null : normalizeEmail(changes.email());

        // Todas as verificações vêm antes de qualquer alteração na entidade, para o flush automático
        // das consultas abaixo não gravar um valor duplicado.
        Map<String, List<String>> errors = new LinkedHashMap<>();
        if (username != null && userRepository.existsByUsernameAndIdNot(username, userId)) {
            addError(errors, "username", ErrorMessages.TAKEN);
        }
        if (email != null && userRepository.existsByEmailAndIdNot(email, userId)) {
            addError(errors, "email", ErrorMessages.TAKEN);
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        if (username != null) {
            user.changeUsername(username);
        }
        if (email != null) {
            user.changeEmail(email);
        }
        if (changes.password() != null) {
            user.changePasswordHash(passwordEncoder.encode(changes.password()));
        }
        if (changes.bio() != null) {
            user.changeBio(blankToNull(changes.bio().value()));
        }
        if (changes.image() != null) {
            user.changeImage(blankToNull(changes.image().value()));
        }
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            // Outra atualização concorrente ocupou o mesmo nome ou e-mail depois da verificação acima.
            throw new ValidationException("username or email", ErrorMessages.TAKEN);
        }
        return UserData.from(user);
    }

    private User findAuthenticatedUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("token", ErrorMessages.INVALID));
    }

    private static String normalizeUsername(String username) {
        return username.strip();
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    /**
     * Bio e imagem em branco equivalem a "sem valor" e são gravadas como {@code null}.
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void addError(Map<String, List<String>> errors, String key, String message) {
        errors.computeIfAbsent(key, ignored -> new ArrayList<>()).add(message);
    }
}
