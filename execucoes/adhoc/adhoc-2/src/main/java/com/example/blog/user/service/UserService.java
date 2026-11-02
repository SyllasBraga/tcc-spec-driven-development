package com.example.blog.user.service;

import java.time.Clock;
import java.util.Locale;
import java.util.Optional;

import com.example.blog.shared.error.ConflictException;
import com.example.blog.shared.error.NotFoundException;
import com.example.blog.shared.error.UnprocessableException;
import com.example.blog.shared.error.ValidationMessages;
import com.example.blog.shared.security.TokenService;
import com.example.blog.user.persistence.User;
import com.example.blog.user.persistence.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro, login, consulta e atualização do usuário. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;
    /** Hash usado quando o e-mail não existe, para que o login leve o mesmo tempo nos dois casos. */
    private final String dummyPasswordHash;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
            Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    /**
     * Cadastra um usuário e emite o token de acesso.
     *
     * @throws ConflictException se o e-mail ou o nome de usuário já estiverem em uso
     */
    @Transactional
    public UserSession register(Registration registration) {
        String email = normalizeEmail(registration.email());
        String username = registration.username().strip();
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("username", ValidationMessages.TAKEN);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("email", ValidationMessages.TAKEN);
        }
        User user = new User(email, username, passwordEncoder.encode(registration.password()), clock.instant());
        userRepository.saveAndFlush(user);
        return new UserSession(UserAccount.from(user), tokenService.issue(user.getId()));
    }

    /**
     * Autentica por e-mail e senha e emite o token de acesso.
     *
     * @throws InvalidCredentialsException se o e-mail não existir ou a senha estiver incorreta
     */
    @Transactional(readOnly = true)
    public UserSession login(Credentials credentials) {
        Optional<User> user = userRepository.findByEmail(normalizeEmail(credentials.email()));
        String storedHash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(credentials.password(), storedHash);
        return user.filter(found -> passwordMatches)
                .map(found -> new UserSession(UserAccount.from(found), tokenService.issue(found.getId())))
                .orElseThrow(InvalidCredentialsException::new);
    }

    /**
     * Busca o usuário pelo id.
     *
     * @throws NotFoundException se o usuário não existir
     */
    @Transactional(readOnly = true)
    public UserAccount findById(long userId) {
        return UserAccount.from(loadUser(userId));
    }

    /**
     * Aplica as alterações pedidas ao usuário.
     *
     * @throws UnprocessableException se nenhum campo for informado
     * @throws ConflictException se o novo e-mail ou nome de usuário já pertencer a outro usuário
     */
    @Transactional
    public UserAccount update(long userId, UserChanges changes) {
        if (changes.isEmpty()) {
            throw new UnprocessableException("user", "must have at least one field");
        }
        User user = loadUser(userId);
        if (changes.email() != null) {
            String email = normalizeEmail(changes.email());
            if (userRepository.existsByEmailAndIdNot(email, userId)) {
                throw new ConflictException("email", ValidationMessages.TAKEN);
            }
            user.changeEmail(email);
        }
        if (changes.username() != null) {
            String username = changes.username().strip();
            if (userRepository.existsByUsernameAndIdNot(username, userId)) {
                throw new ConflictException("username", ValidationMessages.TAKEN);
            }
            user.changeUsername(username);
        }
        if (changes.password() != null) {
            user.changePasswordHash(passwordEncoder.encode(changes.password()));
        }
        if (changes.bioPresent()) {
            user.changeBio(changes.bio());
        }
        if (changes.imagePresent()) {
            user.changeImage(changes.image());
        }
        user.markUpdated(clock.instant());
        userRepository.flush();
        return UserAccount.from(user);
    }

    private User loadUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user", ValidationMessages.NOT_FOUND));
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
