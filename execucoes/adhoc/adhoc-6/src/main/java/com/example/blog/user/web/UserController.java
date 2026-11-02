package com.example.blog.user.web;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.blog.shared.security.AuthenticatedUser;
import com.example.blog.user.service.AuthResult;
import com.example.blog.user.service.NewUser;
import com.example.blog.user.service.UserData;
import com.example.blog.user.service.UserService;

/**
 * Registration, login and the authenticated user ({@code /api/users}, {@code /api/user}).
 */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        NewUserPayload payload = request.user();
        AuthResult result = userService.register(
                new NewUser(payload.username(), payload.email(), payload.password()));
        URI location = UriComponentsBuilder.fromPath("/api/profiles/{username}")
                .buildAndExpand(result.user().username())
                .encode()
                .toUri();
        return ResponseEntity.created(location).body(toResponse(result.user(), result.token()));
    }

    @PostMapping("/users/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = userService.login(request.user().email(), request.user().password());
        return toResponse(result.user(), result.token());
    }

    @GetMapping("/user")
    public UserResponse currentUser(@AuthenticationPrincipal AuthenticatedUser principal) {
        return toResponse(userService.getCurrentUser(principal.id()), principal.token());
    }

    @PutMapping("/user")
    public UserResponse updateCurrentUser(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateUserRequest request) {
        UserData updated = userService.updateCurrentUser(principal.id(), request.user().toChanges());
        return toResponse(updated, principal.token());
    }

    private static UserResponse toResponse(UserData user, String token) {
        return new UserResponse(UserView.of(user, token));
    }
}
