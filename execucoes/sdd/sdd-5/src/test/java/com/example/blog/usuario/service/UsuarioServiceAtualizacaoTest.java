package com.example.blog.usuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.blog.shared.erro.TokenInvalidoException;
import com.example.blog.shared.erro.ValidacaoException;
import com.example.blog.shared.seguranca.TokenService;
import com.example.blog.usuario.persistence.Usuario;
import com.example.blog.usuario.persistence.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceAtualizacaoTest {

    private static final Instant CRIADO = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant AGORA = Instant.parse("2026-11-01T12:00:00Z");

    @Mock
    private UsuarioRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;

    private UsuarioService servico;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        servico = new UsuarioService(repository, passwordEncoder, tokenService, Clock.fixed(AGORA, ZoneOffset.UTC));
        usuario = UsuarioServiceCadastroTest.comId(new Usuario("jake", "jake@jake.jake", "{bcrypt}velho", CRIADO), 5L);
    }

    @Test
    void deveAlterarSomenteOsCamposPresentes() {
        when(repository.findById(5L)).thenReturn(Optional.of(usuario));
        when(repository.saveAndFlush(usuario)).thenReturn(usuario);
        when(tokenService.emitir(5L)).thenReturn("tok");

        UsuarioAutenticadoDto dto = servico.atualizar(5L, atualizacao(
                CampoAtualizado.ausente(), CampoAtualizado.ausente(), CampoAtualizado.ausente(),
                CampoAtualizado.de("Nova bio"), CampoAtualizado.ausente()));

        assertThat(dto).isEqualTo(new UsuarioAutenticadoDto(5L, "jake@jake.jake", "jake", "Nova bio", null, "tok"));
        assertThat(usuario.getSenhaHash()).isEqualTo("{bcrypt}velho");
        assertThat(usuario.getAtualizadoEm()).isEqualTo(AGORA);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void deveRecodificarSenhaELimparBio() {
        usuario.alterarBio("antiga");
        when(repository.findById(5L)).thenReturn(Optional.of(usuario));
        when(repository.saveAndFlush(usuario)).thenReturn(usuario);
        when(passwordEncoder.encode("nova")).thenReturn("{bcrypt}novo");

        servico.atualizar(5L, atualizacao(CampoAtualizado.ausente(), CampoAtualizado.ausente(),
                CampoAtualizado.de("nova"), CampoAtualizado.de(null), CampoAtualizado.ausente()));

        assertThat(usuario.getSenhaHash()).isEqualTo("{bcrypt}novo");
        assertThat(usuario.getBio()).isNull();
    }

    @Test
    void deveRejeitarEmailEUsernameDeOutraConta() {
        when(repository.findById(5L)).thenReturn(Optional.of(usuario));
        when(repository.existsByEmailIgnoreCaseAndIdNot("outro@jake.jake", 5L)).thenReturn(true);
        when(repository.existsByUsernameAndIdNot("outro", 5L)).thenReturn(true);

        assertThatThrownBy(() -> servico.atualizar(5L, atualizacao(CampoAtualizado.de("outro@jake.jake"),
                CampoAtualizado.de("outro"), CampoAtualizado.ausente(), CampoAtualizado.ausente(),
                CampoAtualizado.ausente())))
                .isInstanceOfSatisfying(ValidacaoException.class, excecao -> assertThat(excecao.getErros())
                        .isEqualTo(Map.of("email", List.of("has already been taken"),
                                "username", List.of("has already been taken"))));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveLancarTokenInvalidoQuandoUsuarioNaoExiste() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.atualizar(5L, atualizacao(CampoAtualizado.ausente(),
                CampoAtualizado.ausente(), CampoAtualizado.ausente(), CampoAtualizado.de("x"),
                CampoAtualizado.ausente())))
                .isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    void deveLancarTokenInvalidoAoBuscarUsuarioInexistente() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.buscarAtual(9L)).isInstanceOf(TokenInvalidoException.class);
    }

    private static AtualizacaoUsuario atualizacao(CampoAtualizado<String> email, CampoAtualizado<String> username,
            CampoAtualizado<String> senha, CampoAtualizado<String> bio, CampoAtualizado<String> imagem) {
        return new AtualizacaoUsuario(email, username, senha, bio, imagem);
    }
}
