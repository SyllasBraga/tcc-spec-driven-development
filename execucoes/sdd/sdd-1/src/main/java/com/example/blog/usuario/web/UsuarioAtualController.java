package com.example.blog.usuario.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.usuario.service.AtualizacaoUsuario;
import com.example.blog.usuario.service.UsuarioService;

/**
 * Consulta e atualização do usuário autenticado.
 */
@RestController
@RequestMapping("/api/user")
public class UsuarioAtualController {

    private final UsuarioService usuarioService;
    private final AtualizacaoUsuarioMapper mapper;

    public UsuarioAtualController(UsuarioService usuarioService, AtualizacaoUsuarioMapper mapper) {
        this.usuarioService = usuarioService;
        this.mapper = mapper;
    }

    @GetMapping
    public UsuarioResponse buscar(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.de(usuarioService.buscarAtual(usuarioId(jwt), jwt.getTokenValue()));
    }

    @PutMapping
    public UsuarioResponse atualizar(@AuthenticationPrincipal Jwt jwt, @RequestBody AtualizacaoUsuarioRequest request) {
        AtualizacaoUsuario comando = mapper.converter(request.user());
        return UsuarioResponse.de(usuarioService.atualizar(usuarioId(jwt), jwt.getTokenValue(), comando));
    }

    private static Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
