# Implementation Plan: Recorte A da API Conduit — usuários, autenticação e perfis

**Branch**: `001-conduit-usuarios-perfis` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-conduit-usuarios-perfis/spec.md`, derivada de `descricao-base.md`.

## Summary

Implementar, no backend Spring Boot existente, o recorte A da API Conduit: cadastro e login com JWT, consulta e atualização do usuário autenticado, perfil público e seguir/deixar de seguir. O contrato OpenAPI da descrição-base é obrigatório ([contracts/openapi.yaml](contracts/openapi.yaml)).

Abordagem: Spring Security Resource Server com JWT HS256 e resolvedor do esquema `Token`; pacotes `usuario` e `perfil` (`web` → `service` → `persistence`) e `shared` para segurança e erros; esquema por duas migrations Flyway (`usuario`, `seguidor`); erros sempre em `{"errors": {...}}` por um `@RestControllerAdvice` e handlers de segurança. Detalhes em [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (`java.version` do `pom.xml`)

**Primary Dependencies**: Spring Boot 4.1.2 (`spring-boot-starter-webmvc`, `-data-jpa`, `-validation`, `-flyway`, `-actuator`); a adicionar: `spring-boot-starter-security`, `spring-boot-starter-security-oauth2-resource-server` (Nimbus JOSE para emitir e validar JWT), `spring-boot-starter-security-test` (test), `maven-failsafe-plugin`. Jackson 3 (`tools.jackson`).

**Storage**: PostgreSQL 18.6 (Docker Compose local; Testcontainers nos testes); Flyway + `flyway-database-postgresql`; Hibernate `ddl-auto: validate`.

**Testing**: JUnit 5, AssertJ, Mockito; `@WebMvcTest` + `@MockitoBean`; `@SpringBootTest` + Testcontainers `postgres:18.6` com `@ServiceConnection`. `*Test` no Surefire, `*IT` no Failsafe.

**Target Platform**: servidor Linux/Windows com JVM 25; Tomcat embutido na porta 8080.

**Project Type**: web-service (API REST, somente backend), módulo Maven único.

**Performance Goals**: não definidos pela descrição-base; buscas por e-mail, username e seguimento indexadas.

**Constraints**: contrato OpenAPI literal (rotas, JSON, status, `GenericErrorModel`); `Content-Type` JSON com `charset=utf-8`; sem segredos versionados (`JWT_SECRET` e credenciais do banco por ambiente); API sem estado.

**Scale/Scope**: 7 operações em 5 rotas, 2 tabelas; fora do escopo artigos, feed, comentários, favoritos, tags e exclusão de usuário.

Nenhum item ficou como NEEDS CLARIFICATION: as lacunas da descrição-base foram resolvidas como premissas na spec e decisões em [research.md](research.md).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` ainda é o modelo sem princípios preenchidos. Os portões abaixo vêm de `CLAUDE.md` e `.claude/rules/` (vale a regra mais restritiva).

| # | Portão | Pré-pesquisa | Pós-design |
|---|---|---|---|
| G1 | Stack fixada: Java 25, Spring Boot 4.1.2, Maven Wrapper, PostgreSQL 18.6; sem versões em dependências gerenciadas | OK | OK — novos starters sem versão |
| G2 | Layout `structure.md`: pacote por funcionalidade com `web`/`service`/`persistence`; `shared` sem depender de funcionalidades | OK | OK — `usuario`, `perfil`, `shared` (R15) |
| G3 | Esquema só por migrations Flyway, `ddl-auto: validate`, restrições nomeadas, FKs indexadas, `NOT NULL` por padrão | OK | OK — `bio`/`image` anuláveis por exigência do contrato (justificado em data-model) |
| G4 | Sem segredos versionados; datasource e JWT por variáveis de ambiente | OK | OK — `JWT_SECRET` no `.env`, `.env.example` com valor fictício |
| G5 | Segurança: `SecurityFilterChain` explícita, senha com `PasswordEncoder`, JWT validando assinatura/expiração/emissor via suporte oficial | OK | OK (R1–R5, R11) |
| G6 | API: DTOs records, Bean Validation na borda, erro tratado em um único ponto, sem entidades na API, sem stack trace | OK | OK — formato do contrato substitui `ProblemDetail` (contrato prevalece, `tech.md` Parte 2) |
| G7 | `@Transactional` só em serviços, `readOnly` em leituras; injeção por construtor | OK | OK |
| G8 | Testes: unitários, fatia e integração com Testcontainers `postgres:18.6`; teste de migrations em banco vazio; `./mvnw verify` passa | OK | OK (R16) |
| G9 | Escopo: somente o recorte A, sem funcionalidades extras | OK | OK — FR-031 |
| G10 | Não usar APIs obsoletas nem do Boot 3 (`@MockBean`, `javax`, `com.fasterxml.jackson.databind`) | OK | OK — `@MockitoBean`, `jakarta.*`, `tools.jackson` |

Resultado: sem violações. Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/001-conduit-usuarios-perfis/
├── spec.md              # especificação (EARS) derivada de descricao-base.md
├── plan.md              # este arquivo
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   ├── openapi.yaml     # contrato literal da descrição-base
│   └── README.md        # resumo de endpoints e chaves de erro
└── tasks.md             # Phase 2 (/speckit-tasks; não criado aqui)
```

### Source Code (repository root)

```text
src/main/java/com/example/blog/
├── BlogApplication.java                      # existente
├── usuario/
│   ├── web/          UsuarioController (POST /api/users, POST /api/users/login),
│   │                 UsuarioAtualController (GET/PUT /api/user),
│   │                 RegistroRequest, LoginRequest, AtualizacaoUsuarioRequest,
│   │                 UsuarioResponse, AtualizacaoUsuarioMapper
│   ├── service/      UsuarioService, AtualizacaoUsuario, UsuarioAutenticado (resultado com token)
│   └── persistence/  Usuario, UsuarioRepository
├── perfil/
│   ├── web/          PerfilController (GET /api/profiles/{username}, POST/DELETE .../follow), PerfilResponse
│   ├── service/      PerfilService, Perfil (resultado)
│   └── persistence/  Seguimento, SeguimentoId, SeguimentoRepository
└── shared/
    ├── config/       ClockConfig
    ├── security/     SecurityConfig, JwtProperties, TokenService, TokenAuthorizationResolver,
    │                 ErroAutenticacaoEntryPoint, ErroAcessoNegadoHandler, TokenInvalidoException
    └── web/          ErroResponse, TratadorGlobalDeErros, ValidacaoException, ConflitoException,
                      CredenciaisInvalidasException, RecursoNaoEncontradoException

src/main/resources/
├── application.yml          # + spring.servlet.encoding, conduit.jwt.*
├── application-dev.yml      # novo: spring.config.import do .env
└── db/migration/
    ├── V1__criar_tabela_usuario.sql
    └── V2__criar_tabela_seguidor.sql

src/test/java/com/example/blog/
├── TestcontainersConfiguration.java          # existente
├── MigrationsIT.java                         # todas as migrations em banco vazio + validate
├── usuario/  service/UsuarioServiceTest, web/UsuarioControllerTest, web/AtualizacaoUsuarioMapperTest, UsuarioApiIT
├── perfil/   service/PerfilServiceTest, web/PerfilControllerTest, PerfilApiIT
└── shared/   security/TokenServiceTest, web/TratadorGlobalDeErrosTest

Raiz: pom.xml (dependências + failsafe), .env.example (+ JWT_SECRET), README.md (como rodar e testar)
```

**Structure Decision**: módulo Maven único com pacote base `com.example.blog`, seguindo `structure.md`. `perfil.service` depende de `usuario.service` para localizar usuários; nenhuma dependência no sentido inverso nem de `shared` para funcionalidades.

## Complexity Tracking

Sem violações a justificar.
