# Implementation Plan: Conduit — Recorte A (usuários, autenticação e perfis)

**Branch**: `master` (nenhuma branch de feature criada) | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-conduit-usuarios-perfis/spec.md`

## Summary

Implementar, no projeto Spring Boot existente, as 7 operações do recorte A da API Conduit: cadastro, login, consulta/atualização do usuário atual, consulta de perfil e seguir/deixar de seguir. A autenticação usa JWT HS256 emitido e validado pelo suporte de *resource server* do Spring Security, lido do cabeçalho `Authorization: Token <jwt>`. Dados em PostgreSQL com duas tabelas (`usuario`, `seguimento`) criadas por migrations Flyway. Erros seguem o formato `{"errors": {...}}` do contrato, convertidos em um único `@RestControllerAdvice` e num `AuthenticationEntryPoint` próprio. Decisões detalhadas em [research.md](research.md).

## Technical Context

**Language/Version**: Java 25

**Primary Dependencies**: Spring Boot 4.1.2 (`spring-boot-starter-webmvc`, `-data-jpa`, `-validation`, `-flyway`, `-actuator`, já presentes); adicionar `spring-boot-starter-security-oauth2-resource-server` (inclui Spring Security, Nimbus JOSE e BCrypt). Jackson 3 (`tools.jackson`).

**Storage**: PostgreSQL 18.6 (Docker local em dev; Testcontainers em testes); migrations Flyway; `ddl-auto: validate`.

**Testing**: JUnit 5, AssertJ, Mockito (`@MockitoBean`), `@WebMvcTest`, `@SpringBootTest` + Testcontainers (`@ServiceConnection`); Surefire (`*Test`) e Failsafe (`*IT`, plugin a adicionar).

**Target Platform**: servidor Linux/Windows com JVM 25, JAR executável, porta 8080.

**Project Type**: web-service (API REST somente backend).

**Performance Goals**: cada operação responde em < 1 s em ambiente local (SC-002); sem metas de carga definidas.

**Constraints**: contrato da API obrigatório (rotas, JSON, status, `Content-Type: application/json; charset=utf-8`); segredos somente por variáveis de ambiente (`DB_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRET`).

**Scale/Scope**: 7 operações, 2 tabelas, 2 pacotes de funcionalidade.

Nenhum item ficou como NEEDS CLARIFICATION; os pontos em aberto foram resolvidos em [research.md](research.md).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

O arquivo `.specify/memory/constitution.md` ainda é o modelo vazio, sem princípios ratificados. Os gates abaixo vêm das regras do projeto (`CLAUDE.md` e `.claude/rules/*.md`), que valem como constituição de fato.

| Gate (origem) | Pré-design | Pós-design |
|---|---|---|
| Stack fixada: Java 25, Boot 4.1.2, Maven Wrapper, PostgreSQL 18.6 (`CLAUDE.md`) | ✅ | ✅ sem mudança de versão; só um starter novo, sem versão explícita |
| Pacote por funcionalidade com `web` → `service` → `persistence`; `shared` não depende de funcionalidades (`structure.md`) | ✅ | ✅ `shared.seguranca` emite/valida token só com id e não conhece `Usuario` |
| Esquema somente por migrations Flyway versionadas, restrições nomeadas, `NOT NULL` por padrão, índices em FKs (`tech.md` §6) | ✅ | ✅ `V1`, `V2` em [data-model.md](data-model.md) |
| `ddl-auto: validate`, `open-in-view: false` (`tech.md` §5) | ✅ | ✅ já no `application.yml` |
| Segredos fora de arquivos versionados (`tech.md` §1, §20) | ✅ | ✅ `JWT_SECRET` por ambiente; nos testes, gerado em tempo de execução |
| JWT com suporte oficial do Spring Security, validando assinatura, expiração e emissor; senhas com `PasswordEncoder` (`tech.md` §24) | ✅ | ✅ R1, R8 |
| Erros tratados em um único ponto, sem stack traces (`tech.md` §7, §21) | ✅ | ✅ R4, R5 |
| DTOs como records; entidades fora da API (`tech.md` §18, §21) | ✅ | ✅ |
| `@Transactional` só no serviço, `readOnly` em leituras (`tech.md` §23) | ✅ | ✅ |
| Testes com Testcontainers `postgres:18.6`, IT de migrations em banco vazio, sem H2 (`tech.md` §8, §26) | ✅ | ✅ R11 |
| Sem funcionalidades fora do recorte (`descricao-base.md`) | ✅ | ✅ |

Resultado: **PASS** — nenhuma violação; Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/001-conduit-usuarios-perfis/
├── plan.md              # este arquivo
├── research.md          # Fase 0
├── data-model.md        # Fase 1
├── quickstart.md        # Fase 1
├── contracts/
│   └── openapi.yaml     # Fase 1
├── checklists/
│   └── requirements.md
└── tasks.md             # Fase 2 (/speckit-tasks)
```

### Source Code (repository root)

Pacote raiz `com.example.blog` (classe `BlogApplication`, já existente).

```text
pom.xml                                   # + starter de resource server, + maven-failsafe-plugin
.env.example                              # + JWT_SECRET (valor fictício)
src/main/java/com/example/blog/
├── BlogApplication.java
├── shared/
│   ├── erro/
│   │   ├── ErroResponse.java             # record {"errors": Map<String, List<String>>}
│   │   ├── TratadorGlobalDeErros.java    # @RestControllerAdvice
│   │   ├── RecursoNaoEncontradoException.java
│   │   ├── ConflitoException.java
│   │   ├── ValidacaoException.java
│   │   └── CredenciaisInvalidasException.java
│   └── seguranca/
│       ├── JwtProperties.java            # @ConfigurationProperties("conduit.jwt")
│       ├── SegurancaConfig.java          # SecurityFilterChain, PasswordEncoder, JwtEncoder/Decoder
│       ├── TokenHeaderResolver.java      # BearerTokenResolver para "Token "
│       ├── ErroAutenticacaoEntryPoint.java
│       └── TokenService.java             # emite token a partir do id
├── usuario/
│   ├── web/
│   │   ├── UsuarioController.java        # POST /api/users, POST /api/users/login, GET/PUT /api/user
│   │   ├── CadastroRequest.java, LoginRequest.java, UsuarioResponse.java
│   │   └── AlteracaoUsuarioMapper.java   # JsonNode → AlteracaoUsuario (R6)
│   ├── service/
│   │   ├── UsuarioService.java
│   │   ├── AlteracaoUsuario.java
│   │   └── UsuarioAutenticado.java       # record de saída do serviço
│   └── persistence/
│       ├── Usuario.java
│       └── UsuarioRepository.java
└── perfil/
    ├── web/
    │   ├── PerfilController.java         # GET /api/profiles/{u}, POST/DELETE .../follow
    │   └── PerfilResponse.java
    ├── service/
    │   ├── PerfilService.java            # usa UsuarioService para localizar usuários
    │   └── Perfil.java
    └── persistence/
        ├── Seguimento.java, SeguimentoId.java
        └── SeguimentoRepository.java
src/main/resources/
├── application.yml                       # + conduit.jwt.*, server.servlet.encoding.*
└── db/migration/
    ├── V1__criar_tabela_usuario.sql
    └── V2__criar_tabela_seguimento.sql
src/test/java/com/example/blog/           # espelha main
├── usuario/{web,service}/...Test.java
├── perfil/{web,service}/...Test.java
├── shared/seguranca/...Test.java
├── MigracoesIT.java
├── usuario/UsuarioApiIT.java
└── perfil/PerfilApiIT.java
```

**Structure Decision**: aplicação de módulo único conforme `structure.md`, com duas funcionalidades (`usuario` e `perfil`) e o transversal em `shared`. `perfil.service` depende de `usuario.service` (nunca o inverso), mantendo o sentido `web → service → persistence`.

## Complexity Tracking

Sem violações a justificar.
