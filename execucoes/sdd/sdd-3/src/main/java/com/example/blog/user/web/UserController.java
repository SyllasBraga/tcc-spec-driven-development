package com.example.blog.user.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.shared.security.UsuarioAtual;
import com.example.blog.shared.web.MediaTypes;
import com.example.blog.user.service.UserService;
import com.example.blog.user.service.UsuarioAutenticado;

/**
 * Cadastro, login e conta do usuário autenticado.
 */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final UsuarioAtual usuarioAtual;

    public UserController(UserService userService, UsuarioAtual usuarioAtual) {
        this.userService = userService;
        this.usuarioAtual = usuarioAtual;
    }

    @PostMapping(path = "/users", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaTypes.APPLICATION_JSON_UTF8)
    public ResponseEntity<UserResponse> cadastrar(@Valid @RequestBody NewUserRequest request) {
        UsuarioAutenticado usuario = userService.cadastrar(request.user().paraNovoUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta(usuario));
    }

    @PostMapping(path = "/users/login", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaTypes.APPLICATION_JSON_UTF8)
    public UserResponse entrar(@Valid @RequestBody LoginRequest request) {
        return resposta(userService.autenticar(request.user().paraCredenciais()));
    }

    @GetMapping(path = "/user", produces = MediaTypes.APPLICATION_JSON_UTF8)
    public UserResponse buscarAtual() {
        return resposta(userService.buscarAtual(usuarioAtual.idObrigatorio()));
    }

    @PutMapping(path = "/user", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaTypes.APPLICATION_JSON_UTF8)
    public UserResponse atualizar(@Valid @RequestBody UpdateUserRequest request) {
        return resposta(userService.atualizar(usuarioAtual.idObrigatorio(), request.user().paraAtualizacao()));
    }

    private static UserResponse resposta(UsuarioAutenticado usuario) {
        return new UserResponse(UserView.de(usuario));
    }
}
