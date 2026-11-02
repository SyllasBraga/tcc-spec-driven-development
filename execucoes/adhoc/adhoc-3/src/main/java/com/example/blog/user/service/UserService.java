package com.example.blog.user.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

import com.example.blog.shared.error.ConflictException;
import com.example.blog.shared.error.NotFoundException;
import com.example.blog.shared.error.UnauthorizedException;
import com.example.blog.shared.error.UnprocessableException;
import com.example.blog.user.persistence.AppUser;
import com.example.blog.user.persistence.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras de cadastro, login, consulta e atualização de usuários. */
@Service
public class UserService {

    /** Limite do BCrypt: bytes além de 72 seriam rejeitados pelo codificador. */
    static final int MAX_PASSWORD_BYTES = 72;

    static final String TAKEN = "has already been taken";

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Cadastra um usuário.
     *
     * @throws ConflictException      se o e-mail (sem diferenciar maiúsculas) ou o username já existirem
     * @throws UnprocessableException se a senha exceder o limite suportado
     */
    @Transactional
    public UserData register(Registration registration) {
        String email = registration.email().strip();
        String username = registration.username().strip();
        requireSupportedPassword(registration.password());
        if (userRepository.existsByEmailIgnoringCase(email)) {
            throw new ConflictException("email", TAKEN);
        }
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("username", TAKEN);
        }
        AppUser user = new AppUser(username, email, passwordEncoder.encode(registration.password()), clock.instant());
        AppUser saved = userRepository.saveAndFlush(user);
        log.info("Usuário {} cadastrado", saved.getId());
        return UserData.from(saved);
    }

    /**
     * Confere as credenciais e devolve o usuário correspondente.
     *
     * @throws UnauthorizedException com a chave {@code credentials} se o e-mail não existir ou a senha não
     *                               conferir (sem distinguir os casos)
     */
    @Transactional(readOnly = true)
    public UserData login(String email, String password) {
        return userRepository.findByEmailIgnoringCase(email.strip())
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .map(UserData::from)
                .orElseThrow(() -> new UnauthorizedException("credentials", "invalid"));
    }

    /** @throws NotFoundException se o usuário não existir */
    @Transactional(readOnly = true)
    public UserData getById(long userId) {
        return UserData.from(findUser(userId));
    }

    /**
     * Atualiza os campos informados do usuário.
     *
     * @throws UnprocessableException se nenhum campo for informado ou a senha exceder o limite
     * @throws ConflictException      se o novo e-mail ou username pertencer a outro usuário
     * @throws NotFoundException      se o usuário não existir
     */
    @Transactional
    public UserData update(long userId, UserUpdate update) {
        if (update.isEmpty()) {
            throw new UnprocessableException("user", "must have at least one field");
        }
        AppUser user = findUser(userId);
        if (update.email() != null) {
            String email = update.email().strip();
            if (userRepository.existsByEmailIgnoringCaseAndIdNot(email, userId)) {
                throw new ConflictException("email", TAKEN);
            }
            user.changeEmail(email);
        }
        if (update.username() != null) {
            String username = update.username().strip();
            if (userRepository.existsByUsernameAndIdNot(username, userId)) {
                throw new ConflictException("username", TAKEN);
            }
            user.changeUsername(username);
        }
        if (update.password() != null) {
            requireSupportedPassword(update.password());
            user.changePasswordHash(passwordEncoder.encode(update.password()));
        }
        if (update.bioPresent()) {
            user.changeBio(blankToNull(update.bio()));
        }
        if (update.imagePresent()) {
            user.changeImage(blankToNull(update.image()));
        }
        user.markUpdated(clock.instant());
        return UserData.from(userRepository.saveAndFlush(user));
    }

    private AppUser findUser(long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("user"));
    }

    /** Texto vazio ou só com espaços em campos opcionais equivale a "sem valor". */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void requireSupportedPassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new UnprocessableException("password", "is too long (maximum is 72 bytes)");
        }
    }
}
