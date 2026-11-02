package com.example.blog.user.web;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.blog.shared.security.AuthenticatedUser;
import com.example.blog.shared.security.TokenService;
import com.example.blog.shared.web.ApiMediaTypes;
import com.example.blog.user.service.UserData;
import com.example.blog.user.service.UserService;

/**
 * Cadastro, login e consulta e atualização do usuário autenticado.
 */
@RestController
@RequestMapping(path = "/api", produces = ApiMediaTypes.JSON)
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;

    public UserController(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        UserData user = userService.register(request.user().toRegistration());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/profiles/{username}")
                .build(user.username());
        return ResponseEntity.created(location).body(UserResponse.of(user, tokenService.issueToken(user.id())));
    }

    @PostMapping("/users/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request) {
        UserData user = userService.authenticate(request.user().email(), request.user().password());
        return UserResponse.of(user, tokenService.issueToken(user.id()));
    }

    @GetMapping("/user")
    public UserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        UserData user = userService.getAuthenticatedUser(AuthenticatedUser.idOf(jwt));
        return UserResponse.of(user, jwt.getTokenValue());
    }

    @PutMapping("/user")
    public UserResponse updateCurrentUser(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserRequest request) {
        UserData user = userService.update(AuthenticatedUser.idOf(jwt), request.user().toChanges());
        return UserResponse.of(user, jwt.getTokenValue());
    }
}
