# Implementation Plan: Recorte A da API Conduit — Autenticação, Usuário e Perfis

**Branch**: `master` (sem branch de feature; não há hook de criação) | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-conduit-auth-profiles/spec.md`

## Summary

Implementar o backend do Recorte A da API Conduit: cadastro e login com JWT, consulta e atualização do usuário autenticado, perfil público e seguir/deixar de seguir, obedecendo ao contrato (rotas, JSON, status e formato `{"errors":{...}}`). Abordagem: Spring Boot 4.1.2 com Spring MVC; Spring Security em modo *resource server* para validar o JWT (HS256) lido do cabeçalho `Authorization: Token <jwt>`; Spring Data JPA sobre PostgreSQL 18.6 com esquema criado por migrations Flyway; tratamento de erros centralizado em `@RestControllerAdvice`. Detalhes e alternativas em [research.md](./research.md).

## Technical Context

**Language/Version**: Java 25 (`java.version` do `pom.xml`)

**Primary Dependencies**: Spring Boot 4.1.2 — `spring-boot-starter-webmvc`, `-data-jpa`, `-validation`, `-flyway`, `-actuator` (já presentes); a adicionar: `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server` (Nimbus JOSE para emitir e validar JWT). Jackson 3 (padrão do Boot 4).

**Storage**: PostgreSQL 18.6 (Docker local via `compose.yaml`); Flyway (`flyway-database-postgresql`) em `src/main/resources/db/migration`; `ddl-auto: validate`.

**Testing**: JUnit 5, AssertJ, Mockito (`@MockitoBean`), `@WebMvcTest` + MockMvc, `@SpringBootTest` + Testcontainers `postgres:18.6` com `@ServiceConnection`; a adicionar: `spring-boot-starter-security-test` e o `maven-failsafe-plugin` para os `*IT`.

**Target Platform**: servidor Linux/Windows com JVM 25; JAR executável; configuração por variáveis de ambiente.

**Project Type**: web-service (API REST, somente backend).

**Performance Goals**: 95% das requisições < 500 ms com 50 requisições simultâneas em ambiente local (SC-004).

**Constraints**: contrato da API obrigatório (OpenAPI prevalece); `Content-Type: application/json; charset=utf-8`; nenhum segredo em arquivo versionado; senhas só como hash; nenhuma resposta expõe hash, stack trace ou nomes de tabelas.

**Scale/Scope**: 7 endpoints, 2 tabelas (`users`, `follows`), 2 pacotes de funcionalidade (`user`, `profile`) + `shared`.

Nenhum "NEEDS CLARIFICATION" restante: todos resolvidos em [research.md](./research.md) (R1–R14).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` ainda é o modelo sem princípios preenchidos. Os gates abaixo vêm das regras do projeto (`CLAUDE.md` e `.claude/rules/*.md`), que fazem o papel de constituição.

| # | Gate (origem) | Pré-pesquisa | Pós-design |
|---|---|---|---|
| G1 | Somente backend, escopo do Recorte A, sem funcionalidades extras (`product.md`, `descricao-base.md`) | ✅ | ✅ 7 endpoints do contrato; nada além |
| G2 | Pacote por funcionalidade com `web` → `service` → `persistence`; `shared` sem depender de funcionalidades (`structure.md`) | ✅ | ✅ ver Project Structure; `profile.service` usa `user.persistence` (sentido service → persistence) e `shared` não importa `user`/`profile` |
| G3 | Esquema só por migrations Flyway versionadas, restrições nomeadas, `NOT NULL` por padrão, índices em FKs, `ddl-auto: validate` (`tech.md` banco §6) | ✅ | ✅ `V1`, `V2` em [data-model.md](./data-model.md) |
| G4 | Credenciais e segredo do JWT fora de arquivos versionados; `.env` não versionado (`tech.md` §1, §20) | ✅ | ✅ `JWT_SECRET` via ambiente/`.env` (R13) |
| G5 | Segurança: `SecurityFilterChain` explícito, negação por padrão, JWT com assinatura/expiração/emissor pelo suporte oficial, senha com `PasswordEncoder` (`tech.md` §24) | ✅ | ✅ R1–R5 |
| G6 | Erros tratados em um único ponto, sem detalhes internos; DTOs como records; entidades fora da API (`tech.md` §18, §21) | ✅ | ✅ R6, R8 |
| G7 | Testes: unitários `*Test`, integração `*IT` com Testcontainers `postgres:18.6`, teste de migrations em banco vazio, sem H2 (`tech.md` §11, §26, banco §8) | ✅ | ✅ R12 |
| G8 | Spring Boot 4: starters específicos, `jakarta.*`, Jackson 3, `@MockitoBean`, sem APIs depreciadas (`tech.md` §17) | ✅ | ✅ pontos a conferir na documentação 4.1.2 marcados em R12 |
| G9 | `@Transactional` só nos services, `readOnly` em leituras (`tech.md` §23) | ✅ | ✅ |

Resultado: **sem violações**. Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/001-conduit-auth-profiles/
├── plan.md              # este arquivo
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   └── openapi.yaml     # Phase 1 — contrato do recorte com exemplos de erro
├── checklists/
│   └── requirements.md  # gerado pelo /speckit-specify
└── tasks.md             # Phase 2 (/speckit-tasks — ainda não criado)
```

### Source Code (repository root)

Pacote raiz: `com.example.blog` (de `pom.xml`). A classe `BlogApplication` já existe no pacote raiz.

```text
src/main/java/com/example/blog/
├── BlogApplication.java                       # existente
├── user/
│   ├── web/
│   │   ├── UserController.java                # POST /api/users, POST /api/users/login, GET|PUT /api/user
│   │   ├── NewUserRequest.java                # {"user": NewUserData}
│   │   ├── NewUserData.java
│   │   ├── LoginRequest.java / LoginData.java
│   │   ├── UpdateUserRequest.java / UpdateUserData.java   # @JsonSetter(nulls = AS_EMPTY) em bio/image
│   │   └── UserResponse.java / UserView.java  # {"user": {email, token, username, bio, image}}
│   ├── service/
│   │   ├── UserService.java                   # cadastro, login, atual, atualização (@Transactional)
│   │   ├── NovoUsuario.java / AtualizacaoUsuario.java / UsuarioAutenticado.java   # records de entrada/saída do service
│   │   ├── CampoJaUsadoException.java
│   │   ├── CredenciaisInvalidasException.java
│   │   └── AtualizacaoVaziaException.java
│   └── persistence/
│       ├── User.java                          # @Entity users
│       └── UserRepository.java
├── profile/
│   ├── web/
│   │   ├── ProfileController.java             # GET /api/profiles/{username}, POST|DELETE .../follow
│   │   └── ProfileResponse.java / ProfileView.java
│   ├── service/
│   │   ├── ProfileService.java                # perfil, seguir, deixar de seguir
│   │   ├── Perfil.java                        # record
│   │   ├── PerfilNaoEncontradoException.java
│   │   └── SeguirASiMesmoException.java
│   └── persistence/
│       ├── Follow.java / FollowId.java        # @Entity follows, @EmbeddedId
│       └── FollowRepository.java              # seguir (ON CONFLICT DO NOTHING), deixarDeSeguir
└── shared/
    ├── security/
    │   ├── SecurityConfig.java                # SecurityFilterChain, PasswordEncoder, JwtEncoder/Decoder
    │   ├── JwtProperties.java                 # @ConfigurationProperties("app.jwt") record
    │   ├── TokenService.java                  # emite token (sub=id, iss, iat, exp) com Clock
    │   ├── TokenHeaderResolver.java           # BearerTokenResolver para "Token <jwt>"
    │   ├── ErrorAuthenticationEntryPoint.java # 401 is missing / is invalid
    │   ├── ErrorAccessDeniedHandler.java      # 403
    │   ├── UsuarioAtual.java                  # extrai id do Jwt autenticado (Optional<Long> para rota opcional)
    │   └── UsuarioDoTokenInexistenteException.java
    ├── error/
    │   ├── ErrorResponse.java                 # record {"errors": Map<String, List<String>>}
    │   └── GlobalExceptionHandler.java        # @RestControllerAdvice (tabela R6)
    ├── web/
    │   └── MediaTypes.java                    # APPLICATION_JSON_UTF8
    └── time/
        └── ClockConfig.java                   # @Bean Clock (UTC)

src/main/resources/
├── application.yml                            # + app.jwt.*
├── application-dev.yml                        # novo: spring.config.import: optional:file:.env[.properties]
└── db/migration/
    ├── V1__criar_tabela_users.sql
    └── V2__criar_tabela_follows.sql

src/test/java/com/example/blog/
├── TestcontainersConfiguration.java           # existente (postgres:18.6)
├── MigrationsIT.java
├── user/
│   ├── web/UserControllerTest.java            # @WebMvcTest
│   ├── service/UserServiceTest.java
│   └── UserApiIT.java                         # cenários US1 e US2 ponta a ponta
├── profile/
│   ├── web/ProfileControllerTest.java
│   ├── service/ProfileServiceTest.java
│   └── ProfileApiIT.java                      # cenários US3 e US4
└── shared/
    ├── security/TokenServiceTest.java
    ├── security/TokenHeaderResolverTest.java
    └── error/GlobalExceptionHandlerTest.java

src/test/resources/
└── application.yml                            # app.jwt.secret fictício de teste

.env.example                                   # + JWT_SECRET=<base64_de_32_bytes_aleatorios>
pom.xml                                        # + security, oauth2-resource-server, security-test, maven-failsafe-plugin
```

**Structure Decision**: aplicação Spring Boot de módulo único, conforme `structure.md`. Duas funcionalidades (`user`, `profile`) com `web`/`service`/`persistence`, e o que é transversal (segurança, erros, tipos de mídia, relógio) em `shared`. As exceções de domínio ficam no `service` de cada funcionalidade; para manter `shared` independente das funcionalidades, o `GlobalExceptionHandler` trata uma exceção base `shared.error.ApiException` (com status e mapa de erros) da qual as exceções de domínio herdam, em vez de importar cada exceção concreta.

## Fluxos principais

- **Cadastro**: `UserController` valida → `UserService.cadastrar` normaliza, verifica conflitos (409 com um ou dois campos), grava com hash → `TokenService.emitir(id)` → `201`.
- **Login**: `UserService.autenticar` busca por e-mail normalizado e confere o hash; falha → `CredenciaisInvalidasException` (401). Sucesso → novo token → `200`.
- **Usuário atual**: `UsuarioAtual` obtém o `sub` do `Jwt`; `UserService.buscarAtual(id)` (`readOnly`) → `UsuarioDoTokenInexistenteException` se ausente.
- **Atualização**: `UserService.atualizar(id, AtualizacaoUsuario)` aplica só os campos enviados, verifica conflitos com `...AndIdNot` (422) e devolve um token novo.
- **Perfil**: `ProfileService.buscar(username, Optional<Long> viewerId)` → 404 se ausente; `following` por `existsById`.
- **Seguir/deixar**: `ProfileService.seguir`/`deixarDeSeguir` → 404, 422 (si mesmo), insert idempotente / delete → perfil com `following` correspondente.

## Complexity Tracking

Sem violações dos gates; nada a justificar.
