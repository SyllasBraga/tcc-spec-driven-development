package com.example.blog.usuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.blog.shared.erro.CredenciaisInvalidasException;
import com.example.blog.shared.seguranca.TokenService;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceLoginTest {

    @Mock
    private UsuarioRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;

    private UsuarioService servico;

    @BeforeEach
    void criarServico() {
        servico = new UsuarioService(repository, passwordEncoder, tokenService, Clock.systemUTC());
    }

    @Test
    void deveDevolverUsuarioComTokenQuandoSenhaConfere() {
        Usuario usuario = UsuarioServiceCadastroTest.comId(
                new Usuario("jake", "jake@jake.jake", "{bcrypt}hash", Instant.EPOCH), 3L);
        when(repository.findByEmailIgnoreCase("jake@jake.jake")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("jakejake", "{bcrypt}hash")).thenReturn(true);
        when(tokenService.emitir(3L)).thenReturn("tok");

        UsuarioAutenticadoDto dto = servico.autenticar(" jake@jake.jake ", "jakejake");

        assertThat(dto).isEqualTo(new UsuarioAutenticadoDto(3L, "jake@jake.jake", "jake", null, null, "tok"));
    }

    @Test
    void deveRecusarUsuarioInexistente() {
        when(repository.findByEmailIgnoreCase("ninguem@jake.jake")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.autenticar("ninguem@jake.jake", "jakejake"))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void deveRecusarSenhaQueNaoConfere() {
        Usuario usuario = new Usuario("jake", "jake@jake.jake", "{bcrypt}hash", Instant.EPOCH);
        when(repository.findByEmailIgnoreCase("jake@jake.jake")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "{bcrypt}hash")).thenReturn(false);

        assertThatThrownBy(() -> servico.autenticar("jake@jake.jake", "errada"))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }
}
