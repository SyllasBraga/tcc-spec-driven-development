package com.example.blog.usuario.web;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.usuario.service.UsuarioAutenticado;
import com.example.blog.usuario.service.UsuarioService;

/**
 * Cadastro e login.
 */
@RestController
@RequestMapping("/api/users")
public class UsuarioController {

    private static final URI LOCAL_USUARIO_ATUAL = URI.create("/api/user");

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        RegistroRequest.UsuarioNovo novo = request.user();
        UsuarioAutenticado usuario = usuarioService.registrar(novo.username(), novo.email(), novo.password());
        return ResponseEntity.created(LOCAL_USUARIO_ATUAL).body(UsuarioResponse.de(usuario));
    }

    @PostMapping("/login")
    public UsuarioResponse entrar(@Valid @RequestBody LoginRequest request) {
        LoginRequest.Credenciais credenciais = request.user();
        return UsuarioResponse.de(usuarioService.entrar(credenciais.email(), credenciais.password()));
    }
}
