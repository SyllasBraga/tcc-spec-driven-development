package com.example.blog.usuario.web;

import com.example.blog.shared.seguranca.UsuarioAutenticado;
import com.example.blog.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cadastro, login e manutenção da conta do usuário autenticado.
 */
@RestController
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AtualizacaoUsuarioMapper atualizacaoMapper;

    public UsuarioController(UsuarioService usuarioService, AtualizacaoUsuarioMapper atualizacaoMapper) {
        this.usuarioService = usuarioService;
        this.atualizacaoMapper = atualizacaoMapper;
    }

    @PostMapping("/api/users")
    public ResponseEntity<UsuarioEnvelope> cadastrar(@Valid @RequestBody CadastroRequest requisicao) {
        NovoUsuario novo = requisicao.user();
        var criado = usuarioService.cadastrar(novo.username(), novo.email(), novo.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioEnvelope.de(criado));
    }

    @PostMapping("/api/users/login")
    public UsuarioEnvelope autenticar(@Valid @RequestBody LoginRequest requisicao) {
        Credenciais credenciais = requisicao.user();
        return UsuarioEnvelope.de(usuarioService.autenticar(credenciais.email(), credenciais.password()));
    }

    @GetMapping("/api/user")
    public UsuarioEnvelope buscarAtual(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioEnvelope.de(usuarioService.buscarAtual(UsuarioAutenticado.id(jwt)));
    }

    @PutMapping("/api/user")
    public UsuarioEnvelope atualizar(@AuthenticationPrincipal Jwt jwt, @RequestBody AtualizacaoRequest requisicao) {
        Long id = UsuarioAutenticado.id(jwt);
        return UsuarioEnvelope.de(usuarioService.atualizar(id, atualizacaoMapper.converter(requisicao)));
    }
}
