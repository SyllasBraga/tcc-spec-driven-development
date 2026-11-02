package com.example.blog.usuario.web;

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

import com.example.blog.shared.seguranca.TokenService;
import com.example.blog.usuario.service.UsuarioAutenticado;
import com.example.blog.usuario.service.UsuarioService;

import tools.jackson.databind.JsonNode;

/** Cadastro, login e usuário atual. */
@RestController
@RequestMapping("/api")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final TokenService tokenService;
    private final AlteracaoUsuarioMapper alteracaoUsuarioMapper;

    public UsuarioController(UsuarioService usuarioService, TokenService tokenService,
            AlteracaoUsuarioMapper alteracaoUsuarioMapper) {
        this.usuarioService = usuarioService;
        this.tokenService = tokenService;
        this.alteracaoUsuarioMapper = alteracaoUsuarioMapper;
    }

    @PostMapping("/users")
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroRequest requisicao) {
        CadastroRequest.Dados dados = requisicao.user();
        UsuarioAutenticado usuario = usuarioService.cadastrar(dados.username(), dados.email(), dados.password());
        URI perfil = UriComponentsBuilder.fromPath("/api/profiles/{username}")
                .buildAndExpand(usuario.username())
                .encode()
                .toUri();
        return ResponseEntity.created(perfil).body(UsuarioResponse.de(usuario));
    }

    @PostMapping("/users/login")
    public UsuarioResponse entrar(@Valid @RequestBody LoginRequest requisicao) {
        LoginRequest.Dados dados = requisicao.user();
        return UsuarioResponse.de(usuarioService.autenticar(dados.email(), dados.password()));
    }

    @GetMapping("/user")
    public UsuarioResponse buscarAtual(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.de(usuarioService.buscarAtual(tokenService.idDoUsuario(jwt), jwt.getTokenValue()));
    }

    @PutMapping("/user")
    public UsuarioResponse atualizar(@AuthenticationPrincipal Jwt jwt, @RequestBody JsonNode corpo) {
        return UsuarioResponse.de(usuarioService.atualizar(tokenService.idDoUsuario(jwt), jwt.getTokenValue(),
                alteracaoUsuarioMapper.converter(corpo)));
    }
}
