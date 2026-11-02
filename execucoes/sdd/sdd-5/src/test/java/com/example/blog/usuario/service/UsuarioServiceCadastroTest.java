package com.example.blog.usuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.blog.shared.erro.ConflitoException;
import com.example.blog.shared.seguranca.TokenService;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceCadastroTest {

    private static final Instant AGORA = Instant.parse("2026-11-01T12:00:00Z");

    @Mock
    private UsuarioRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;

    private UsuarioService servico;

    @BeforeEach
    void criarServico() {
        servico = new UsuarioService(repository, passwordEncoder, tokenService, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    void deveGravarComSenhaCodificadaEDevolverToken() {
        when(passwordEncoder.encode("jakejake")).thenReturn("{bcrypt}hash");
        when(repository.saveAndFlush(any(Usuario.class))).thenAnswer(chamada -> comId(chamada.getArgument(0), 7L));
        when(tokenService.emitir(7L)).thenReturn("tok");

        UsuarioAutenticadoDto dto = servico.cadastrar(" jake ", " jake@jake.jake ", "jakejake");

        ArgumentCaptor<Usuario> gravado = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).saveAndFlush(gravado.capture());
        assertThat(gravado.getValue().getSenhaHash()).isEqualTo("{bcrypt}hash");
        assertThat(gravado.getValue().getUsername()).isEqualTo("jake");
        assertThat(gravado.getValue().getEmail()).isEqualTo("jake@jake.jake");
        assertThat(gravado.getValue().getCriadoEm()).isEqualTo(AGORA);
        assertThat(dto).isEqualTo(new UsuarioAutenticadoDto(7L, "jake@jake.jake", "jake", null, null, "tok"));
    }

    @Test
    void deveLancarConflitoComTodasAsChavesEmUso() {
        when(repository.existsByUsername("jake")).thenReturn(true);
        when(repository.existsByEmailIgnoreCase("jake@jake.jake")).thenReturn(true);

        assertThatThrownBy(() -> servico.cadastrar("jake", "jake@jake.jake", "jakejake"))
                .isInstanceOfSatisfying(ConflitoException.class, excecao -> assertThat(excecao.getErros())
                        .isEqualTo(Map.of("email", List.of("has already been taken"),
                                "username", List.of("has already been taken"))));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveLancarConflitoSoDoUsername() {
        when(repository.existsByUsername("jake")).thenReturn(true);

        assertThatThrownBy(() -> servico.cadastrar("jake", "jake@jake.jake", "jakejake"))
                .isInstanceOfSatisfying(ConflitoException.class, excecao -> assertThat(excecao.getErros())
                        .containsOnlyKeys("username"));
    }

    @Test
    void deveTraduzirViolacaoDeUnicidadeDoEmailPorCorrida() {
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}hash");
        when(repository.saveAndFlush(any(Usuario.class))).thenThrow(violacao("uq_usuario_email"));

        assertThatThrownBy(() -> servico.cadastrar("jake", "jake@jake.jake", "jakejake"))
                .isInstanceOfSatisfying(ConflitoException.class, excecao -> assertThat(excecao.getErros())
                        .isEqualTo(Map.of("email", List.of("has already been taken"))));
    }

    @Test
    void deveTraduzirViolacaoDeUnicidadeDoUsernamePorCorrida() {
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}hash");
        when(repository.saveAndFlush(any(Usuario.class))).thenThrow(violacao("uq_usuario_username"));

        assertThatThrownBy(() -> servico.cadastrar("jake", "jake@jake.jake", "jakejake"))
                .isInstanceOfSatisfying(ConflitoException.class, excecao -> assertThat(excecao.getErros())
                        .isEqualTo(Map.of("username", List.of("has already been taken"))));
    }

    static DataIntegrityViolationException violacao(String restricao) {
        var causa = new ConstraintViolationException("duplicado", new SQLException("duplicado"), restricao);
        return new DataIntegrityViolationException("violação", causa);
    }

    static Usuario comId(Usuario usuario, Long id) {
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
