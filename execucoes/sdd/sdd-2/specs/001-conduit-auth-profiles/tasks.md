---

description: "Lista de tarefas do Recorte A da API Conduit"
---

# Tasks: Recorte A da API Conduit — autenticação, usuário e perfis

**Input**: Design documents from `/specs/001-conduit-auth-profiles/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

**Tests**: Incluídos. A spec exige uma suíte de testes de API (SC-001), e as regras do projeto (`tech.md` §15, §26 e Banco §8) exigem testes de comportamento e o teste de migrations em banco vazio. Escreva os testes de cada história antes da implementação e confirme que falham.

**Organization**: Tarefas agrupadas por história de usuário (US1–US5 da spec).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: história a que a tarefa pertence (US1…US5)
- Pacote raiz `com.example.blog`. Caminhos abaixo:
  - `MAIN` = `src/main/java/com/example/blog`
  - `TEST` = `src/test/java/com/example/blog`

## Convenções que valem para todas as tarefas

- Spring Boot 4.1.2, Jackson 3 (`tools.jackson.*`), `jakarta.*`, `@MockitoBean`. Confira na documentação 4.1 o pacote de `@WebMvcTest` e dos starters de teste antes de usar (research R10).
- Injeção por construtor com campos `final`. DTOs são records. `@Transactional` só nos serviços (`readOnly = true` nas leituras).
- Corpo de erro sempre `{"errors":{"<chave>":["<mensagem>"]}}` via `MAIN/shared/error/ErrorResponse.java`. Mensagens literais: `can't be blank`, `is invalid`, `has already been taken`, `can't be empty`, `not found`, `is missing`, `invalid`, `cannot follow yourself`.
- Rode o Maven com `-q` e mostre só erros e as últimas 50 linhas de log.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: dependências, configuração e variáveis de ambiente.

- [X] T001 Em `pom.xml`, adicionar sem `<version>` as dependências `org.springframework.boot:spring-boot-starter-security` e `org.springframework.boot:spring-boot-starter-oauth2-resource-server`, e `org.springframework.boot:spring-boot-starter-security-test` com `<scope>test</scope>`. Em `<build><plugins>`, adicionar `org.apache.maven.plugins:maven-failsafe-plugin` (versão gerenciada pelo parent) com os goals `integration-test` e `verify`, para executar as classes `*IT`.
- [X] T002 [P] Em `.env.example`, acrescentar a linha `JWT_SECRET=<segredo_local_com_32_ou_mais_caracteres>`. Se o `.env` local existir, acrescentar `JWT_SECRET` com 48 caracteres aleatórios gerados com o módulo `secrets` do Python, gravados direto no arquivo, sem imprimir o valor.
- [X] T003 [P] Em `src/main/resources/application.yml`, acrescentar:
  - `server.servlet.encoding.charset: UTF-8` e `server.servlet.encoding.force-response: true` (research R6);
  - `app.jwt.secret: ${JWT_SECRET}`, `app.jwt.issuer: conduit-blog` e `app.jwt.ttl: 24h`;
  - nível de log `WARN` para `org.springframework.security`.
- [X] T004 [P] Criar `src/test/resources/application.yml` com `app.jwt.secret` fixo e claramente fictício (ex.: `segredo-de-teste-ficticio-com-mais-de-32-bytes!!`), `app.jwt.issuer: conduit-blog` e `app.jwt.ttl: 24h`, para que os testes não dependam do `.env`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: esquema de usuários, tratamento de erros e segurança JWT, usados por todas as histórias.

**⚠️ CRITICAL**: nenhuma história começa antes do fim desta fase.

### Esquema e persistência de usuários

- [X] T005 Criar `src/main/resources/db/migration/V1__criar_tabela_users.sql` com a tabela `public.users`:
  - `id bigint generated always as identity`, `constraint pk_users primary key (id)`;
  - `username text not null`, `constraint uq_users_username unique (username)`, `constraint ck_users_username_not_blank check (btrim(username) <> '')`;
  - `email text not null`, `constraint ck_users_email_not_blank check (btrim(email) <> '')`;
  - `password_hash text not null`;
  - `bio text null` e `image text null` (opcionais pelo contrato);
  - `created_at timestamptz not null default now()` e `updated_at timestamptz not null default now()`;
  - em seguida, `create unique index uq_users_email on public.users (lower(email));`.
- [X] T006 [P] Criar `MAIN/user/persistence/UserEntity.java`:
  - `@Entity @Table(name = "users")`; id com `@GeneratedValue(strategy = IDENTITY)`;
  - campos `username`, `email`, `passwordHash` (coluna `password_hash`), `bio`, `image`, `createdAt`/`updatedAt` (`Instant`);
  - construtor `protected` sem argumentos e construtor de criação `(username, email, passwordHash, Instant agora)` com `bio`/`image` `null`;
  - métodos `alterarEmail`, `alterarUsername`, `alterarSenha(String hash)`, `alterarBio(String)`, `alterarImage(String)` e `registrarAtualizacao(Instant)`;
  - getters somente leitura; `equals`/`hashCode` pelo id (instâncias não persistidas só são iguais a si mesmas).
- [X] T007 Criar `MAIN/user/persistence/UserRepository.java` (`JpaRepository<UserEntity, Long>`) com:
  - `Optional<UserEntity> findByUsername(String)` e `Optional<UserEntity> findByEmailIgnoreCase(String)`;
  - `boolean existsByUsername(String)` e `boolean existsByEmailIgnoreCase(String)`;
  - `boolean existsByUsernameAndIdNot(String, Long)` e `boolean existsByEmailIgnoreCaseAndIdNot(String, Long)`.
  - Depende de T006.
- [X] T008 [P] Criar `MAIN/shared/config/ClockConfig.java` com o bean `Clock` (`Clock.systemUTC()`), usado para `createdAt`/`updatedAt` e para a emissão do token.

### Tratamento de erros (`shared/error`)

- [X] T009 [P] Criar `MAIN/shared/error/ErrorResponse.java`, `record ErrorResponse(Map<String, List<String>> errors)`, com as fábricas `of(String chave, String mensagem)` e `of(Map<String, List<String>>)`, que preservam a ordem de inserção (`LinkedHashMap`) e copiam para coleções imutáveis.
- [X] T010 [P] Criar as exceções não verificadas em `MAIN/shared/error/`. Cada uma carrega um `Map<String, List<String>>` de erros, exceto as duas de 401:
  - `ValidationException.java` (422);
  - `ConflictException.java` (409; aceita uma ou várias chaves com `has already been taken`);
  - `NotFoundException.java` (404; construtor `(String recurso)` gera `{recurso: ["not found"]}`);
  - `InvalidCredentialsException.java` (401 `credentials: invalid`);
  - `InvalidTokenException.java` (401 `token: is invalid`).
- [X] T011 Criar `MAIN/shared/error/GlobalExceptionHandler.java` (`@RestControllerAdvice`), conforme research R5:
  - `MethodArgumentNotValidException` → 422. A chave é o último segmento do campo (`user.email` → `email`). Erro no próprio campo `user` (ausente) → chave `body` com `can't be empty`. Várias mensagens por chave são agrupadas.
  - `HttpMessageNotReadableException` → 422 `{"errors":{"body":["can't be empty"]}}`.
  - `ValidationException` → 422. `ConflictException` → 409. `NotFoundException` → 404. `InvalidCredentialsException` → 401. `InvalidTokenException` → 401.
  - `DataIntegrityViolationException` → 409, com a chave pelo nome da restrição: `uq_users_username` → `username`, `uq_users_email` → `email`. Outras restrições → 500.
  - `Exception` → 500 `{"errors":{"server":["unexpected error"]}}`, registrado com SLF4J (`log.error` com a exceção como último argumento), sem detalhes ao cliente.
  - Depende de T009 e T010.

### Segurança JWT (`shared/security`)

- [X] T012 [P] Criar `MAIN/shared/security/JwtProperties.java`: `@ConfigurationProperties("app.jwt") @Validated record JwtProperties(@NotBlank @Size(min = 32) String secret, @NotBlank String issuer, @NotNull Duration ttl)`. Habilitar com `@ConfigurationPropertiesScan` em `MAIN/BlogApplication.java`.
- [X] T013 Criar `MAIN/shared/security/JwtConfig.java` (`@Configuration`) com os beans (depende de T012):
  - `SecretKey` HmacSHA256 a partir de `secret` em UTF-8;
  - `JwtEncoder` (`NimbusJwtEncoder.withSecretKey(...)`);
  - `JwtDecoder` (`NimbusJwtDecoder.withSecretKey(...).macAlgorithm(MacAlgorithm.HS256)`) com `JwtValidators.createDefaultWithIssuer(issuer)`, que valida `exp` e `iss`;
  - `PasswordEncoder` (`BCryptPasswordEncoder`).
- [X] T014 Criar `MAIN/shared/security/TokenService.java` (`@Service`) com `String emitir(Long userId)`. Usa o `JwtEncoder`, cabeçalho `alg=HS256`, `sub=userId`, `iss`, `iat=clock.instant()` e `exp=iat+ttl`. Depende de T008 e T013.
- [X] T015 [P] Criar `MAIN/shared/security/TokenAuthorizationHeaderResolver.java`, implementação de `BearerTokenResolver` (research R2):
  - devolve o token só quando o cabeçalho `Authorization` começa com `Token ` (com espaço);
  - devolve `null` se o cabeçalho faltar, se o esquema for outro (ex.: `Bearer`) ou se a requisição for `POST /api/users` ou `POST /api/users/login`.
- [X] T016 [P] Criar `MAIN/shared/security/TokenAuthenticationEntryPoint.java`, implementação de `AuthenticationEntryPoint` (research R3):
  - status 401;
  - `{"errors":{"token":["is invalid"]}}` quando a exceção for `OAuth2AuthenticationException` (inclui `InvalidBearerTokenException`), senão `{"errors":{"token":["is missing"]}}`;
  - corpo serializado com o `JsonMapper` injetado e `Content-Type` `application/json`.
- [X] T017 Criar `MAIN/shared/security/SecurityConfig.java` com um bean `SecurityFilterChain` (depende de T013, T015 e T016):
  - CSRF desabilitado, sessão `STATELESS`, `httpBasic` e `formLogin` desabilitados;
  - `permitAll` para `POST /api/users`, `POST /api/users/login`, `GET /api/profiles/*` e `GET /actuator/health`; `authenticated()` para o resto;
  - `oauth2ResourceServer(jwt)` com o `bearerTokenResolver` de T015 e o `authenticationEntryPoint` de T016;
  - `exceptionHandling().authenticationEntryPoint(T016)`.
- [X] T018 [P] Criar `MAIN/shared/security/CurrentUserId.java` (`@Component`):
  - `Long exigir()`: lê o `Jwt` do `SecurityContextHolder`; `sub` ausente ou não numérico → `InvalidTokenException`;
  - `Long obterOuNulo()`: devolve `null` se a requisição for anônima.

### Testes de fundação

- [X] T019 [P] Criar `TEST/MigrationsIT.java`: `@SpringBootTest` + `@Import(TestcontainersConfiguration.class)` em banco vazio (`postgres:18.6`). Verifica que a aplicação sobe com `ddl-auto: validate` e que `flyway_schema_history` lista todas as migrations com `success = true` (consulta via `JdbcTemplate`).
- [X] T020 [P] Criar `TEST/shared/security/TokenServiceTest.java` (unitário, `Clock` fixo, segredo fictício). Cobre:
  - o token emitido é aceito pelo `JwtDecoder` e tem `sub` igual ao id e `iss` igual ao emissor;
  - um token expirado (relógio avançado além de `ttl`) é rejeitado;
  - um token assinado com outro segredo é rejeitado.
- [X] T021 [P] Criar `TEST/shared/security/TokenAuthorizationHeaderResolverTest.java` (unitário, `MockHttpServletRequest`):
  - `Token abc` → `abc`;
  - `Bearer abc` → `null`;
  - sem cabeçalho → `null`;
  - `POST /api/users/login` com `Token abc` → `null`.

**Checkpoint**: `./mvnw -q verify` passa; base pronta para as histórias.

---

## Phase 3: User Story 1 - Cadastro de novo usuário (Priority: P1) 🎯 MVP

**Goal**: `POST /api/users` cria a conta e devolve o User com token (FR-001 a FR-004, FR-027 a FR-031).

**Independent Test**: `POST /api/users` válido → `201` com `user.token`, `bio: null`, `image: null`; e-mail ou nome repetido → `409`; campo ausente → `422`.

### Tests for User Story 1 ⚠️

- [X] T022 [P] [US1] Criar `TEST/user/service/UserServiceTest.java` com Mockito (`UserRepository`, `PasswordEncoder`, `TokenService`, `Clock` fixo). Testes de cadastro:
  - `deveCadastrarUsuarioComSenhaCriptografadaEToken`;
  - `deveRemoverEspacosDasPontasDeUsernameEEmail`;
  - `deveLancarConflitoComUsernameEEmailQuandoAmbosJaExistem`, que espera as chaves `email` e `username` juntas;
  - `deveLancarConflitoSoComEmailQuandoEmailJaExisteIgnorandoMaiusculas`.
- [X] T023 [P] [US1] Criar `TEST/user/UserApiIT.java` (`@SpringBootTest`, `@AutoConfigureMockMvc`, `@Import(TestcontainersConfiguration.class)`; cada teste usa nomes e e-mails únicos e fictícios). Cenários de cadastro:
  - `201` com o corpo exato de FR-001 (`bio` e `image` presentes como `null`, `token` não vazio) e `Content-Type` compatível com `application/json;charset=UTF-8`;
  - `409` `{"errors":{"username":["has already been taken"]}}`;
  - `409` para e-mail com maiúsculas diferentes;
  - `422` `email: can't be blank` sem `email`;
  - `422` `email: is invalid` para `"nao-e-email"`;
  - `422` `username: can't be blank` para `"   "`;
  - `422` `body: can't be empty` para corpo `{`, corpo vazio e `{}` sem `user`;
  - a resposta nunca contém `password` nem `password_hash`.

### Implementation for User Story 1

- [X] T024 [P] [US1] Criar `MAIN/user/web/RegisterUserRequest.java`: `record RegisterUserRequest(@NotNull @Valid NewUserFields user)` e o record aninhado `NewUserFields`:
  - `@NotBlank(message = "can't be blank") String username`;
  - `@NotBlank(message = "can't be blank") @Email(message = "is invalid") String email`;
  - `@NotBlank(message = "can't be blank") String password`.
- [X] T025 [P] [US1] Criar `MAIN/user/web/UserResponse.java`: `record UserResponse(UserBody user)`, com `record UserBody(String email, String token, String username, String bio, String image)` e a fábrica `from(AuthenticatedUser)`. A serialização deve incluir `bio`/`image` `null` (não configurar `NON_NULL`).
- [X] T026 [P] [US1] Criar `MAIN/user/service/AuthenticatedUser.java`: `record AuthenticatedUser(Long id, String email, String username, String bio, String image, String token)`.
- [X] T027 [US1] Criar `MAIN/user/service/UserService.java` (`@Service`) com `@Transactional AuthenticatedUser cadastrar(String username, String email, String password)`:
  - aplica `trim` em `username` e `email`;
  - acumula conflitos (`existsByEmailIgnoreCase` → `email`, `existsByUsername` → `username`) e lança um único `ConflictException` com todas as chaves;
  - grava `UserEntity` com `passwordEncoder.encode(password)` e `clock.instant()`;
  - devolve o usuário com `tokenService.emitir(id)`.
  - Depende de T007, T014 e T026.
- [X] T028 [US1] Criar `MAIN/user/web/UserController.java` (`@RestController @RequestMapping("/api")`) com `@PostMapping("/users")`: recebe `@Valid @RequestBody RegisterUserRequest`, chama `UserService.cadastrar` e responde `ResponseEntity.status(201)` com `UserResponse`. Depende de T024, T025 e T027.
- [X] T029 [P] [US1] Criar `TEST/user/web/UserControllerTest.java` (`@WebMvcTest(UserController.class)`, `@Import` de `SecurityConfig`, `JwtConfig`, `TokenAuthorizationHeaderResolver`, `TokenAuthenticationEntryPoint` e `GlobalExceptionHandler`; `@MockitoBean UserService`). Cobre:
  - `201` com o corpo User;
  - `422` por campo em branco, com a chave do campo;
  - `422 body` para JSON malformado;
  - `409` quando o serviço lança `ConflictException`.

**Checkpoint**: US1 funcional; `UserApiIT` (cenários de cadastro) passa.

---

## Phase 4: User Story 2 - Login de usuário existente (Priority: P1)

**Goal**: `POST /api/users/login` devolve o User com novo token (FR-005 a FR-007).

**Independent Test**: cadastrar, logar com as credenciais → `200` com token; senha errada → `401 credentials: invalid`; sem `password` → `422`.

### Tests for User Story 2 ⚠️

- [X] T030 [P] [US2] Em `TEST/user/service/UserServiceTest.java`, acrescentar testes de `autenticar`:
  - `deveAutenticarComEmailIgnorandoMaiusculas`;
  - `deveLancarCredenciaisInvalidasQuandoEmailNaoExiste`;
  - `deveLancarCredenciaisInvalidasQuandoSenhaNaoConfere`.
- [X] T031 [P] [US2] Em `TEST/user/UserApiIT.java`, acrescentar cenários de login:
  - `200` com o objeto User e `token` não vazio;
  - `200` com e-mail em maiúsculas;
  - `401` `{"errors":{"credentials":["invalid"]}}` para senha errada e para e-mail inexistente;
  - `422` `password: can't be blank` sem `password`;
  - `422` `email: can't be blank` sem `email`;
  - login com cabeçalho `Authorization: Token invalido` ainda responde `200`, porque o token é ignorado na rota pública.

### Implementation for User Story 2

- [X] T032 [P] [US2] Criar `MAIN/user/web/LoginRequest.java`: `record LoginRequest(@NotNull @Valid LoginFields user)`, com `record LoginFields(@NotBlank(message = "can't be blank") String email, @NotBlank(message = "can't be blank") String password)`.
- [X] T033 [US2] Em `MAIN/user/service/UserService.java`, acrescentar `@Transactional(readOnly = true) AuthenticatedUser autenticar(String email, String password)`:
  - `findByEmailIgnoreCase(email.trim())` e `passwordEncoder.matches`;
  - qualquer falha → `InvalidCredentialsException`, sem indicar qual campo falhou;
  - sucesso → `tokenService.emitir(id)`.
- [X] T034 [US2] Em `MAIN/user/web/UserController.java`, acrescentar `@PostMapping("/users/login")`, que recebe `@Valid @RequestBody LoginRequest` e responde `200` com `UserResponse`. Acrescentar em `TEST/user/web/UserControllerTest.java` os casos `200` e `422` do login.

**Checkpoint**: US1 e US2 funcionam de forma independente.

---

## Phase 5: User Story 3 - Consulta e atualização do usuário autenticado (Priority: P2)

**Goal**: `GET /api/user` e `PUT /api/user` (FR-008 a FR-019).

**Independent Test**: com token válido, `GET /api/user` → `200`; `PUT /api/user` com `bio`/`image` → `200` com os valores novos; sem token → `401 token: is missing`.

### Tests for User Story 3 ⚠️

- [X] T035 [P] [US3] Em `TEST/user/service/UserServiceTest.java`, acrescentar testes de `buscarAtual` e `atualizar`:
  - usuário inexistente → `InvalidTokenException`;
  - atualiza só os campos presentes;
  - `bio` presente com `null` limpa o valor;
  - `email`/`username`/`password` presentes em branco ou `null` → `ValidationException` com `can't be blank`;
  - nenhum campo presente → `ValidationException` `{"user":["can't be empty"]}`;
  - `email` inválido → `is invalid`;
  - conflito com outro usuário → `ConflictException`;
  - manter o próprio e-mail ou nome não gera conflito;
  - nova senha é gravada com hash.
- [X] T036 [P] [US3] Em `TEST/user/UserApiIT.java`, acrescentar cenários:
  - `GET /api/user` com `Authorization: Token <jwt>` → `200` com o User e um token válido;
  - sem cabeçalho → `401` `{"errors":{"token":["is missing"]}}`;
  - `Bearer <jwt>` → `401 is missing`;
  - `Token abc.def.ghi` → `401` `{"errors":{"token":["is invalid"]}}`;
  - token expirado (emitido com `Clock` no passado via `JwtEncoder` do contexto) → `401 is invalid`;
  - token de id inexistente → `401 is invalid`;
  - `PUT` com `bio` e `image` → `200` e valores persistidos (confirmados por novo `GET`);
  - `PUT` com `{"user":{"bio":null}}` → `bio: null`;
  - `PUT` com `{"user":{}}` → `422 user: can't be empty`;
  - `PUT` com e-mail de outro usuário → `409 email: has already been taken`;
  - `PUT` com nova senha → login antigo `401` e novo `200`;
  - `PUT` sem token → `401`.

### Implementation for User Story 3

- [X] T037 [P] [US3] Criar `MAIN/user/service/UserUpdate.java`: `record UserUpdate(boolean hasEmail, String email, boolean hasUsername, String username, boolean hasPassword, String password, boolean hasBio, String bio, boolean hasImage, String image)`, com o método `boolean isVazio()` (nenhum `has*` verdadeiro).
- [X] T038 [P] [US3] Criar `MAIN/user/web/UpdateUserRequest.java`, `record UpdateUserRequest(JsonNode user)` (`tools.jackson.databind.JsonNode`), com o método `UserUpdate toUserUpdate()` (research R7):
  - `user` ausente ou que não seja objeto → `ValidationException` `{"body":["can't be empty"]}`;
  - para cada campo aceito (`email`, `username`, `password`, `bio`, `image`), `has*` = `user.has(campo)` e valor = texto, ou `null` se o nó for `null`;
  - um valor não textual em `email`/`username`/`password` → `ValidationException` `can't be blank` naquela chave;
  - campos desconhecidos são ignorados.
- [X] T039 [US3] Em `MAIN/user/service/UserService.java`, acrescentar:
  - `@Transactional(readOnly = true) AuthenticatedUser buscarAtual(Long userId)`: `findById`, ausente → `InvalidTokenException`; emite novo token.
  - `@Transactional AuthenticatedUser atualizar(Long userId, UserUpdate update)`:
    - `isVazio()` → 422 `user: can't be empty`;
    - para `email`/`username`/`password` presentes: `null` ou em branco → 422 `can't be blank`;
    - `email` com `trim` e validado com o mesmo critério do `@Email` → senão 422 `is invalid`. Usar um validador simples e documentado em `MAIN/user/service/EmailFormat.java`, alinhado ao `@Email` do Hibernate Validator.
    - acumula erros 422 por chave antes de lançar;
    - conflitos com `existsByEmailIgnoreCaseAndIdNot` e `existsByUsernameAndIdNot` → 409;
    - aplica os campos presentes (`bio`/`image` podem ser `null`), `alterarSenha(encode)` e `registrarAtualizacao(clock.instant())`;
    - emite novo token.
- [X] T040 [US3] Em `MAIN/user/web/UserController.java`, acrescentar:
  - `@GetMapping("/user")`: `currentUserId.exigir()` → `buscarAtual` → `200 UserResponse`;
  - `@PutMapping("/user")`: `@RequestBody UpdateUserRequest` → `toUserUpdate()` → `atualizar` → `200 UserResponse`.
  - Acrescentar em `TEST/user/web/UserControllerTest.java`: `401 is missing` sem token, `200` com `@WithMockUser`/`jwt()` de `spring-security-test` e `422` com `{"user":{}}`.

**Checkpoint**: US1–US3 funcionam; `UserApiIT` passa por completo.

---

## Phase 6: User Story 4 - Consulta de perfil público (Priority: P2)

**Goal**: `GET /api/profiles/{username}` com `following` calculado (FR-020 a FR-022). Cria a tabela `follows`, necessária para calcular `following`.

**Independent Test**: cadastrar `ana`, `GET /api/profiles/ana` sem token → `200 following: false`; nome inexistente → `404 profile: not found`.

### Tests for User Story 4 ⚠️

- [X] T041 [P] [US4] Criar `TEST/profile/service/ProfileServiceTest.java` (Mockito: `UserRepository`, `FollowRepository`). Testes de `buscarPerfil`:
  - anônimo → `following=false`;
  - autenticado que segue → `true`;
  - autenticado que não segue → `false`;
  - nome inexistente → `NotFoundException("profile")`;
  - nome com maiúsculas diferentes → não encontrado (sensível a maiúsculas).
- [X] T042 [P] [US4] Criar `TEST/profile/ProfileApiIT.java` (`@SpringBootTest`, `@AutoConfigureMockMvc`, Testcontainers). Cria os usuários via `POST /api/users` com nomes únicos. Cenários:
  - `GET /api/profiles/{u}` sem token → `200` `{"profile":{"username":u,"bio":null,"image":null,"following":false}}`;
  - com token válido de outro usuário → `200 following:false`;
  - com `Authorization: Token abc.def.ghi` → `401 token: is invalid`;
  - com `Bearer <jwt>` → `200` anônimo;
  - nome inexistente → `404` `{"errors":{"profile":["not found"]}}`.

### Implementation for User Story 4

- [X] T043 [US4] Criar `src/main/resources/db/migration/V2__criar_tabela_follows.sql` com `public.follows`:
  - `follower_id bigint not null`, `constraint fk_follows_follower foreign key (follower_id) references public.users (id)`;
  - `followed_id bigint not null`, `constraint fk_follows_followed foreign key (followed_id) references public.users (id)`;
  - `created_at timestamptz not null default now()`;
  - `constraint pk_follows primary key (follower_id, followed_id)` e `constraint ck_follows_not_self check (follower_id <> followed_id)`;
  - em seguida, `create index ix_follows_followed_id on public.follows (followed_id);`.
- [X] T044 [P] [US4] Criar `MAIN/profile/persistence/FollowId.java`: `@Embeddable`, com `Long followerId` (coluna `follower_id`) e `Long followedId` (coluna `followed_id`), construtor protegido sem argumentos e `equals`/`hashCode` pelos dois campos.
- [X] T045 [US4] Criar `MAIN/profile/persistence/FollowEntity.java` (`@Entity @Table(name = "follows")`, `@EmbeddedId FollowId id`, `Instant createdAt`, sem `@ManyToOne`) e `MAIN/profile/persistence/FollowRepository.java` (`JpaRepository<FollowEntity, FollowId>`). Depende de T044.
- [X] T046 [P] [US4] Criar `MAIN/profile/service/Profile.java`: `record Profile(String username, String bio, String image, boolean following)`.
- [X] T047 [US4] Criar `MAIN/profile/service/ProfileService.java` (`@Service`) com `@Transactional(readOnly = true) Profile buscarPerfil(String username, Long viewerIdOuNulo)`:
  - `userRepository.findByUsername(username)`, ausente → `NotFoundException("profile")`;
  - `following = viewerIdOuNulo != null && followRepository.existsById(new FollowId(viewer, alvo.id))`.
  - Depende de T007, T045 e T046.
- [X] T048 [P] [US4] Criar `MAIN/profile/web/ProfileResponse.java`: `record ProfileResponse(ProfileBody profile)`, com `record ProfileBody(String username, String bio, String image, boolean following)` e a fábrica `from(Profile)`.
- [X] T049 [US4] Criar `MAIN/profile/web/ProfileController.java` (`@RestController @RequestMapping("/api/profiles")`) com `@GetMapping("/{username}")`: `currentUserId.obterOuNulo()` → `buscarPerfil` → `200 ProfileResponse`. Depende de T047 e T048.
- [X] T050 [P] [US4] Criar `TEST/profile/web/ProfileControllerTest.java` (`@WebMvcTest(ProfileController.class)` com as mesmas importações de segurança de T029; `@MockitoBean ProfileService`). Cobre: `200` anônimo com o corpo Profile e `404` quando o serviço lança `NotFoundException`.

**Checkpoint**: US4 funcional; `MigrationsIT` passa com `V1` e `V2`.

---

## Phase 7: User Story 5 - Seguir e deixar de seguir usuários (Priority: P3)

**Goal**: `POST`/`DELETE /api/profiles/{username}/follow`, idempotentes (FR-023 a FR-026).

**Independent Test**: A segue B → `following: true`; repetir → `true`; A deixa de seguir → `false`; repetir → `false`; seguir a si mesmo → `422`.

### Tests for User Story 5 ⚠️

- [X] T051 [P] [US5] Em `TEST/profile/service/ProfileServiceTest.java`, acrescentar testes:
  - `seguir` grava a relação quando não existe;
  - `seguir` não grava de novo quando já existe;
  - `seguir` a si mesmo → `ValidationException {"profile":["cannot follow yourself"]}`;
  - `seguir` alvo inexistente → `NotFoundException`;
  - `deixarDeSeguir` remove quando existe e não falha quando não existe;
  - `deixarDeSeguir` a si mesmo → `following=false`;
  - seguidor inexistente (token de usuário removido) → `InvalidTokenException`.
- [X] T052 [P] [US5] Em `TEST/profile/ProfileApiIT.java`, acrescentar cenários:
  - `POST /api/profiles/B/follow` com token de A → `200 following:true`; repetir → `200 true`;
  - `GET /api/profiles/B` com token de A → `following:true`;
  - `DELETE` → `200 following:false`; repetir → `200 false`;
  - `POST /api/profiles/A/follow` com token de A → `422 profile: cannot follow yourself`;
  - `POST` e `DELETE` sem token → `401 token: is missing`;
  - `POST /api/profiles/inexistente/follow` → `404 profile: not found`.

### Implementation for User Story 5

- [X] T053 [US5] Em `MAIN/profile/service/ProfileService.java`, acrescentar:
  - `@Transactional Profile seguir(String username, Long followerId)`:
    - seguidor via `userRepository.existsById`, ausente → `InvalidTokenException`;
    - alvo via `findByUsername`, ausente → `NotFoundException("profile")`;
    - alvo igual ao seguidor → `ValidationException {"profile":["cannot follow yourself"]}`;
    - se `!existsById(id)`, `save(new FollowEntity(id, clock.instant()))`;
    - devolve `following=true`.
  - `@Transactional Profile deixarDeSeguir(String username, Long followerId)`:
    - mesmas verificações de existência (sem a regra de si mesmo);
    - `deleteById` só se `existsById`;
    - devolve `following=false`.
- [X] T054 [US5] Em `MAIN/profile/web/ProfileController.java`, acrescentar `@PostMapping("/{username}/follow")` e `@DeleteMapping("/{username}/follow")`. Ambos usam `currentUserId.exigir()` e respondem `200 ProfileResponse`. Acrescentar em `TEST/profile/web/ProfileControllerTest.java` os casos `200` com `jwt()` e `401` sem token.
- [X] T055 [US5] Em `MAIN/shared/error/GlobalExceptionHandler.java`, tratar a violação de `pk_follows` (corrida em `seguir`) sem erro 500. Preferível: em `ProfileService.seguir`, capturar `DataIntegrityViolationException` em torno do `saveAndFlush` e devolver `following=true` (research R9). Cobrir com um teste unitário em `TEST/profile/service/ProfileServiceTest.java`.

**Checkpoint**: todas as histórias funcionam; `ProfileApiIT` passa.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T056 [P] Criar `TEST/shared/ContentTypeIT.java` (Testcontainers): verificar que respostas `201`, `200`, `401` (do entry point), `404`, `409` e `422` têm `Content-Type` igual a `application/json` com `charset=UTF-8`, sem diferenciar maiúsculas (FR-027, research R6).
- [X] T057 [P] Atualizar `README.md` (criar se não existir) com:
  - pré-requisitos e criação do `.env` (incluindo `JWT_SECRET`);
  - `docker compose up -d db`, como executar a aplicação e `./mvnw -q verify`;
  - lista das 7 rotas e link para `specs/001-conduit-auth-profiles/contracts/openapi.yaml`.
- [X] T058 [P] Revisar Javadoc das classes públicas de serviço (`UserService`, `ProfileService`, `TokenService`) e de `GlobalExceptionHandler`, descrevendo o contrato e as exceções lançadas.
- [X] T059 Conferir que nenhum log registra senha, hash ou token. Remover imports não usados e código morto. Confirmar que não há `System.out` nem `@Autowired` em campos. Arquivos: `src/main/java/com/example/blog/**`.
- [X] T060 Rodar `./mvnw -q verify` e corrigir falhas (mostrar só erros e as últimas 50 linhas).
- [X] T061 Executar os 20 cenários manuais de `specs/001-conduit-auth-profiles/quickstart.md` contra a aplicação local (banco Docker saudável, `\conninfo` confirmado antes de qualquer `psql`) e registrar o resultado, mostrando só as requisições reprovadas.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências.
- **Foundational (Phase 2)**: depende do Setup e bloqueia todas as histórias.
- **US1 (Phase 3)** e **US2 (Phase 4)**: dependem só da Fase 2. Compartilham `UserService.java`, `UserController.java` e `UserApiIT.java`, então rode-as em sequência (US1 → US2).
- **US3 (Phase 5)**: depende da Fase 2. Para obter token nos testes ponta a ponta, usa o cadastro de US1; na prática, rode depois de US1.
- **US4 (Phase 6)**: depende da Fase 2 e usa o cadastro de US1 nos testes ponta a ponta. Os arquivos de código não conflitam com US2/US3, então pode rodar em paralelo a elas.
- **US5 (Phase 7)**: depende de US4 (tabela `follows`, `ProfileService` e `ProfileController`).
- **Polish (Phase 8)**: depois de todas as histórias.

### Grafo das histórias

```text
Setup → Foundational ─┬─ US1 ─┬─ US2
                      │       ├─ US3
                      │       └─ US4 ── US5
                      └────────────────────── Polish (após todas)
```

### Within Each User Story

- Testes primeiro (devem falhar), depois DTOs/records, serviço, controller e fatia web.
- Tarefas sem [P] no mesmo arquivo seguem a ordem dos IDs.

### Parallel Opportunities

- Fase 1: T002, T003 e T004 em paralelo, depois de T001.
- Fase 2: T006, T008, T009, T010, T012, T015, T016 e T018 em paralelo. Depois, T007, T011, T013 → T014 → T017. Os testes T019, T020 e T021 rodam em paralelo.
- US1: T022, T023, T024, T025 e T026 em paralelo; depois T027 → T028; T029 em paralelo a T028, após T024–T025.
- US4: T041, T042, T044, T046 e T048 em paralelo; T043 → T045 → T047 → T049.
- US4 (pacote `profile`) pode avançar em paralelo a US2/US3 (pacote `user`).

---

## Parallel Example: User Story 1

```bash
# Testes de US1 juntos:
Task: "UserServiceTest (cadastro) em src/test/java/com/example/blog/user/service/UserServiceTest.java"
Task: "UserApiIT (cadastro) em src/test/java/com/example/blog/user/UserApiIT.java"

# DTOs e records de US1 juntos:
Task: "RegisterUserRequest em src/main/java/com/example/blog/user/web/RegisterUserRequest.java"
Task: "UserResponse em src/main/java/com/example/blog/user/web/UserResponse.java"
Task: "AuthenticatedUser em src/main/java/com/example/blog/user/service/AuthenticatedUser.java"
```

## Parallel Example: User Story 4

```bash
Task: "FollowId em src/main/java/com/example/blog/profile/persistence/FollowId.java"
Task: "Profile em src/main/java/com/example/blog/profile/service/Profile.java"
Task: "ProfileResponse em src/main/java/com/example/blog/profile/web/ProfileResponse.java"
Task: "ProfileServiceTest em src/test/java/com/example/blog/profile/service/ProfileServiceTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Fase 1 (Setup) e Fase 2 (Foundational).
2. Fase 3 (US1, cadastro com token).
3. **Parar e validar**: `./mvnw -q verify` e cenários 1–3 do quickstart.

### Incremental Delivery

1. Setup + Foundational → base pronta (migrations, erros, JWT).
2. US1 → cadastro (MVP).
3. US2 → login (autenticação completa).
4. US3 → conta do usuário.
5. US4 → perfis públicos.
6. US5 → seguir e deixar de seguir.
7. Polish → `Content-Type`, README e validação do quickstart.

---

## Notes

- Toda mudança de esquema vai em migration nova. `V1` e `V2` só podem ser editadas enquanto não forem commitadas nem compartilhadas.
- Testes usam somente Testcontainers (`postgres:18.6`), nunca o banco de desenvolvimento, e dados fictícios criados pelo próprio teste.
- Nunca imprimir o conteúdo do `.env` nem o `JWT_SECRET`.
- Faça commit só quando o usuário pedir.
