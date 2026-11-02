# Implementation Plan: Conduit — Recorte A (autenticação, usuário e perfis)

**Branch**: `master` (nenhuma branch de funcionalidade foi criada) | **Date**: 2026-11-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-conduit-auth-perfis/spec.md`

## Summary

O recorte A da API Conduit tem 7 endpoints sob `/api`:

- cadastro e login com JWT;
- consulta e atualização do usuário atual;
- perfil público;
- seguir e deixar de seguir.

Os formatos JSON, os códigos de status e o formato de erro seguem a OpenAPI obrigatória ([contracts/openapi.yaml](contracts/openapi.yaml)).

A abordagem técnica:

- **Autenticação**: Spring Security como *resource server* JWT (HMAC-SHA256, Nimbus), com um resolvedor próprio para o prefixo `Token`.
- **Senhas**: BCrypt.
- **Persistência**: JPA sobre PostgreSQL, com o esquema criado por duas migrations Flyway (`usuario` e `seguimento`).
- **Erros**: tratamento centralizado que gera `{"errors": {...}}`.
- **Testes**: unitários, de fatia web, de integração com Testcontainers e uma suíte Hurl.

## Technical Context

**Language/Version**: Java 25 (`java.version` do `pom.xml`)

**Primary Dependencies**:

- Spring Boot 4.1.2: webmvc, data-jpa, validation, flyway e actuator, já presentes;
- a adicionar: `spring-boot-starter-security` e `spring-boot-starter-security-oauth2-resource-server`;
- Jackson 3 (`tools.jackson`).

**Storage**: PostgreSQL 18.6 (`postgres:18.6`), em Docker local (`compose.yaml`), com migrations Flyway.

**Testing**:

- JUnit 5, AssertJ, Mockito (`@MockitoBean`) e `spring-security-test`;
- Testcontainers `postgres:18.6` com `@ServiceConnection`;
- Surefire (`*Test`) e Failsafe (`*IT`, a adicionar);
- Hurl para a suíte ponta a ponta.

**Target Platform**: servidor Linux/JVM, JAR executável, porta 8080.

**Project Type**: web-service (API REST, somente backend).

**Performance Goals**: 95% das requisições abaixo de 1 s com até 50 usuários simultâneos (SC-005). O BCrypt com custo padrão (10) domina o tempo de cadastro e login.

**Constraints**:

- contrato OpenAPI imutável;
- `Content-Type: application/json; charset=utf-8`;
- nenhum segredo versionado (`JWT_SECRET` e credenciais do banco vêm do ambiente);
- `ddl-auto: validate`;
- `open-in-view: false`.

**Scale/Scope**: 7 endpoints, 2 tabelas e 2 pacotes de funcionalidade, além de `shared`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

O arquivo `.specify/memory/constitution.md` ainda é o modelo em branco e não define princípios. Por isso, os portões adotados são as regras do projeto em `.claude/rules/`. Em caso de conflito, vale a regra mais restritiva.

| Portão | Origem | Status |
|---|---|---|
| Pacotes por funcionalidade (`web` → `service` → `persistence`); `shared` sem depender de funcionalidades | `structure.md` | ✅ |
| Esquema somente por migrations nomeadas `V<n>__...`, restrições nomeadas, `NOT NULL` por padrão e FKs indexadas | `tech.md` §6 e §7 | ✅ |
| `ddl-auto: validate`, `open-in-view: false`, credenciais do ambiente | `tech.md` §5 e §20 | ✅ (já no `application.yml`) |
| Senhas com `PasswordEncoder`; JWT pelo suporte oficial, validando assinatura, expiração e emissor; chave fora do código | `tech.md` §14 e §24 | ✅ (R1, R7) |
| Erros tratados em um único ponto, sem detalhes internos; o formato do contrato prevalece sobre `ProblemDetail` | `tech.md` §7 e §21 | ✅ (R4) |
| DTOs como records; entidades fora da API; injeção por construtor; `@Transactional` só em serviços | `tech.md` §18 a §23 | ✅ |
| Testes com Testcontainers e a mesma versão do PostgreSQL; teste de migrations em banco vazio; sem H2 | `tech.md` §8 e §26 | ✅ (R12) |
| Starters do Boot 4, sem versões soltas, `jakarta.*`, Jackson 3, sem APIs obsoletas | `tech.md` §17 | ✅ (R13; nomes de starter a confirmar na documentação 4.1.2) |
| Escopo limitado ao recorte A | `descricao-base.md` | ✅ |

**Reavaliação pós-design**: todos os portões continuam ✅. Não há violações, então a seção Complexity Tracking não é necessária.

## Project Structure

### Documentation (this feature)

```text
specs/001-conduit-auth-perfis/
├── plan.md              # este arquivo
├── research.md          # Fase 0: decisões R1–R13
├── data-model.md        # Fase 1: tabelas, entidades, migrations
├── quickstart.md        # Fase 1: guia de validação
├── contracts/
│   ├── openapi.yaml     # contrato obrigatório (cópia de descricao-base.md)
│   └── README.md        # resumo de endpoints e catálogo de erros
├── checklists/requirements.md
└── tasks.md             # Fase 2 (/speckit-tasks; não criado aqui)
```

### Source Code (repository root)

O pacote base é `com.example.blog` (`groupId`/`artifactId` do `pom.xml`). A classe `BlogApplication` já existe no pacote raiz.

```text
src/main/java/com/example/blog/
├── BlogApplication.java
├── usuario/
│   ├── web/
│   │   ├── UsuarioController.java          # POST /api/users, POST /api/users/login, GET|PUT /api/user
│   │   ├── CadastroRequest.java            # records com Bean Validation (raiz "user")
│   │   ├── LoginRequest.java
│   │   ├── AtualizacaoUsuarioMapper.java   # ObjectNode → AtualizacaoUsuario (ausente × null)
│   │   ├── AtualizacaoRequest.java         # raiz "user" do PUT como ObjectNode
│   │   └── UsuarioResponse.java            # {"user": {...}}
│   ├── service/
│   │   ├── UsuarioService.java             # cadastro, login, consulta, atualização (@Transactional)
│   │   ├── AtualizacaoUsuario.java         # record com campos Alteracao<T>
│   │   ├── Alteracao.java                  # sealed: Manter | Definir(valor)
│   │   ├── UsuarioAutenticado.java         # record de saída do serviço (dados + token)
│   │   └── (exceções 401/409/422 vêm de shared.erro)
│   └── persistence/
│       ├── Usuario.java                    # entidade JPA
│       └── UsuarioRepository.java          # findByEmail, findByUsername, existsBy...
├── perfil/
│   ├── web/
│   │   ├── PerfilController.java           # GET /api/profiles/{u}, POST|DELETE /api/profiles/{u}/follow
│   │   └── PerfilResponse.java             # {"profile": {...}}
│   ├── service/
│   │   ├── PerfilService.java              # consultar, seguir, deixar de seguir
│   │   ├── Perfil.java                     # record de saída do serviço
│   │   └── PerfilNaoEncontradoException.java  # 404 profile: not found
│   └── persistence/
│       ├── Seguimento.java                 # entidade com @EmbeddedId
│       ├── SeguimentoId.java
│       └── SeguimentoRepository.java
└── shared/
    ├── seguranca/
    │   ├── SegurancaConfig.java            # SecurityFilterChain, PasswordEncoder, JwtEncoder/Decoder
    │   ├── JwtProperties.java              # @ConfigurationProperties("conduit.jwt") record
    │   ├── TokenService.java               # emissão de JWT (sub, iss, iat, exp), Clock injetado
    │   ├── ConduitTokenResolver.java       # "Authorization: Token <jwt>"
    │   └── ErroSegurancaHandler.java       # AuthenticationEntryPoint (401) e AccessDeniedHandler (403)
    └── erro/
        ├── ErrosResponse.java              # record {"errors": Map<String, List<String>>}
        ├── CampoJaUtilizadoException.java  # 409 com o campo
        ├── RecursoNaoEncontradoException.java  # base dos 404 (chave do recurso)
        ├── ValidacaoException.java         # 422 com o campo
        ├── NaoAutenticadoException.java    # 401 (credentials invalid / token is invalid)
        └── TratadorGlobalDeErros.java      # @RestControllerAdvice único

src/main/resources/
├── application.yml                         # + conduit.jwt.*, server.servlet.encoding.*
└── db/migration/
    ├── V1__criar_tabela_usuario.sql
    └── V2__criar_tabela_seguimento.sql

src/test/java/com/example/blog/
├── TestcontainersConfiguration.java        # já existe (postgres:18.6)
├── MigracoesIT.java                        # aplica migrations em banco vazio + validate
├── usuario/
│   ├── service/UsuarioServiceTest.java
│   ├── web/UsuarioControllerTest.java      # @WebMvcTest
│   ├── web/AtualizacaoUsuarioMapperTest.java
│   ├── CadastroLoginIT.java                # US1 com Testcontainers
│   └── UsuarioAtualIT.java                 # US2 com Testcontainers
├── perfil/
│   ├── service/PerfilServiceTest.java
│   ├── web/PerfilControllerTest.java
│   ├── PerfilIT.java
│   └── SeguirIT.java
└── shared/seguranca/
    ├── ConduitTokenResolverTest.java
    ├── TokenServiceTest.java
    └── AutenticacaoIT.java                 # 401 ausente/inválido/Bearer/expirado

src/test/hurl/recorte-a.hurl                # suíte ponta a ponta (Hurl)
```

Arquivos de suporte que também mudam:

- `pom.xml`: starters de segurança e Failsafe;
- `.env.example`: `JWT_SECRET` fictício;
- `README.md`: como construir, testar e executar.

**Structure Decision**: aplicação Spring Boot de módulo único com o layout de `.claude/rules/structure.md`. Há duas funcionalidades, `usuario` e `perfil`, e as regras transversais ficam em `shared`.

O pacote `perfil.service` depende de `usuario.persistence.UsuarioRepository` para localizar usuários pelo nome. Essa dependência entre funcionalidades segue o sentido permitido (service → persistence) e não cria ciclos, porque `usuario` não depende de `perfil`.

As exceções genéricas (`CampoJaUtilizadoException`, `RecursoNaoEncontradoException`, `ValidacaoException`) ficam em `shared.erro`, para que o tratador global não dependa dos pacotes de funcionalidade. Nos serviços, as exceções específicas estendem essas bases.

## Complexity Tracking

Não aplicável: nenhuma violação dos portões.
