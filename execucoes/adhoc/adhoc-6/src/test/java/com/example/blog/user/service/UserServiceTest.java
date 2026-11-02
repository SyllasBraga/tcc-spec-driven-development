package com.example.blog.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.blog.shared.error.ConflictException;
import com.example.blog.shared.error.ValidationException;
import com.example.blog.shared.security.TokenService;
import com.example.blog.user.persistence.UserAccount;
import com.example.blog.user.persistence.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private UserAccountRepository users;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(users, passwordEncoder, tokenService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void registerReportsEveryTakenField() {
        when(users.existsByUsername("jake")).thenReturn(true);
        when(users.existsByEmail("jake@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(new NewUser("jake", "jake@example.com", "secret")))
                .isInstanceOfSatisfying(ConflictException.class, exception -> assertThat(exception.getErrors())
                        .isEqualTo(Map.of("username", List.of("has already been taken"),
                                "email", List.of("has already been taken"))));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void registerNormalizesEmailBeforeCheckingUniqueness() {
        when(users.existsByEmail("jake@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(new NewUser("jake", "  Jake@Example.COM ", "secret")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void registerRejectsPasswordLongerThan72Bytes() {
        String password = "é".repeat(37);

        assertThatThrownBy(() -> userService.register(new NewUser("jake", "jake@example.com", password)))
                .isInstanceOfSatisfying(ValidationException.class,
                        exception -> assertThat(exception.getErrors()).containsKey("password"));
    }

    @Test
    void loginWithUnknownEmailFailsWithInvalidCredentials() {
        when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login("nobody@example.com", "secret"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginWithWrongPasswordFailsWithInvalidCredentials() {
        UserAccount user = new UserAccount("jake", "jake@example.com", "hash", NOW);
        when(users.findByEmail("jake@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.login("jake@example.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(tokenService, never()).issueFor(anyLong());
    }

    @Test
    void updateWithoutFieldsIsRejected() {
        UserChanges noChanges = new UserChanges(null, null, null, false, null, false, null);

        assertThatThrownBy(() -> userService.updateCurrentUser(1, noChanges))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateRejectsEmailOfAnotherUserWithoutChangingTheEntity() {
        UserAccount user = new UserAccount("jake", "jake@example.com", "hash", NOW);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.existsByEmail("taken@example.com")).thenReturn(true);
        UserChanges changes = new UserChanges("taken@example.com", null, null, false, null, false, null);

        assertThatThrownBy(() -> userService.updateCurrentUser(1, changes))
                .isInstanceOf(ConflictException.class);
        assertThat(user.getEmail()).isEqualTo("jake@example.com");
    }

    @Test
    void updateClearsBioAndImageSentAsNullOrBlank() {
        UserAccount user = persistedUser();
        user.changeBio("old bio");
        user.changeImage("https://example.com/old.png");
        when(users.findById(1L)).thenReturn(Optional.of(user));
        UserChanges changes = new UserChanges(null, null, null, true, null, true, "  ");

        userService.updateCurrentUser(1, changes);

        assertThat(user.getBio()).isNull();
        assertThat(user.getImage()).isNull();
    }

    private static UserAccount persistedUser() {
        UserAccount user = new UserAccount("jake", "jake@example.com", "hash", NOW);
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
