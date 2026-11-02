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

/**
 * Perfil público (autenticação opcional) e seguir/deixar de seguir (autenticação obrigatória).
 */
@RestController
@RequestMapping("/api/profiles/{username}")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    /**
     * @param jwt token do usuário autenticado, ou {@code null} em consulta anônima
     */
    @GetMapping
    public PerfilResponse consultar(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        Perfil perfil = jwt == null
                ? perfilService.consultar(username)
                : perfilService.consultar(username, usuarioId(jwt));
        return PerfilResponse.de(perfil);
    }

    @PostMapping("/follow")
    public PerfilResponse seguir(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        return PerfilResponse.de(perfilService.seguir(usuarioId(jwt), username));
    }

    @DeleteMapping("/follow")
    public PerfilResponse deixarDeSeguir(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        return PerfilResponse.de(perfilService.deixarDeSeguir(usuarioId(jwt), username));
    }

    private static Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
