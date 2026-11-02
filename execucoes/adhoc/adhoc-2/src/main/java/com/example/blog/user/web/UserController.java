package com.example.blog.user.web;

import java.net.URI;

import jakarta.validation.Valid;

import com.example.blog.shared.security.AuthenticatedUser;
import com.example.blog.shared.web.ApiMediaTypes;
import com.example.blog.user.service.UserAccount;
import com.example.blog.user.service.UserService;
import com.example.blog.user.service.UserSession;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro, login e manutenção do usuário autenticado. */
@RestController
@RequestMapping(path = "/api", produces = ApiMediaTypes.JSON_UTF8_VALUE)
class UserController {

    private static final URI CURRENT_USER_LOCATION = URI.create("/api/user");

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    ResponseEntity<UserResponse> register(@Valid @RequestBody NewUserRequest request) {
        UserSession session = userService.register(request.user().toRegistration());
        return ResponseEntity.created(CURRENT_USER_LOCATION).body(toResponse(session));
    }

    @PostMapping("/users/login")
    UserResponse login(@Valid @RequestBody LoginRequest request) {
        return toResponse(userService.login(request.user().toCredentials()));
    }

    @GetMapping("/user")
    UserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser authenticated = AuthenticatedUser.from(jwt);
        UserAccount account = userService.findById(authenticated.id());
        return new UserResponse(UserBody.from(account, authenticated.token()));
    }

    @PutMapping("/user")
    UserResponse updateCurrentUser(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateUserRequest request) {
        AuthenticatedUser authenticated = AuthenticatedUser.from(jwt);
        UserAccount account = userService.update(authenticated.id(), request.user().toChanges());
        return new UserResponse(UserBody.from(account, authenticated.token()));
    }

    private static UserResponse toResponse(UserSession session) {
        return new UserResponse(UserBody.from(session.account(), session.token()));
    }
}
