package com.example.blog.usuario.web;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.shared.seguranca.TokenService;
import com.example.blog.usuario.service.UsuarioAutenticado;
import com.example.blog.usuario.service.UsuarioService;

/**
 * Cadastro, login, consulta e atualização do usuário atual.
 */
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class UsuarioController {

	private static final URI LOCAL_USUARIO_ATUAL = URI.create("/api/user");

	private final UsuarioService usuarioService;

	private final AtualizacaoUsuarioMapper atualizacaoMapper;

	private final TokenService tokenService;

	public UsuarioController(UsuarioService usuarioService, AtualizacaoUsuarioMapper atualizacaoMapper,
			TokenService tokenService) {
		this.usuarioService = usuarioService;
		this.atualizacaoMapper = atualizacaoMapper;
		this.tokenService = tokenService;
	}

	@PostMapping("/users")
	public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
		CadastroRequest.Dados dados = request.user();
		UsuarioAutenticado usuario = usuarioService.cadastrar(dados.username(), dados.email(), dados.password());
		return ResponseEntity.created(LOCAL_USUARIO_ATUAL).body(UsuarioResponse.de(usuario));
	}

	@PostMapping("/users/login")
	public UsuarioResponse autenticar(@Valid @RequestBody LoginRequest request) {
		LoginRequest.Dados dados = request.user();
		return UsuarioResponse.de(usuarioService.autenticar(dados.email(), dados.password()));
	}

	@GetMapping("/user")
	public UsuarioResponse consultarAtual(@AuthenticationPrincipal Jwt jwt) {
		return UsuarioResponse.de(usuarioService.consultarAtual(tokenService.usuarioId(jwt)));
	}

	@PutMapping("/user")
	public UsuarioResponse atualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AtualizacaoRequest request) {
		return UsuarioResponse.de(usuarioService.atualizar(tokenService.usuarioId(jwt),
				atualizacaoMapper.converter(request.user())));
	}

}
