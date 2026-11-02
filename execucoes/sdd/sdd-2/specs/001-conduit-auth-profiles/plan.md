# Implementation Plan: Recorte A da API Conduit — autenticação, usuário e perfis

**Branch**: `master` (sem branch de feature) | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-conduit-auth-profiles/spec.md`

## Summary

Implementar 7 operações REST do Conduit: cadastro, login, consulta e atualização do usuário autenticado, perfil público, seguir e deixar de seguir. O contrato JSON e de status é o de `descricao-base.md`. A autenticação usa JWT HS256 emitido e validado pelo resource server do Spring Security, com o esquema `Authorization: Token <jwt>`. Os dados ficam em PostgreSQL (`users`, `follows`), criados por migrations Flyway. Os erros são convertidos em um único `@RestControllerAdvice` para o formato `{"errors":{...}}`. Detalhes das decisões em [research.md](./research.md).

## Technical Context

**Language/Version**: Java 25

**Primary Dependencies**: Spring Boot 4.1.2 (webmvc, data-jpa, validation, flyway, actuator, já no `pom.xml`); novos: `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server` (Nimbus JOSE); Jackson 3 (`tools.jackson`)

**Storage**: PostgreSQL 18.6 (Docker local em `compose.yaml`); esquema por Flyway; `ddl-auto: validate`

**Testing**: JUnit 5, AssertJ, Mockito (`@MockitoBean`), `@WebMvcTest`, `@SpringBootTest` + MockMvc, Testcontainers `postgres:18.6` (`TestcontainersConfiguration` existente), `spring-security-test`; Surefire (`*Test`) + Failsafe (`*IT`)

**Target Platform**: servidor Linux/Windows com JVM 25; JAR executável com Tomcat embutido

**Project Type**: web-service (API REST, somente backend)

**Performance Goals**: p95 < 500 ms com até 50 usuários simultâneos em ambiente local (SC-005)

**Constraints**: contrato JSON fixo (`descricao-base.md`); `Content-Type: application/json; charset=utf-8`; segredos só por variável de ambiente (`JWT_SECRET`, credenciais do banco); sem stack traces nas respostas

**Scale/Scope**: 7 operações, 2 tabelas, 2 pacotes de funcionalidade (`user`, `profile`) + `shared`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` ainda é o modelo sem preencher, sem princípios ratificados. As regras do projeto em `.claude/rules/` (product, structure, tech) servem como portões:

| Portão (origem) | Status |
|---|---|
| Escopo só backend; nada além de `descricao-base.md` (product.md) | ✅ Somente as 7 operações do recorte |
| Pacote por funcionalidade com `web`/`service`/`persistence`, dependência só `web → service → persistence`; `shared` não depende de funcionalidades (structure.md) | ✅ `profile.service` usa `user.persistence` (sentido permitido); `shared` não importa `user`/`profile` (R3) |
| Esquema só por migrations Flyway versionadas, restrições nomeadas, `ddl-auto: validate` (tech.md, Banco §1/§6) | ✅ `V1`, `V2` (data-model.md) |
| Colunas `NOT NULL` por padrão | ✅ Exceções justificadas: `bio` e `image` são opcionais pelo contrato (`string | null`) |
| Sem segredos versionados; credenciais por ambiente (tech.md §14/§20) | ✅ `JWT_SECRET` no `.env`; só valor fictício em `.env.example` e em recursos de teste |
| Senhas com `PasswordEncoder` (tech.md §24) | ✅ BCrypt (R4) |
| JWT pelo suporte oficial, com assinatura, expiração e emissor validados (tech.md §24) | ✅ resource server + Nimbus (R1) |
| Erros num único ponto, sem detalhes internos (tech.md §7/§21) | ✅ `GlobalExceptionHandler` + `AuthenticationEntryPoint` (R3, R5) |
| DTOs como records; entidades fora da API (tech.md §18/§21) | ✅ |
| `@Transactional` só no serviço, `readOnly` nas leituras (tech.md §23) | ✅ |
| Testes com Testcontainers `postgres:18.6`, sem H2; teste de migrations em banco vazio (tech.md Banco §8) | ✅ R11 |
| APIs do Spring Boot 4 / Jackson 3 / `@MockitoBean` (tech.md §17/§26) | ✅ Conferir os nomes de pacotes na documentação 4.1 durante a implementação (R10) |
| Formato de erro `ProblemDetail` (tech.md §21) | ➖ Não aplicado: o contrato fixa `{"errors":{...}}` e prevalece (tech.md, Parte 2, preâmbulo) |

**Resultado pré-pesquisa**: aprovado. **Resultado pós-design**: aprovado, sem violações a justificar.

## Project Structure

### Documentation (this feature)

```text
specs/001-conduit-auth-profiles/
├── plan.md              # Este arquivo
├── research.md          # Fase 0
├── data-model.md        # Fase 1
├── quickstart.md        # Fase 1
├── contracts/
│   └── openapi.yaml     # Fase 1
├── checklists/
│   └── requirements.md  # /speckit-specify
└── tasks.md             # Fase 2 (/speckit-tasks; não criado aqui)
```

### Source Code (repository root)

```text
src/main/java/com/example/blog/
├── BlogApplication.java                       # existente
├── user/
│   ├── web/
│   │   ├── UserController.java                # POST /api/users, POST /api/users/login, GET|PUT /api/user
│   │   ├── RegisterUserRequest.java           # record { NewUserFields user } + Bean Validation
│   │   ├── LoginRequest.java                  # record { LoginFields user }
│   │   ├── UpdateUserRequest.java             # record { JsonNode user } → UserUpdate (R7)
│   │   └── UserResponse.java                  # record { UserBody user }
│   ├── service/
│   │   ├── UserService.java                   # cadastrar, autenticar, buscarAtual, atualizar
│   │   ├── UserUpdate.java                    # record com campos e indicadores de presença
│   │   └── AuthenticatedUser.java             # record de saída (dados + token)
│   └── persistence/
│       ├── UserEntity.java
│       └── UserRepository.java
├── profile/
│   ├── web/
│   │   ├── ProfileController.java             # GET /api/profiles/{u}, POST|DELETE /api/profiles/{u}/follow
│   │   └── ProfileResponse.java               # record { ProfileBody profile }
│   ├── service/
│   │   ├── ProfileService.java                # buscarPerfil, seguir, deixarDeSeguir
│   │   └── Profile.java                       # record de saída
│   └── persistence/
│       ├── FollowEntity.java
│       ├── FollowId.java
│       └── FollowRepository.java
└── shared/
    ├── security/
    │   ├── SecurityConfig.java                # SecurityFilterChain, CSRF off, sessão stateless, rotas públicas
    │   ├── JwtProperties.java                 # @ConfigurationProperties("app.jwt") record + @Validated
    │   ├── JwtConfig.java                     # JwtEncoder/JwtDecoder HS256, validadores de exp e iss, PasswordEncoder
    │   ├── TokenService.java                  # emitir(userId)
    │   ├── TokenAuthorizationHeaderResolver.java  # BearerTokenResolver p/ "Token "
    │   ├── TokenAuthenticationEntryPoint.java # 401 is missing / is invalid
    │   └── CurrentUserId.java                 # extrai o id (sub) do Jwt autenticado, se houver
    └── error/
        ├── GlobalExceptionHandler.java        # @RestControllerAdvice (R5)
        ├── ErrorResponse.java                 # record { Map<String,List<String>> errors }
        ├── ValidationException.java           # 422
        ├── ConflictException.java             # 409
        ├── NotFoundException.java             # 404
        ├── InvalidCredentialsException.java   # 401 credentials
        └── InvalidTokenException.java         # 401 token is invalid

src/main/resources/
├── application.yml                            # + app.jwt.*, server.servlet.encoding.*
└── db/migration/
    ├── V1__criar_tabela_users.sql
    └── V2__criar_tabela_follows.sql

src/test/java/com/example/blog/
├── TestcontainersConfiguration.java           # existente
├── MigrationsIT.java                          # migrations em banco vazio + validate
├── user/
│   ├── service/UserServiceTest.java
│   ├── web/UserControllerTest.java            # @WebMvcTest
│   └── UserApiIT.java                         # US1–US3 ponta a ponta
├── profile/
│   ├── service/ProfileServiceTest.java
│   ├── web/ProfileControllerTest.java         # @WebMvcTest
│   └── ProfileApiIT.java                      # US4–US5 ponta a ponta
└── shared/security/TokenServiceTest.java

src/test/resources/application.yml             # app.jwt.secret fictício de teste

pom.xml                                        # + security, oauth2-resource-server, security-test, failsafe
.env.example                                   # + JWT_SECRET=<segredo_local_32+_caracteres>
```

**Structure Decision**: Projeto único Spring Boot no layout de `structure.md`, pacote raiz `com.example.blog`. O pacote `profile` depende de `user.persistence` para resolver usuários por nome; `user` não depende de `profile`. `shared` contém segurança e erros e não importa pacotes de funcionalidade: o usuário autenticado chega aos serviços como id (`Long`), e o token é emitido por `TokenService` a partir do id.

## Fluxos principais

- **Cadastro**: `UserController` valida o DTO (422) → `UserService.cadastrar` normaliza (`trim`), verifica conflitos de `username` e `email` juntos (409 com as duas chaves), grava com hash BCrypt e emite o token → `201`.
- **Login**: `UserService.autenticar` busca por e-mail sem diferenciar maiúsculas e compara o hash. Falha → `InvalidCredentialsException` (401). Sucesso → novo token, `200`.
- **Usuário atual**: o controller obtém `CurrentUserId` do `Jwt`. O serviço carrega o usuário (inexistente → `InvalidTokenException`) e devolve um token novo.
- **Atualização**: `UpdateUserRequest` → `UserUpdate` (presença por campo) → o serviço valida (422), checa conflitos excluindo o próprio id (409) e aplica só os campos enviados (`@Transactional`).
- **Perfil**: `ProfileService.buscarPerfil(username, viewerIdOuNulo)` → `404` se não existe; `following` = `FollowRepository.existsById`.
- **Seguir/deixar de seguir**: `@Transactional`; `422` quando o alvo é o próprio usuário (só no seguir); operações idempotentes (R9).

## Complexity Tracking

Sem violações a justificar.
