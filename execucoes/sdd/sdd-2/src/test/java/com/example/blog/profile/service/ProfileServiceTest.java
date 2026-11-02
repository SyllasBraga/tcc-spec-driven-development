package com.example.blog.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.blog.profile.persistence.FollowId;
import com.example.blog.profile.persistence.FollowRepository;
import com.example.blog.shared.error.InvalidTokenException;
import com.example.blog.shared.error.NotFoundException;
import com.example.blog.shared.error.ValidationException;
import com.example.blog.user.persistence.UserEntity;
import com.example.blog.user.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final Instant AGORA = Instant.parse("2026-01-10T12:00:00Z");
    private static final Long ID_ANA = 1L;
    private static final Long ID_JAKE = 2L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowRepository followRepository;

    private ProfileService profileService;
    private UserEntity ana;

    @BeforeEach
    void preparar() {
        profileService = new ProfileService(userRepository, followRepository, Clock.fixed(AGORA, ZoneOffset.UTC));
        ana = new UserEntity("ana", "ana@exemplo.test", "hash", AGORA);
        ReflectionTestUtils.setField(ana, "id", ID_ANA);
        ana.alterarBio("bio da ana");
    }

    @Nested
    class BuscarPerfil {

        @Test
        void deveRetornarFollowingFalsoParaAnonimo() {
            when(userRepository.findByUsername("ana")).thenReturn(Optional.of(ana));

            Profile perfil = profileService.buscarPerfil("ana", null);

            assertThat(perfil).isEqualTo(new Profile("ana", "bio da ana", null, false));
            verify(followRepository, never()).existsById(any());
        }

        @Test
        void deveRetornarFollowingVerdadeiroQuandoQuemConsultaSegue() {
            when(userRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
            when(followRepository.existsById(new FollowId(ID_JAKE, ID_ANA))).thenReturn(true);

            assertThat(profileService.buscarPerfil("ana", ID_JAKE).following()).isTrue();
        }

        @Test
        void deveRetornarFollowingFalsoQuandoQuemConsultaNaoSegue() {
            when(userRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
            when(followRepository.existsById(new FollowId(ID_JAKE, ID_ANA))).thenReturn(false);

            assertThat(profileService.buscarPerfil("ana", ID_JAKE).following()).isFalse();
        }

        @Test
        void deveLancarNaoEncontradoParaNomeInexistente() {
            when(userRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.buscarPerfil("ninguem", null))
                    .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.body().errors())
                            .isEqualTo(Map.of("profile", List.of("not found"))));
        }

        @Test
        void deveDiferenciarMaiusculasNoNome() {
            when(userRepository.findByUsername("ANA")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.buscarPerfil("ANA", null)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Seguir {

        @Test
        void deveGravarRelacaoQuandoNaoExiste() {
            when(userRepository.existsById(ID_JAKE)).thenReturn(true);
            when(userRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
            when(followRepository.existsById(new FollowId(ID_JAKE, ID_ANA))).thenReturn(false);
            when(followRepository.inserirSeAusente(ID_JAKE, ID_ANA, AGORA)).thenReturn(1);

            Profile perfil = profileService.seguir("ana", ID_JAKE);

            assertThat(perfil).isEqualTo(new Profile("ana", "bio da ana", null, true));
            verify(followRepository).inserirSeAusente(ID_JAKE, ID_ANA, AGORA);
        }

        @Test
        void naoDeveGravarDeNovoQuandoJaSegue() {
            when(userRepository.existsById(ID_JAKE)).thenReturn(true);
            when(userRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
            when(followRepository.existsById(new FollowId(ID_JAKE, ID_ANA))).thenReturn(true);

            assertThat(profileService.seguir("ana", ID_JAKE).following()).isTrue();
            verify(followRepository, never()).inserirSeAusente(anyLong(), anyLong(), any());
        }

        @Test
        void deveTratarInsercaoConcorrenteComoSucesso() {
            when(userRepository.existsById(ID_JAKE)).thenReturn(true);
            when(userRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
            when(followRepository.existsById(new FollowId(ID_JAKE, ID_ANA))).thenReturn(false);
            when(followRepository.inserirSeAusente(ID_JAKE, ID_ANA, AGORA)).thenReturn(0);

            assertThat(profileService.seguir("ana", ID_JAKE).following()).isTrue();
        }

        @Test
        void deveLancarValidacaoAoSeguirASiMesmo() {
            when(userRepository.existsById(ID_ANA)).thenReturn(true);
            when(userRepository.findByUsername("ana")).thenReturn(Optional.of(ana));

            assertThatThrownBy(() -> profileService.seguir("ana", ID_ANA))
                    .isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.body().errors())
                            .isEqualTo(Map.of("profile", List.of("cannot follow yourself"))));
            verify(followRepository, never()).inserirSeAusente(anyLong(), anyLong(), any());
        }

        @Test
        void deveLancarNaoEncontradoQuandoAlvoNaoExiste() {
            when(userRepository.existsById(ID_JAKE)).thenReturn(true);
            when(userRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.seguir("ninguem", ID_JAKE))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void deveLancarTokenInvalidoQuandoSeguidorNaoExiste() {
            when(userRepository.existsById(ID_JAKE)).thenReturn(false);

            assertThatThrownBy(() -> profileService.seguir("ana", ID_JAKE))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }

    @Nested
    class DeixarDeSeguir {

        @Test
        void deveRemoverRelacao() {
            when(userRepository.existsById(ID_JAKE)).thenReturn(true);
            when(userRepository.findByUsername("ana")).thenReturn(Optional.of(ana));

            Profile perfil = profileService.deixarDeSeguir("ana", ID_JAKE);

            assertThat(perfil).isEqualTo(new Profile("ana", "bio da ana", null, false));
            verify(followRepository).deleteById(new FollowId(ID_JAKE, ID_ANA));
        }

        @Test
        void deveRetornarFollowingFalsoParaSiMesmoSemErro() {
            when(userRepository.existsById(ID_ANA)).thenReturn(true);
            when(userRepository.findByUsername("ana")).thenReturn(Optional.of(ana));

            assertThat(profileService.deixarDeSeguir("ana", ID_ANA).following()).isFalse();
        }

        @Test
        void deveLancarNaoEncontradoQuandoAlvoNaoExiste() {
            when(userRepository.existsById(ID_JAKE)).thenReturn(true);
            when(userRepository.findByUsername("ninguem")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.deixarDeSeguir("ninguem", ID_JAKE))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void deveLancarTokenInvalidoQuandoSeguidorNaoExiste() {
            when(userRepository.existsById(ID_JAKE)).thenReturn(false);

            assertThatThrownBy(() -> profileService.deixarDeSeguir("ana", ID_JAKE))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }
}
