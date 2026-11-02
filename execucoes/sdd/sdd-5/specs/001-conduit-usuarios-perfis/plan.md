# Implementation Plan: Conduit — Recorte A (usuários, autenticação e perfis)

**Branch**: `master` (nenhum branch de funcionalidade criado) | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-conduit-usuarios-perfis/spec.md`

## Summary

Implementar os 7 endpoints do recorte A da API Conduit — cadastro, login, consulta/atualização do usuário autenticado, perfil público e seguir/deixar de seguir — conforme o contrato OpenAPI de `descricao-base.md`. Abordagem: Spring Boot 4.1.2 (Spring MVC) com Spring Security como resource server JWT (HS256, esquema `Authorization: Token <jwt>`), senhas com BCrypt, persistência em PostgreSQL 18.6 via Spring Data JPA, esquema por migrations Flyway e erros no formato `{"errors":{...}}` centralizados em um `@RestControllerAdvice`. Detalhes em [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (`java.version` no `pom.xml`)

**Primary Dependencies**: Spring Boot 4.1.2 — `spring-boot-starter-webmvc`, `-data-jpa`, `-validation`, `-flyway` + `flyway-database-postgresql`, `-actuator` (já presentes); **a adicionar**: `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server` e, em teste, `spring-boot-starter-security-test`. Jackson 3 (`tools.jackson`).

**Storage**: PostgreSQL 18.6 (Docker local, `compose.yaml`); esquema por Flyway (`V1`, `V2`); `ddl-auto: validate`.

**Testing**: JUnit 5, AssertJ, Mockito (`@MockitoBean`), `@WebMvcTest`, `@SpringBootTest` + Testcontainers PostgreSQL 18.6 (`@ServiceConnection`, classe `TestcontainersConfiguration` existente). Surefire (`*Test`) e Failsafe (`*IT`, plugin a adicionar).

**Target Platform**: servidor Linux/Windows com JVM 25; JAR executável; porta 8080.

**Project Type**: web-service (API REST, somente backend).

**Performance Goals**: 95% das requisições < 1 s com 50 requisições simultâneas em ambiente local (SC-005).

**Constraints**: contrato OpenAPI obrigatório (rotas, JSON, status, formato de erro); `Content-Type: application/json; charset=utf-8`; segredos (`JWT_SECRET`, credenciais do banco) só por variáveis de ambiente; sem `ddl-auto` alterando esquema.

**Scale/Scope**: 7 endpoints, 2 tabelas, 2 funcionalidades (`usuario`, `perfil`) + `shared`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` ainda é o modelo sem preenchimento (nenhum princípio ratificado). Os portões efetivos são as regras do projeto em `CLAUDE.md` e `.claude/rules/` (vale a mais restritiva):

| Portão (origem) | Situação |
|---|---|
| Escopo somente backend; nada fora do recorte A (`product.md`, `descricao-base.md`) | ✅ só os 7 endpoints; sem artigos/feed/comentários/tags/exclusão |
| Stack fixada: Java 25, Boot 4.1.2, Maven Wrapper, PostgreSQL 18.6 (`CLAUDE.md`) | ✅ sem mudança de versões; novas dependências sem versão (BOM) |
| Pacote por funcionalidade `web`→`service`→`persistence`; `shared` sem depender de funcionalidades (`structure.md`) | ✅ `usuario`, `perfil`, `shared` (R12); `perfil`→`usuario` sem ciclo |
| Esquema só por migrations Flyway nomeadas, restrições nomeadas, `NOT NULL` por padrão, índices em FKs (`tech.md` §6) | ✅ `V1`, `V2`; nulos só em `bio`/`imagem` (exigência do contrato) |
| Credenciais fora de arquivos versionados; `.env.example` fictício (`tech.md` §1, §2.2, §20) | ✅ `JWT_SECRET` via ambiente, adicionado ao `.env.example` com valor fictício |
| Spring Security com `SecurityFilterChain`, resource server oficial, BCrypt, JWT com assinatura/expiração/emissor (`tech.md` §24) | ✅ R1–R4 |
| Erros num único `@RestControllerAdvice`; formato do contrato prevalece sobre `ProblemDetail` (`tech.md` §21) | ✅ R8 |
| Entidades fora da API; DTOs records; injeção por construtor; `@Transactional` só em serviços (`tech.md` §18–23) | ✅ data-model |
| Testes: unitários `*Test`, integração `*IT` com Testcontainers 18.6; teste de migrations em banco vazio; sem H2 (`tech.md` §8, §26) | ✅ R13 |
| Sem APIs do Boot 3.x/obsoletas (`@MockBean`, `javax.*`, `com.fasterxml.jackson`) (`tech.md` §17) | ✅ `@MockitoBean`, `jakarta.*`, `tools.jackson` |

**Resultado (pré-pesquisa)**: aprovado, sem violações. **Reavaliação pós-design (Fase 1)**: aprovado — o modelo de dados, o contrato de erros e a estrutura não introduzem violações; nada a justificar em Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/001-conduit-usuarios-perfis/
├── plan.md              # este arquivo
├── research.md          # Fase 0 — decisões técnicas R1–R14
├── data-model.md        # Fase 1 — tabelas, entidades, repositórios, DTOs
├── quickstart.md        # Fase 1 — roteiro de validação
├── contracts/
│   ├── openapi.yaml     # contrato normativo (extraído de descricao-base.md)
│   └── erros.md         # status por endpoint e catálogo de erros
├── checklists/
│   └── requirements.md  # checklist de qualidade da spec
└── tasks.md             # Fase 2 (/speckit-tasks — não criado aqui)
```

### Source Code (repository root)

```text
pom.xml                                  # + security, oauth2-resource-server, security-test, failsafe
.env.example                             # + JWT_SECRET (valor fictício)
src/main/java/com/example/blog/
├── BlogApplication.java                 # existente, pacote raiz
├── usuario/
│   ├── web/
│   │   ├── UsuarioController.java       # POST /api/users, POST /api/users/login, GET/PUT /api/user
│   │   ├── CadastroRequest.java, NovoUsuario.java
│   │   ├── LoginRequest.java, Credenciais.java
│   │   ├── AtualizacaoRequest.java, AtualizacaoUsuarioMapper.java
│   │   └── UsuarioEnvelope.java, UsuarioResponse.java
│   ├── service/
│   │   ├── UsuarioService.java          # cadastro, login, atual, atualização (@Transactional)
│   │   ├── AtualizacaoUsuario.java, CampoAtualizado.java
│   │   └── UsuarioAutenticadoDto.java
│   └── persistence/
│       ├── Usuario.java                 # @Entity
│       └── UsuarioRepository.java
├── perfil/
│   ├── web/
│   │   ├── PerfilController.java        # GET /api/profiles/{u}, POST/DELETE /api/profiles/{u}/follow
│   │   └── PerfilEnvelope.java, PerfilResponse.java
│   ├── service/
│   │   ├── PerfilService.java
│   │   └── PerfilDto.java
│   └── persistence/
│       ├── Seguimento.java, SeguimentoId.java
│       └── SeguimentoRepository.java
└── shared/
    ├── seguranca/
    │   ├── SegurancaConfig.java         # SecurityFilterChain, JwtEncoder/Decoder, PasswordEncoder, Clock
    │   ├── TokenProperties.java         # @ConfigurationProperties("conduit.token")
    │   ├── TokenService.java            # emite JWT
    │   ├── TokenHeaderResolver.java     # "Authorization: Token <jwt>"
    │   ├── ErroAutenticacaoHandler.java # entry point 401 / access denied 403
    │   └── UsuarioAutenticado.java      # extrai id do Jwt
    └── erro/
        ├── ManipuladorDeErros.java      # @RestControllerAdvice
        ├── ErrosResponse.java, MensagensErro.java
        └── ValidacaoException.java, ConflitoException.java, CredenciaisInvalidasException.java,
            RecursoNaoEncontradoException.java, TokenInvalidoException.java
src/main/resources/
├── application.yml                      # + conduit.token.*, server.servlet.encoding.*
└── db/migration/
    ├── V1__criar_tabela_usuario.sql
    └── V2__criar_tabela_seguimento.sql
src/test/java/com/example/blog/          # espelha main
├── TestcontainersConfiguration.java, TestBlogApplication.java   # existentes
├── MigracoesIT.java
├── usuario/{web,service}/...Test.java, usuario/UsuarioApiIT.java
├── perfil/{web,service}/...Test.java,  perfil/PerfilApiIT.java
└── shared/seguranca/...Test.java
src/test/resources/application-test.yml  # segredo JWT fictício de teste (perfil "test")
```

**Structure Decision**: projeto único Spring Boot no layout de `structure.md`, com pacote raiz `com.example.blog` (classe `BlogApplication` existente mantida em vez de renomear para `Application`, para não alterar a base). Duas funcionalidades (`usuario`, `perfil`) e `shared` para segurança e erros.

## Fluxos principais

- **Cadastro**: controller valida (`@Valid`) → `UsuarioService.cadastrar` verifica unicidade (409), grava com `PasswordEncoder` (`saveAndFlush`, traduz violação de `uq_*`) → `TokenService.emitir(id)` → `201` `UsuarioEnvelope`.
- **Login**: `findByEmailIgnoreCase` + `passwordEncoder.matches` → `CredenciaisInvalidasException` (401) ou `200` com token novo.
- **Usuário atual**: `UsuarioAutenticado.id(jwt)` → `buscarPorId` (inexistente → `TokenInvalidoException`) → `200` com token novo.
- **Atualização**: `AtualizacaoUsuarioMapper` (422) → serviço valida unicidade excluindo o próprio id (422) → aplica só campos presentes → `200` com token.
- **Perfil**: `findByUsername` (404) → `following = autenticado != null && seguimentoRepository.existsById(...)`.
- **Seguir/deixar de seguir**: busca alvo (404) → si mesmo (422) → insere se não existe / `deleteById` → `200` Profile.

## Complexity Tracking

Sem violações a justificar.
