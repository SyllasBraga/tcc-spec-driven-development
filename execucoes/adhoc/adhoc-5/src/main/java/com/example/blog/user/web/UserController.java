package com.example.blog.user.web;

import java.net.URI;

import jakarta.validation.Valid;

import com.example.blog.shared.security.AuthenticatedUser;
import com.example.blog.shared.security.TokenService;
import com.example.blog.user.service.UserData;
import com.example.blog.user.service.UserService;
import com.example.blog.user.service.UserUpdate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro, login e dados do usuário autenticado. */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;

    public UserController(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @PostMapping("/users")
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        RegisterUserRequest.NewUser user = request.user();
        UserData created = userService.register(user.username(), user.email(), user.password());
        return ResponseEntity.created(URI.create("/api/user"))
                .body(UserResponse.of(created, tokenService.issue(created.id())));
    }

    @PostMapping("/users/login")
    UserResponse login(@Valid @RequestBody LoginRequest request) {
        UserData user = userService.login(request.user().email(), request.user().password());
        return UserResponse.of(user, tokenService.issue(user.id()));
    }

    @GetMapping("/user")
    UserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser authenticated = AuthenticatedUser.from(jwt);
        return UserResponse.of(userService.getById(authenticated.id()), authenticated.token());
    }

    @PutMapping("/user")
    UserResponse updateCurrentUser(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateUserRequest request) {
        AuthenticatedUser authenticated = AuthenticatedUser.from(jwt);
        UserUpdate update = request.user().toUserUpdate();
        return UserResponse.of(userService.update(authenticated.id(), update), authenticated.token());
    }
}
