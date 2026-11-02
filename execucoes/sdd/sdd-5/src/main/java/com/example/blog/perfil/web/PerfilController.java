package com.example.blog.perfil.web;

import com.example.blog.perfil.service.PerfilService;
import com.example.blog.shared.seguranca.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Perfil público (autenticação opcional) e seguir/deixar de seguir (autenticação obrigatória).
 */
@RestController
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping("/api/profiles/{username}")
    public PerfilEnvelope buscar(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        return PerfilEnvelope.de(perfilService.buscarPerfil(username, UsuarioAutenticado.idOuNulo(jwt)));
    }

    @PostMapping("/api/profiles/{username}/follow")
    public PerfilEnvelope seguir(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        return PerfilEnvelope.de(perfilService.seguir(username, UsuarioAutenticado.id(jwt)));
    }

    @DeleteMapping("/api/profiles/{username}/follow")
    public PerfilEnvelope deixarDeSeguir(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
        return PerfilEnvelope.de(perfilService.deixarDeSeguir(username, UsuarioAutenticado.id(jwt)));
    }
}
