package com.example.blog.user.web;

import java.net.URI;

import jakarta.validation.Valid;

import com.example.blog.shared.security.AuthenticatedUser;
import com.example.blog.shared.security.TokenService;
import com.example.blog.user.service.UserData;
import com.example.blog.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro, login e manutenção do usuário autenticado. */
@RestController
@RequestMapping("/api")
public class UserController {

    private static final URI CURRENT_USER_LOCATION = URI.create("/api/user");

    private final UserService userService;
    private final TokenService tokenService;

    public UserController(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    // Location aponta para /api/user, onde o usuário criado é consultado com o token devolvido.
    @PostMapping("/users")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegistrationRequest request) {
        UserData user = userService.register(request.toRegistration());
        return ResponseEntity.created(CURRENT_USER_LOCATION).body(UserResponse.of(user, tokenService.issue(user.id())));
    }

    @PostMapping("/users/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request) {
        UserData user = userService.login(request.user().email(), request.user().password());
        return UserResponse.of(user, tokenService.issue(user.id()));
    }

    @GetMapping("/user")
    public UserResponse currentUser(AuthenticatedUser currentUser) {
        return UserResponse.of(userService.getById(currentUser.id()), currentUser.token());
    }

    @PutMapping("/user")
    public UserResponse updateCurrentUser(AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateUserRequest request) {
        UserData user = userService.update(currentUser.id(), request.toUserUpdate());
        return UserResponse.of(user, currentUser.token());
    }
}
