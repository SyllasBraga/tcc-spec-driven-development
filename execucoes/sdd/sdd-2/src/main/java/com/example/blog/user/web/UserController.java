package com.example.blog.user.web;

import com.example.blog.shared.security.CurrentUserId;
import com.example.blog.user.service.AuthenticatedUser;
import com.example.blog.user.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Cadastro, login e conta do usuário autenticado.
 */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final CurrentUserId currentUserId;

    public UserController(UserService userService, CurrentUserId currentUserId) {
        this.userService = userService;
        this.currentUserId = currentUserId;
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> cadastrar(@Valid @RequestBody RegisterUserRequest request) {
        RegisterUserRequest.NewUserFields campos = request.user();
        AuthenticatedUser usuario = userService.cadastrar(campos.username(), campos.email(), campos.password());
        URI perfil = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/profiles/{username}")
                .buildAndExpand(usuario.username())
                .encode()
                .toUri();
        return ResponseEntity.created(perfil).body(UserResponse.from(usuario));
    }

    @PostMapping("/users/login")
    public UserResponse autenticar(@Valid @RequestBody LoginRequest request) {
        return UserResponse.from(userService.autenticar(request.user().email(), request.user().password()));
    }

    @GetMapping("/user")
    public UserResponse buscarAtual() {
        return UserResponse.from(userService.buscarAtual(currentUserId.exigir()));
    }

    @PutMapping("/user")
    public UserResponse atualizar(@RequestBody UpdateUserRequest request) {
        return UserResponse.from(userService.atualizar(currentUserId.exigir(), request.toUserUpdate()));
    }
}
