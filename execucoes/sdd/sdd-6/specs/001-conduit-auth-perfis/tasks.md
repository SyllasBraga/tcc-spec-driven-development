---

description: "Task list for Conduit — Recorte A (autenticação, usuário e perfis)"
---

# Tasks: Conduit — Recorte A (autenticação, usuário e perfis)

**Input**: Design documents from `specs/001-conduit-auth-perfis/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: Incluídos. As regras do projeto exigem testes: `.claude/rules/tech.md` §11, §15, §26 e §8 (teste de migrations em banco vazio), além da decisão R12 do `research.md`. Testes unitários e de fatia terminam em `Test` (Surefire), testes de integração terminam em `IT` (Failsafe, com Testcontainers `postgres:18.6`).

**Organization**: As tarefas estão agrupadas por história de usuário, para que cada uma seja implementada e testada de forma independente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência de tarefas incompletas).
- **[Story]**: história de usuário da tarefa (US1 a US4).

## Convenções para todas as tarefas

- O pacote base é `com.example.blog`, com o código em `src/main/java/com/example/blog/` e os testes em `src/test/java/com/example/blog/`.
- Spring Boot 4.1.2 e Java 25:
  - use `jakarta.*` e Jackson 3 (`tools.jackson.*`), e `@MockitoBean` em vez de `@MockBean`;
  - confira na documentação da versão fixada o nome dos starters e os pacotes das anotações de teste (`@WebMvcTest`, `@AutoConfigureMockMvc`), que mudaram no Boot 4.
- Injeção por construtor, com campos `final`. DTOs são records. Nenhuma entidade JPA sai da API. `@Transactional` só nos serviços, com `readOnly = true` nas leituras.
- Todo erro sai no formato `{"errors": {"<chave>": ["<mensagem>"]}}`. O catálogo completo está em [contracts/README.md](contracts/README.md).
- Nenhum segredo vai para arquivos versionados.
- Rode o Maven com `-q` (`./mvnw -q verify`) e relate só os erros e as últimas 50 linhas de log.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: dependências, configuração e infraestrutura de teste.

- [X] T001 Edit `pom.xml`:
  - adicionar, **sem versão**:
    - `org.springframework.boot:spring-boot-starter-security`;
    - `org.springframework.boot:spring-boot-starter-security-oauth2-resource-server`;
    - `org.springframework.boot:spring-boot-starter-security-test` (escopo `test`);
  - adicionar em `<build><plugins>` o `maven-failsafe-plugin`, sem versão, com os goals `integration-test` e `verify`, para rodar `*IT`;
  - confirmar os nomes dos starters de segurança na documentação do Spring Boot 4.1.2 (R1, R13). Se o nome com prefixo `security-` não existir, usar o equivalente indicado na documentação dessa versão.
- [X] T002 [P] Edit `.env.example`:
  - acrescentar a linha `JWT_SECRET=<base64_de_32_bytes_aleatorios>`, com valor claramente fictício;
  - criar o `.env` local a partir de `.env.example`, se ainda não existir, conforme `.claude/rules/tech.md` §2.2:
    - `POSTGRES_DB=sdd_6`;
    - `POSTGRES_USER=agente`;
    - `POSTGRES_PASSWORD`: 32 caracteres gerados com o módulo `secrets` do Python;
    - `DB_PORT=5432`;
    - `DB_URL=jdbc:postgresql://localhost:5432/sdd_6`;
    - `JWT_SECRET`: Base64 de 32 bytes gerado com `secrets.token_bytes(32)`;
  - gravar os valores direto no arquivo, sem imprimi-los.
- [X] T003 [P] Edit `src/main/resources/application.yml`:
  - adicionar `conduit.jwt.secret: ${JWT_SECRET}`, `conduit.jwt.issuer: conduit` e `conduit.jwt.ttl: 24h`;
  - adicionar `server.servlet.encoding.charset: UTF-8` e `server.servlet.encoding.force-response: true` (R10);
  - manter `ddl-auto: validate`, `open-in-view: false` e `server.shutdown: graceful`.
- [X] T004 [P] Edit `src/test/java/com/example/blog/TestcontainersConfiguration.java`:
  - fornecer `conduit.jwt.secret` aleatório em tempo de execução: Base64 de 32 bytes de `SecureRandom`, registrado como propriedade de teste, por exemplo via `DynamicPropertyRegistrar` ou `@DynamicPropertySource`, conforme a API do Boot 4.1.2;
  - manter `postgres:18.6` com `@ServiceConnection`;
  - não versionar nenhum segredo.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: esquema de usuário, segurança JWT e tratamento de erros, que todas as histórias usam.

**⚠️ CRITICAL**: nenhuma história começa antes do fim desta fase.

- [X] T005 Create `src/main/resources/db/migration/V1__criar_tabela_usuario.sql`, criando `public.usuario` com as colunas abaixo, como em [data-model.md](data-model.md):

  | Coluna | Definição |
  |---|---|
  | `id` | `bigint generated always as identity`, `pk_usuario` |
  | `email` | `varchar(255) NOT NULL`, `uq_usuario_email` |
  | `username` | `varchar(50) NOT NULL`, `uq_usuario_username` |
  | `senha_hash` | `varchar(255) NOT NULL` |
  | `bio` | `text NULL` |
  | `image` | `text NULL` |
  | `criado_em` | `timestamptz NOT NULL DEFAULT now()` |
  | `atualizado_em` | `timestamptz NOT NULL DEFAULT now()` |

  Restrições adicionais:
  - `ck_usuario_email_nao_vazio CHECK (btrim(email) <> '')`;
  - `ck_usuario_username_nao_vazio CHECK (btrim(username) <> '')`.

  Escreva SQL puro, sem `IF NOT EXISTS`.
- [X] T006 Create the JPA entity `src/main/java/com/example/blog/usuario/persistence/Usuario.java`, mapeada para `usuario`:
  - id `Long` gerado por `IDENTITY`;
  - campos `email`, `username`, `senhaHash`, `bio`, `image`, `criadoEm` e `atualizadoEm` (`Instant`);
  - construtor protegido sem argumentos e construtor público `(email, username, senhaHash, Instant agora)`;
  - métodos de negócio `alterarEmail`, `alterarUsername`, `alterarSenhaHash`, `alterarBio`, `alterarImage` e `registrarAtualizacao(Instant)`, sem *setters* genéricos;
  - getters só do necessário;
  - `equals`/`hashCode` pelo `id`, com instâncias não persistidas diferentes entre si.
- [X] T007 Create `src/main/java/com/example/blog/usuario/persistence/UsuarioRepository.java`, uma interface `JpaRepository<Usuario, Long>` com:
  - `Optional<Usuario> findByEmail(String email)`;
  - `Optional<Usuario> findByUsername(String username)`;
  - `boolean existsByEmail(String email)`;
  - `boolean existsByUsername(String username)`;
  - `boolean existsByEmailAndIdNot(String email, Long id)`;
  - `boolean existsByUsernameAndIdNot(String username, Long id)`.
- [X] T008 [P] Create `src/main/java/com/example/blog/shared/erro/ErrosResponse.java`, um record `ErrosResponse(Map<String, List<String>> errors)` com:
  - fábrica `de(String chave, String mensagem)`;
  - cópia imutável no construtor compacto.
- [X] T009 [P] Create em `src/main/java/com/example/blog/shared/erro/` as exceções de domínio não verificadas. Todas guardam `chave` e `mensagem`:

  | Classe | Uso |
  |---|---|
  | `CampoJaUtilizadoException.java` | campo em uso; mensagem `"has already been taken"`; vira 409 |
  | `RecursoNaoEncontradoException.java` | recurso inexistente; mensagem `"not found"`; vira 404 |
  | `ValidacaoException.java` | regra de validação; vira 422 |
  | `NaoAutenticadoException.java` | vira 401; ver os usos abaixo |

  Usos de `NaoAutenticadoException`:
  - credenciais inválidas, com a chave `credentials` e a mensagem `invalid`;
  - token de usuário que não existe mais, com a chave `token` e a mensagem `is invalid`.
- [X] T010 Create `src/main/java/com/example/blog/shared/erro/TratadorGlobalDeErros.java`, o único `@RestControllerAdvice`, devolvendo sempre `ErrosResponse` (R4):

  | Exceção | Status | Corpo |
  |---|---|---|
  | `MethodArgumentNotValidException` | 422 | uma chave por campo (nome do campo JSON sem o prefixo `user.`), mensagem do *constraint*; a raiz `user` nula vira `body: ["can't be blank"]` |
  | `HttpMessageNotReadableException` | 422 | `body: ["is invalid"]` |
  | `ValidacaoException` | 422 | chave e mensagem da exceção |
  | `CampoJaUtilizadoException` | 409 | chave e mensagem da exceção |
  | `RecursoNaoEncontradoException` | 404 | chave e mensagem da exceção |
  | `NaoAutenticadoException` | 401 | chave e mensagem da exceção |
  | `DataIntegrityViolationException` | 409 | `email` se o nome da restrição contiver `uq_usuario_email`, `username` se contiver `uq_usuario_username`; senão `body: ["has already been taken"]` |
  | `Exception` | 500 | `server: ["unexpected error"]`; registra com SLF4J (`log.error` com a exceção) e não expõe detalhes |

  O tratador não pode depender de pacotes de funcionalidade.
- [X] T011 [P] Create `src/main/java/com/example/blog/shared/seguranca/JwtProperties.java`, um record `@ConfigurationProperties("conduit.jwt") @Validated` com:
  - `@NotBlank String secret`;
  - `@NotBlank String issuer`;
  - `@NotNull Duration ttl`.

  Registre o record com `@ConfigurationPropertiesScan` em `BlogApplication` ou `@EnableConfigurationProperties` em `SegurancaConfig`.
- [X] T012 [P] Create `src/main/java/com/example/blog/shared/seguranca/ConduitTokenResolver.java`, que implementa `BearerTokenResolver` (R2):
  - **sem cabeçalho `Authorization`**: retorna `null`;
  - **cabeçalho no formato `Token <jwt>`**, com prefixo exato `Token ` e token não vazio: retorna o token;
  - **qualquer outro formato** (`Bearer x`, `Token` sem valor, cabeçalho vazio): lança `InvalidBearerTokenException`.
- [X] T013 [P] Create `src/main/java/com/example/blog/shared/seguranca/ErroSegurancaHandler.java`, que implementa `AuthenticationEntryPoint` e `AccessDeniedHandler`:
  - **401**:
    - se a exceção é (ou tem como causa) `InvalidBearerTokenException` ou outra falha de token, o corpo é `{"errors":{"token":["is invalid"]}}`;
    - se não há credencial, o corpo é `{"errors":{"token":["is missing"]}}`;
  - **403**: `{"errors":{"authorization":["forbidden"]}}`;
  - **cabeçalhos**: `Content-Type: application/json;charset=UTF-8`, com o corpo serializado pelo `JsonMapper` do Jackson 3 injetado;
  - **nunca** incluir `WWW-Authenticate` com detalhes internos.
- [X] T014 Create `src/main/java/com/example/blog/shared/seguranca/TokenService.java`, que emite o JWT com `JwtEncoder`:
  - algoritmo HS256;
  - claims `sub` = id do usuário (texto), `iss` = `issuer`, `iat` = `clock.instant()` e `exp` = `iat + ttl`;
  - método `String emitir(Long usuarioId)`;
  - método `Long usuarioId(Jwt jwt)` que lê `sub`;
  - recebe um `Clock` injetado.
- [X] T015 Create `src/main/java/com/example/blog/shared/seguranca/SegurancaConfig.java`, com os beans:
  - `Clock` (`Clock.systemUTC()`);
  - `PasswordEncoder` (`PasswordEncoderFactories.createDelegatingPasswordEncoder()`);
  - `SecretKey` (HmacSHA256, a partir de `secret` decodificado em Base64);
  - `JwtEncoder` (`NimbusJwtEncoder` com `ImmutableSecret`);
  - `JwtDecoder` (`NimbusJwtDecoder.withSecretKey(...).macAlgorithm(HS256)`), com validadores de expiração e emissor via `JwtValidators.createDefaultWithIssuer(issuer)`;
  - `SecurityFilterChain`:
    - `csrf` desabilitado (API sem estado, com token);
    - sessão `STATELESS`;
    - `permitAll` para `POST /api/users`, `POST /api/users/login`, `GET /api/profiles/*` e `GET /actuator/health`;
    - `authenticated` para o restante;
    - `oauth2ResourceServer(jwt)` com `bearerTokenResolver(ConduitTokenResolver)`, `authenticationEntryPoint` e `accessDeniedHandler` do `ErroSegurancaHandler`;
    - `exceptionHandling` com os mesmos handlers.
- [X] T016 [P] Create `src/test/java/com/example/blog/shared/seguranca/ConduitTokenResolverTest.java`, com testes unitários:
  - sem cabeçalho → `null`;
  - `Token abc.def.ghi` → `abc.def.ghi`;
  - `Bearer abc` → lança `InvalidBearerTokenException`;
  - `Token ` vazio → lança `InvalidBearerTokenException`.
- [X] T017 [P] Create `src/test/java/com/example/blog/shared/seguranca/TokenServiceTest.java`, com teste unitário usando um `Clock` fixo, um encoder e um decoder reais com chave de teste gerada em memória. O token emitido deve ser decodificado com `sub`, `iss` e `exp = iat + ttl`.
- [X] T018 Create `src/test/java/com/example/blog/MigracoesIT.java`:
  - `@SpringBootTest` com `@Import(TestcontainersConfiguration.class)`;
  - o contexto sobe, ou seja, o Flyway aplica todas as migrations em banco vazio e o `ddl-auto: validate` passa;
  - consulta `flyway_schema_history` via `JdbcTemplate` e verifica que todas as versões têm `success = true`.

**Checkpoint**: `./mvnw -q verify` passa, a aplicação sobe com o esquema `usuario` e as rotas protegidas respondem 401 no formato do contrato.

---

## Phase 3: User Story 1 - Cadastro e login (Priority: P1) 🎯 MVP

**Goal**: `POST /api/users` (201) e `POST /api/users/login` (200) retornam `{"user": {email, token, username, bio, image}}`, com os erros 401, 409 e 422 (FR-012 a FR-019).

**Independent Test**: cadastrar um usuário, fazer login e verificar o objeto `user` com token válido. Cadastro repetido → 409, senha errada → 401, campos vazios → 422.

### Tests for User Story 1

> Escreva primeiro e confirme que falham antes da implementação.

- [X] T019 [P] [US1] Create `src/test/java/com/example/blog/usuario/service/UsuarioServiceTest.java`, com testes unitários (Mockito para `UsuarioRepository`, `PasswordEncoder` e `TokenService`; `Clock` fixo) para:
  - `cadastrar` normaliza o e-mail (`trim` e minúsculas) e grava o hash;
  - e-mail existente → `CampoJaUtilizadoException("email")`;
  - username existente → `CampoJaUtilizadoException("username")`;
  - `autenticar` com e-mail inexistente ou senha errada → `NaoAutenticadoException("credentials", "invalid")`;
  - `autenticar` com sucesso devolve o token.
- [X] T020 [P] [US1] Create `src/test/java/com/example/blog/usuario/web/UsuarioControllerTest.java`, um teste `@WebMvcTest(UsuarioController.class)` que importa `SegurancaConfig`, `ErroSegurancaHandler`, `ConduitTokenResolver`, `TratadorGlobalDeErros` e `JwtProperties`, com `@MockitoBean UsuarioService`. Casos:
  - `POST /api/users` válido → 201 com o JSON `user` e `bio`/`image` presentes como `null`;
  - `{"user":{}}` → 422 com as chaves `email`, `username` e `password` contendo `"can't be blank"`;
  - e-mail inválido → 422 `email: ["is invalid"]`;
  - JSON malformado → 422 `body`;
  - `POST /api/users/login` sem `password` → 422;
  - `Content-Type` contém `application/json` e `charset=UTF-8`.
- [X] T021 [P] [US1] Create `src/test/java/com/example/blog/usuario/CadastroLoginIT.java`, com `@SpringBootTest`, `@AutoConfigureMockMvc` e `TestcontainersConfiguration`:
  - fluxo real cadastro → 201 → login → 200, com token aceito em `GET /api/user` (apenas status 200, se a US2 já existir; senão, valide o token decodificando-o);
  - cadastro com e-mail repetido em outra caixa (`JAKE@x.com` × `jake@x.com`) → 409 `email`;
  - username repetido → 409 `username`;
  - senha errada → 401 `credentials`;
  - nenhuma resposta contém `password` nem `senha`.

  Cada teste usa dados fictícios únicos, como um sufixo aleatório no username e no e-mail.

### Implementation for User Story 1

- [X] T022 [P] [US1] Create `src/main/java/com/example/blog/usuario/web/CadastroRequest.java`, um record `CadastroRequest(@NotNull(message="can't be blank") @Valid Dados user)` com o record aninhado `Dados`:

  | Campo | Restrições |
  |---|---|
  | `username` | `@NotBlank(message="can't be blank") @Size(max=50, message="is too long")` |
  | `email` | `@NotBlank(message="can't be blank") @Email(message="is invalid") @Size(max=255, message="is too long")` |
  | `password` | `@NotBlank(message="can't be blank")` |
- [X] T023 [P] [US1] Create `src/main/java/com/example/blog/usuario/web/LoginRequest.java`, um record `LoginRequest(@NotNull(message="can't be blank") @Valid Dados user)`, com `Dados(@NotBlank(message="can't be blank") String email, @NotBlank(message="can't be blank") String password)`.
- [X] T024 [P] [US1] Create:
  - `src/main/java/com/example/blog/usuario/service/UsuarioAutenticado.java`, o record `(String email, String token, String username, String bio, String image)`;
  - `src/main/java/com/example/blog/usuario/web/UsuarioResponse.java`, o record `UsuarioResponse(Dados user)`, com `Dados(email, token, username, bio, image)` e fábrica `de(UsuarioAutenticado)`.

  `bio` e `image` são sempre serializados, inclusive como `null`. Não configure `NON_NULL`.
- [X] T025 [US1] Create `src/main/java/com/example/blog/usuario/service/UsuarioService.java` (`@Service`), com dois métodos:
  - `@Transactional UsuarioAutenticado cadastrar(String username, String email, String senha)`:
    - faz `trim` do username;
    - faz `trim` e `toLowerCase(Locale.ROOT)` do e-mail;
    - verifica `existsByEmail`, que lança `CampoJaUtilizadoException("email")`;
    - verifica `existsByUsername`, que lança `CampoJaUtilizadoException("username")`;
    - grava `Usuario` com `passwordEncoder.encode(senha)` e `clock.instant()`;
    - emite o token com `TokenService`;
  - `@Transactional(readOnly = true) UsuarioAutenticado autenticar(String email, String senha)`:
    - busca por e-mail normalizado;
    - se não existir ou se `matches` falhar, lança `NaoAutenticadoException("credentials", "invalid")`.

  Registre com `log.info("Usuário {} cadastrado", id)`, sem dados pessoais. Depende de T006, T007, T014 e T024.
- [X] T026 [US1] Create `src/main/java/com/example/blog/usuario/web/UsuarioController.java` (`@RestController`, `@RequestMapping("/api")`):
  - `POST /users` com `@Valid @RequestBody CadastroRequest` → `ResponseEntity.created(URI.create("/api/user")).body(UsuarioResponse)` (201 com `Location`);
  - `POST /users/login` com `@Valid @RequestBody LoginRequest` → 200 com `UsuarioResponse`.

  Ambos produzem `application/json`. O controller não tem regra de negócio.

**Checkpoint**: US1 funcional e testável sozinha (MVP).

---

## Phase 4: User Story 2 - Consultar e atualizar o próprio usuário (Priority: P2)

**Goal**: `GET /api/user` e `PUT /api/user` autenticados por `Authorization: Token <jwt>` (FR-007 a FR-009 e FR-020 a FR-026).

**Independent Test**: com o token do cadastro, `GET /api/user` → 200. `PUT` com `bio`/`image` → 200, com os campos alterados e os demais mantidos. Sem token → 401 `token: ["is missing"]`.

### Tests for User Story 2

- [X] T027 [P] [US2] Create `src/test/java/com/example/blog/usuario/web/AtualizacaoUsuarioMapperTest.java`, com testes unitários para:
  - campo ausente → `Alteracao.Manter`;
  - `"bio": null` → `Definir(null)`;
  - `"image": "x"` → `Definir("x")`;
  - objeto vazio → `ValidacaoException("body", "at least one field is required")`;
  - `"email": null` ou `""` → `ValidacaoException("email", "can't be blank")`;
  - `"email": "invalido"` → `ValidacaoException("email", "is invalid")`;
  - `"password": ""` → `ValidacaoException("password", "can't be blank")`;
  - `"username"` com mais de 50 caracteres → `ValidacaoException("username", "is too long")`;
  - valor não textual (por exemplo, número em `email`) → `ValidacaoException(<campo>, "is invalid")`.
- [X] T028 [P] [US2] Extend `src/test/java/com/example/blog/usuario/service/UsuarioServiceTest.java`, com testes unitários para:
  - `consultarAtual` com id inexistente → `NaoAutenticadoException("token", "is invalid")`;
  - `atualizar` altera só os campos `Definir`;
  - e-mail de outro usuário → `CampoJaUtilizadoException("email")`;
  - o próprio e-mail atual não gera conflito;
  - nova senha é gravada como hash;
  - `bio` `null` limpa o campo.
- [X] T029 [P] [US2] Create `src/test/java/com/example/blog/usuario/UsuarioAtualIT.java` (Testcontainers + MockMvc), com os casos:
  - `GET /api/user` com `Token <jwt>` → 200 com o mesmo `email` e `username` e um token presente;
  - `PUT` com `{"user":{"bio":"I like to skateboard","image":"https://i.stack.imgur.com/xHWG8.jpg"}}` → 200, com `email` e `username` inalterados;
  - `PUT` com `"bio": null` → `bio` `null`;
  - `PUT` com nova senha → login com a nova senha → 200, e com a antiga → 401;
  - `PUT` com e-mail de outro usuário → 409;
  - `PUT` com `{"user":{}}` → 422.
- [X] T030 [P] [US2] Create `src/test/java/com/example/blog/shared/seguranca/AutenticacaoIT.java` (Testcontainers + MockMvc), com os casos:
  - `GET /api/user` sem cabeçalho → 401 `{"errors":{"token":["is missing"]}}`;
  - `Authorization: Bearer <jwt válido>` → 401 `token: ["is invalid"]`;
  - token com assinatura alterada → 401;
  - token expirado (emitido com `TokenService` e `Clock` no passado, ou com `JwtEncoder` e `exp` passado) → 401;
  - token com `iss` diferente → 401;
  - `PUT /api/user` sem token → 401.

  Todas as respostas têm `Content-Type` com `charset=UTF-8`.

### Implementation for User Story 2

- [X] T031 [P] [US2] Create:
  - `src/main/java/com/example/blog/usuario/service/Alteracao.java`, `sealed interface Alteracao<T>` com `record Manter<T>()` e `record Definir<T>(T valor)`, além das fábricas `manter()` e `definir(T)`;
  - `src/main/java/com/example/blog/usuario/service/AtualizacaoUsuario.java`, o record `(Alteracao<String> email, Alteracao<String> username, Alteracao<String> senha, Alteracao<String> bio, Alteracao<String> image)`.
- [X] T032 [US2] Create `src/main/java/com/example/blog/usuario/web/AtualizacaoUsuarioMapper.java` (`@Component`), que converte o `ObjectNode` da raiz `user` (Jackson 3, `tools.jackson.databind.node.ObjectNode`) em `AtualizacaoUsuario`, seguindo R6 e as regras de T027:
  - considera só os campos aceitos (`email`, `username`, `password`, `bio`, `image`) e ignora os demais;
  - exige ao menos um campo aceito;
  - para `email`, `username` e `password`: aplica `trim` e valida que não são nulos nem vazios;
  - para `email`: valida o formato (mesmo critério de `@Email`) e o máximo de 255 caracteres;
  - para `username`: máximo de 50 caracteres;
  - para `bio` e `image`: aceita texto ou `null`.

  Depende de T031.
- [X] T033 [P] [US2] Create `src/main/java/com/example/blog/usuario/web/AtualizacaoRequest.java`, o record `AtualizacaoRequest(@NotNull(message="can't be blank") ObjectNode user)`, que carrega a raiz `user` do `PUT`.
- [X] T034 [US2] Extend `src/main/java/com/example/blog/usuario/service/UsuarioService.java`:
  - `@Transactional(readOnly = true) UsuarioAutenticado consultarAtual(Long id)`: se o usuário não existir, lança `NaoAutenticadoException("token", "is invalid")`; emite um token novo;
  - `@Transactional UsuarioAutenticado atualizar(Long id, AtualizacaoUsuario alteracao)`:
    - usa `switch` exaustivo sobre `Alteracao`;
    - e-mail normalizado (minúsculas) com `existsByEmailAndIdNot` → `CampoJaUtilizadoException("email")`;
    - `existsByUsernameAndIdNot` → `CampoJaUtilizadoException("username")`;
    - senha → `passwordEncoder.encode`;
    - `bio`/`image` aceitam `null`;
    - chama `registrarAtualizacao(clock.instant())`;
    - devolve um token novo.
- [X] T035 [US2] Extend `src/main/java/com/example/blog/usuario/web/UsuarioController.java`:
  - `GET /user` com `@AuthenticationPrincipal Jwt jwt` → `usuarioService.consultarAtual(tokenService.usuarioId(jwt))` → 200 com `UsuarioResponse`;
  - `PUT /user` com `@Valid @RequestBody AtualizacaoRequest` → `mapper.converter(request.user())` → `usuarioService.atualizar(...)` → 200.

  O `TokenService` fica em `shared` e só é usado para extrair o id do `Jwt`.
- [X] T036 [US2] Extend `src/test/java/com/example/blog/usuario/web/UsuarioControllerTest.java`, cobrindo:
  - `GET /api/user` com `jwt()` do `spring-security-test` (ou cabeçalho `Token` com decoder mockado) → 200;
  - sem token → 401 `token: ["is missing"]`;
  - `PUT` com `{"user":{}}` → 422 `body`;
  - `PUT` sem raiz `user` → 422.

**Checkpoint**: US1 e US2 funcionam e são testáveis de forma independente.

---

## Phase 5: User Story 3 - Consultar perfil público (Priority: P3)

**Goal**: `GET /api/profiles/{username}` com autenticação opcional retorna `{"profile": {username, bio, image, following}}`, com 404 para usuário inexistente e 401 para token inválido (FR-027 a FR-029).

**Independent Test**: cadastrar um usuário e consultar o perfil sem token → 200 `following: false`. Inexistente → 404 `profile: ["not found"]`.

### Tests for User Story 3

- [X] T037 [P] [US3] Create `src/test/java/com/example/blog/perfil/service/PerfilServiceTest.java`, com testes unitários (Mockito para `UsuarioRepository` e `SeguimentoRepository`):
  - `consultar(username, null)` → `following=false`, sem consultar `SeguimentoRepository`;
  - `consultar(username, idLeitor)` com relação existente → `following=true`;
  - username inexistente → `PerfilNaoEncontradoException` (chave `profile`, mensagem `not found`).
- [X] T038 [P] [US3] Create `src/test/java/com/example/blog/perfil/web/PerfilControllerTest.java`, um teste `@WebMvcTest(PerfilController.class)` com a mesma configuração de segurança de T020 e `@MockitoBean PerfilService`:
  - `GET /api/profiles/jake` sem token → 200, com o JSON `profile` e `bio`/`image` presentes mesmo quando `null`;
  - com `Authorization: Bearer x` → 401;
  - serviço lançando `PerfilNaoEncontradoException` → 404 `{"errors":{"profile":["not found"]}}`.
- [X] T039 [P] [US3] Create `src/test/java/com/example/blog/perfil/PerfilIT.java` (Testcontainers + MockMvc), com os casos:
  - perfil de usuário cadastrado sem token → 200 `following: false`;
  - com token de outro usuário que o segue → `following: true`, inserindo a relação diretamente via `SeguimentoRepository` no teste, para não depender da US4;
  - username inexistente → 404;
  - token inválido → 401.

### Implementation for User Story 3

- [X] T040 [US3] Create `src/main/resources/db/migration/V2__criar_tabela_seguimento.sql`, criando `public.seguimento`:

  | Coluna | Definição |
  |---|---|
  | `seguidor_id` | `bigint NOT NULL`, `CONSTRAINT fk_seguimento_seguidor FOREIGN KEY (seguidor_id) REFERENCES public.usuario (id) ON DELETE CASCADE` |
  | `seguido_id` | `bigint NOT NULL`, `CONSTRAINT fk_seguimento_seguido FOREIGN KEY (seguido_id) REFERENCES public.usuario (id) ON DELETE CASCADE` |
  | `criado_em` | `timestamptz NOT NULL DEFAULT now()` |

  Também:
  - `CONSTRAINT pk_seguimento PRIMARY KEY (seguidor_id, seguido_id)`;
  - `CREATE INDEX ix_seguimento_seguido_id ON public.seguimento (seguido_id)`;
  - sem `CHECK` contra seguir a si mesmo.
- [X] T041 [P] [US3] Create:
  - `src/main/java/com/example/blog/perfil/persistence/SeguimentoId.java`, `@Embeddable` com `seguidorId` e `seguidoId` (`Long`, colunas `seguidor_id`/`seguido_id`), construtor sem argumentos protegido e `equals`/`hashCode` pelos dois campos;
  - `src/main/java/com/example/blog/perfil/persistence/Seguimento.java`, `@Entity @Table(name="seguimento")` com `@EmbeddedId SeguimentoId id` e `Instant criadoEm` (coluna `criado_em`), construtor `(SeguimentoId, Instant)`, sem associações `@ManyToOne`.
- [X] T042 [P] [US3] Create `src/main/java/com/example/blog/perfil/persistence/SeguimentoRepository.java`, uma interface `JpaRepository<Seguimento, SeguimentoId>`. As operações usadas são `existsById`, `save` e `deleteById`.
- [X] T043 [P] [US3] Create:
  - `src/main/java/com/example/blog/perfil/service/Perfil.java`, o record `(String username, String bio, String image, boolean following)`;
  - `src/main/java/com/example/blog/perfil/service/PerfilNaoEncontradoException.java`, que estende `RecursoNaoEncontradoException` com a chave `profile` e a mensagem `not found`;
  - `src/main/java/com/example/blog/perfil/web/PerfilResponse.java`, o record `PerfilResponse(Dados profile)`, com `Dados(username, bio, image, following)` e fábrica `de(Perfil)`.
- [X] T044 [US3] Create `src/main/java/com/example/blog/perfil/service/PerfilService.java` (`@Service`), com o método `@Transactional(readOnly = true) Perfil consultar(String username, Long leitorId)`:
  - `leitorId` é nulo quando a requisição é anônima;
  - busca com `UsuarioRepository.findByUsername` e, se não encontrar, lança `PerfilNaoEncontradoException`;
  - calcula `following = leitorId != null && seguimentoRepository.existsById(new SeguimentoId(leitorId, alvo.getId()))`.

  Depende de T041 a T043.
- [X] T045 [US3] Create `src/main/java/com/example/blog/perfil/web/PerfilController.java` (`@RestController`, `@RequestMapping("/api/profiles")`), com `GET /{username}` e `@AuthenticationPrincipal Jwt jwt` (nulo quando anônimo). Usa `TokenService.usuarioId(jwt)` quando houver token e devolve 200 com `PerfilResponse`.

**Checkpoint**: US3 funcional e testável sozinha.

---

## Phase 6: User Story 4 - Seguir e deixar de seguir (Priority: P4)

**Goal**: `POST` e `DELETE /api/profiles/{username}/follow` autenticados, idempotentes, retornando o `profile` do alvo com `following` true/false (FR-030 a FR-033).

**Independent Test**: com dois usuários, o primeiro segue o segundo (`following: true`) e depois deixa de seguir (`following: false`). Sem token → 401, alvo inexistente → 404.

### Tests for User Story 4

- [X] T046 [P] [US4] Extend `src/test/java/com/example/blog/perfil/service/PerfilServiceTest.java`, com os casos:
  - `seguir` insere quando não existe e não insere de novo quando já existe;
  - `seguir` devolve `following=true`;
  - `deixarDeSeguir` remove quando existe, não falha quando não existe e devolve `following=false`;
  - alvo inexistente → `PerfilNaoEncontradoException`;
  - seguir a si mesmo é permitido.
- [X] T047 [P] [US4] Create `src/test/java/com/example/blog/perfil/SeguirIT.java` (Testcontainers + MockMvc), com os casos:
  - `POST /api/profiles/{b}/follow` com token de A → 200 `following: true`;
  - repetir → 200 `following: true`, com uma única linha em `seguimento` (contada via repository);
  - `GET /api/profiles/{b}` com token de A → `following: true`;
  - `DELETE` → 200 `following: false`;
  - repetir `DELETE` → 200 `following: false`;
  - sem token em `POST`/`DELETE` → 401 `token: ["is missing"]`;
  - alvo inexistente → 404 `profile: ["not found"]`.

### Implementation for User Story 4

- [X] T048 [US4] Extend `src/main/java/com/example/blog/perfil/service/PerfilService.java`, com dois métodos `@Transactional`:
  - `Perfil seguir(String username, Long seguidorId)`: busca o alvo (404 se não existir) e, se `!existsById(id)`, salva `new Seguimento(id, clock.instant())`. Devolve o perfil com `following=true`;
  - `Perfil deixarDeSeguir(String username, Long seguidorId)`: busca o alvo (404) e, se `existsById(id)`, executa `deleteById(id)`. Devolve o perfil com `following=false`.

  Injete o `Clock`.
- [X] T049 [US4] Extend `src/main/java/com/example/blog/perfil/web/PerfilController.java` com `POST /{username}/follow` e `DELETE /{username}/follow`, ambos com `@AuthenticationPrincipal Jwt jwt` e resposta 200 com `PerfilResponse`. A rota já exige autenticação pela `SecurityFilterChain` (T015), porque só `GET /api/profiles/*` é público.
- [X] T050 [US4] Extend `src/test/java/com/example/blog/perfil/web/PerfilControllerTest.java`, cobrindo:
  - `POST /api/profiles/jake/follow` com `jwt()` → 200 `following: true`;
  - sem token → 401;
  - `DELETE` com `jwt()` → 200 `following: false`.

**Checkpoint**: as quatro histórias funcionam de forma independente.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: validação ponta a ponta, documentação e verificação final.

- [X] T051 [P] Create `src/test/hurl/recorte-a.hurl`, a suíte Hurl com as variáveis `{{host}}` e `{{run_id}}`, que percorre os 14 cenários de [quickstart.md](quickstart.md) §3:
  - capturar os tokens com `[Captures]`;
  - verificar status, `jsonpath` dos campos (`$.user.token exists`, `$.user.bio == null`, `$.profile.following == true`) e `header "Content-Type" contains "charset=UTF-8"`;
  - usar usernames e e-mails com o sufixo `{{run_id}}`.
- [X] T052 [P] Create `README.md` na raiz, documentando:
  - pré-requisitos (JDK 25, Docker, Hurl opcional);
  - a criação do `.env` a partir de `.env.example`, incluindo `JWT_SECRET`;
  - `docker compose up -d db`, `./mvnw -q verify` e `./mvnw spring-boot:run`;
  - a execução da suíte Hurl;
  - a lista dos 7 endpoints com link para `specs/001-conduit-auth-perfis/contracts/`.
- [X] T053 Revisar todo o código novo contra `.claude/rules/tech.md` §15 e §27:
  - sem imports não usados nem curinga;
  - sem `System.out`;
  - sem `@Autowired` em campo;
  - nenhum log com e-mail, senha ou token;
  - nenhuma API `@Deprecated`;
  - Javadoc nas classes públicas de serviço e segurança.
- [X] T054 Run `./mvnw -q verify` e corrigir falhas até ficar verde. Relate só os erros e as últimas 50 linhas de log.
- [X] T055 Validar de ponta a ponta conforme [quickstart.md](quickstart.md):
  - `docker compose up -d db` e esperar `healthy` em `docker compose ps`;
  - subir a aplicação com as variáveis do `.env`, sem imprimi-las;
  - confirmar `V1` e `V2` com `success = true` em `flyway_schema_history`, pelo `psql` dentro do contêiner, em modo somente leitura, após `\conninfo`;
  - rodar `hurl --test` em `src/test/hurl/recorte-a.hurl` e relatar só as requisições reprovadas;
  - parar a aplicação ao final.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências.
- **Foundational (Phase 2)**: depende do Setup e bloqueia todas as histórias.
- **US1 (Phase 3)**: depende só da fase Foundational.
- **US2 (Phase 4)**:
  - depende da fase Foundational;
  - estende `UsuarioService`, `UsuarioController` e `UsuarioServiceTest`, criados na US1, então execute depois da US1;
  - nos testes, usa o cadastro (US1) para obter o token.
- **US3 (Phase 5)**:
  - depende da fase Foundational;
  - não depende de US1/US2 no código, mas os testes de integração criam usuários via `POST /api/users` (US1) ou direto pelo `UsuarioRepository`.
- **US4 (Phase 6)**: depende da US3, porque estende `PerfilService`, `PerfilController`, a migration `V2` e `Seguimento`.
- **Polish (Phase 7)**: depende de todas as histórias.

### Grafo de conclusão das histórias

```text
Setup → Foundational ─┬─> US1 (MVP) ──> US2
                      └─> US3 ──> US4
                                         └──> Polish (após US2 e US4)
```

### Within Each User Story

- Testes primeiro, falhando, e depois a implementação.
- Migrations e entidades → repositories → serviços → controllers.
- Feche a história no checkpoint antes de seguir para a próxima prioridade.

### Parallel Opportunities

- **Setup**: T002, T003 e T004 em paralelo, depois de T001.
- **Foundational**:
  - T008, T009, T011, T012 e T013 em paralelo;
  - T016 e T017 em paralelo depois de T012 e T014;
  - T010 depois de T008 e T009;
  - T015 depois de T011 a T014.
- **Entre histórias**: depois da fase Foundational, a cadeia US1→US2 e a cadeia US3→US4 podem andar em paralelo. Os arquivos não se sobrepõem, exceto as leituras de `UsuarioRepository`.

---

## Parallel Example: User Story 1

```bash
# Testes da US1 juntos:
Task: "T019 UsuarioServiceTest em src/test/java/com/example/blog/usuario/service/UsuarioServiceTest.java"
Task: "T020 UsuarioControllerTest em src/test/java/com/example/blog/usuario/web/UsuarioControllerTest.java"
Task: "T021 CadastroLoginIT em src/test/java/com/example/blog/usuario/CadastroLoginIT.java"

# DTOs da US1 juntos:
Task: "T022 CadastroRequest em src/main/java/com/example/blog/usuario/web/CadastroRequest.java"
Task: "T023 LoginRequest em src/main/java/com/example/blog/usuario/web/LoginRequest.java"
Task: "T024 UsuarioAutenticado + UsuarioResponse"
```

## Parallel Example: User Story 3

```bash
Task: "T041 Seguimento + SeguimentoId em src/main/java/com/example/blog/perfil/persistence/"
Task: "T042 SeguimentoRepository em src/main/java/com/example/blog/perfil/persistence/SeguimentoRepository.java"
Task: "T043 Perfil, PerfilNaoEncontradoException e PerfilResponse"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Fase 1 (Setup) e fase 2 (Foundational).
2. Fase 3 (US1): cadastro e login.
3. **Parar e validar**: `./mvnw -q verify` e os cenários 1 a 5 do quickstart.

### Incremental Delivery

1. Setup + Foundational: fundação pronta.
2. US1: MVP com cadastro e login.
3. US2: conta autenticada (consulta e atualização).
4. US3: perfis públicos.
5. US4: seguir e deixar de seguir.
6. Polish: Hurl, README e validação final.

---

## Notes

- [P] = arquivos diferentes, sem dependências pendentes.
- Migrations aplicadas são imutáveis. Se `V1`/`V2` já tiverem sido aplicadas fora do banco local ou commitadas, toda correção vira uma nova migration (`V3`...).
- Nenhuma operação destrutiva no banco ou no Docker (`down -v`, `DROP`) sem confirmação explícita do usuário.
- Faça um commit por tarefa ou grupo lógico, **somente quando o usuário pedir**.
