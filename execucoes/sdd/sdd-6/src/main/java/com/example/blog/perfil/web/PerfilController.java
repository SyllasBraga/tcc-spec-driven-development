package com.example.blog.perfil.web;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.perfil.service.PerfilService;
import com.example.blog.shared.seguranca.TokenService;

/**
 * Perfis públicos e relação de seguir. A consulta aceita requisições anônimas; seguir e deixar de seguir exigem
 * autenticação (configurada na {@code SecurityFilterChain}).
 */
@RestController
@RequestMapping(path = "/api/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
public class PerfilController {

	private final PerfilService perfilService;

	private final TokenService tokenService;

	public PerfilController(PerfilService perfilService, TokenService tokenService) {
		this.perfilService = perfilService;
		this.tokenService = tokenService;
	}

	@GetMapping("/{username}")
	public PerfilResponse consultar(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
		Long leitorId = jwt != null ? tokenService.usuarioId(jwt) : null;
		return PerfilResponse.de(perfilService.consultar(username, leitorId));
	}

	@PostMapping("/{username}/follow")
	public PerfilResponse seguir(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
		return PerfilResponse.de(perfilService.seguir(username, tokenService.usuarioId(jwt)));
	}

	@DeleteMapping("/{username}/follow")
	public PerfilResponse deixarDeSeguir(@PathVariable String username, @AuthenticationPrincipal Jwt jwt) {
		return PerfilResponse.de(perfilService.deixarDeSeguir(username, tokenService.usuarioId(jwt)));
	}

}
