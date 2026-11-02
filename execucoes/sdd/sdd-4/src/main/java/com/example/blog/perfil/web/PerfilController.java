package com.example.blog.perfil.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.perfil.service.Perfil;
import com.example.blog.perfil.service.PerfilService;
import com.example.blog.shared.seguranca.TokenService;

/** Perfis públicos e seguir/deixar de seguir. */
@RestController
@RequestMapping("/api/profiles")
public class PerfilController {

    private final PerfilService perfilService;
    private final TokenService tokenService;

    public PerfilController(PerfilService perfilService, TokenService tokenService) {
        this.perfilService = perfilService;
        this.tokenService = tokenService;
    }

    /** Autenticação opcional: {@code jwt} é {@code null} em requisições anônimas. */
    @GetMapping("/{username}")
    public PerfilResponse buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable String username) {
        Perfil perfil = jwt == null
                ? perfilService.buscar(username)
                : perfilService.buscarComo(tokenService.idDoUsuario(jwt), username);
        return PerfilResponse.de(perfil);
    }

    @PostMapping("/{username}/follow")
    public PerfilResponse seguir(@AuthenticationPrincipal Jwt jwt, @PathVariable String username) {
        return PerfilResponse.de(perfilService.seguir(tokenService.idDoUsuario(jwt), username));
    }

    @DeleteMapping("/{username}/follow")
    public PerfilResponse deixarDeSeguir(@AuthenticationPrincipal Jwt jwt, @PathVariable String username) {
        return PerfilResponse.de(perfilService.deixarDeSeguir(tokenService.idDoUsuario(jwt), username));
    }
}
