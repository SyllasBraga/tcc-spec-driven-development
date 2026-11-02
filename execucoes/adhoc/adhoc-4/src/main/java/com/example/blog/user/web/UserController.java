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
import org.springframework.web.util.UriComponentsBuilder;

import com.example.blog.shared.security.AuthenticatedUser;
import com.example.blog.user.service.RegisterUserCommand;
import com.example.blog.user.service.UpdateUserCommand;
import com.example.blog.user.service.UserResult;
import com.example.blog.user.service.UserService;

/** Cadastro, login e conta do usuário autenticado. */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody NewUserRequest request) {
        NewUser user = request.user();
        UserResult result = userService.register(
                new RegisterUserCommand(user.username(), user.email(), user.password()));
        URI location = UriComponentsBuilder.fromPath("/api/profiles/{username}")
                .buildAndExpand(result.username())
                .encode()
                .toUri();
        return ResponseEntity.created(location).body(UserResponse.from(result));
    }

    @PostMapping("/users/login")
    public UserResponse login(@Valid @RequestBody LoginUserRequest request) {
        return UserResponse.from(userService.login(request.user().email(), request.user().password()));
    }

    @GetMapping("/user")
    public UserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(userService.getCurrentUser(AuthenticatedUser.from(jwt)));
    }

    @PutMapping("/user")
    public UserResponse updateCurrentUser(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserRequest request) {
        UpdateUser user = request.user();
        UpdateUserCommand command = new UpdateUserCommand(
                user.email(), user.username(), user.password(), user.bio(), user.image());
        return UserResponse.from(userService.updateCurrentUser(AuthenticatedUser.from(jwt), command));
    }
}
