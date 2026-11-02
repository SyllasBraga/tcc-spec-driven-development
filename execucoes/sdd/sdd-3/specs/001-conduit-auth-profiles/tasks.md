---

description: "Task list for Recorte A da API Conduit — Autenticação, Usuário e Perfis"
---

# Tasks: Recorte A da API Conduit — Autenticação, Usuário e Perfis

**Input**: Design documents from `specs/001-conduit-auth-profiles/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/openapi.yaml](./contracts/openapi.yaml), [quickstart.md](./quickstart.md)

**Tests**: incluídos. A spec exige suíte automatizada cobrindo 100% dos cenários de aceitação (SC-001), e `.claude/rules/tech.md` §11, §26 e "Testes e migrations" §8 exigem testes unitários (`*Test`), de integração (`*IT`, Testcontainers `postgres:18.6`) e teste de migrations em banco vazio.

**Organization**: tarefas agrupadas por história de usuário, para cada uma ser implementada e testada de forma independente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência de tarefa incompleta)
- **[Story]**: história de usuário da tarefa (US1, US2, US3, US4)

## Path Conventions

Projeto Spring Boot de módulo único (ver plan.md). Pacote raiz `com.example.blog`.

- Código: `src/main/java/com/example/blog/`
- Recursos: `src/main/resources/`
- Testes: `src/test/java/com/example/blog/` (espelha o código) e `src/test/resources/`

## Convenções que valem para todas as tarefas

- Java 25, Spring Boot 4.1.2, Jackson 3 (*databind* em `tools.jackson`; anotações em `com.fasterxml.jackson.annotation`), namespace `jakarta.*`, sem APIs depreciadas. Em dúvida sobre pacote ou anotação do Boot 4, conferir a documentação da 4.1.2 antes de escrever.
- 4 espaços, UTF-8, até 120 colunas, imports explícitos, chaves em todo bloco, injeção por construtor com campos `final`.
- DTOs são records; entidades JPA nunca saem da API; `@Transactional` só em services (`readOnly = true` em leituras).
- Todo mapeamento HTTP declara `produces = MediaTypes.APPLICATION_JSON_UTF8`.
- Todo erro sai como `{"errors":{"<chave>":["<mensagem>"]}}` (research R6).
- Logging com SLF4J, mensagens parametrizadas, nunca registrar senha, hash, token ou e-mail.
- Rodar Maven sempre com `-q` (`./mvnw -q verify`) e mostrar só erros e as últimas 50 linhas de log.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: dependências, configuração e segredos.

- [X] T001 Atualizar `pom.xml`: adicionar, **sem versão**, `org.springframework.boot:spring-boot-starter-security`, `org.springframework.boot:spring-boot-starter-oauth2-resource-server` e, com `<scope>test</scope>`, `org.springframework.boot:spring-boot-starter-security-test` (conferir o nome do starter de teste na documentação do Boot 4.1.2; se não existir, usar `org.springframework.security:spring-security-test` em escopo `test`). Em `<build><plugins>`, adicionar `org.apache.maven.plugins:maven-failsafe-plugin` (versão gerenciada pelo parent) com `<executions>` contendo os goals `integration-test` e `verify`, para os testes `*IT` rodarem em `./mvnw -q verify`. Confirmar com `./mvnw -q compile`.
- [X] T002 [P] Em `.env.example`, acrescentar a linha `JWT_SECRET=<base64_de_32_bytes_aleatorios>`. Se `.env` não existir, criá-lo a partir de `.env.example` conforme `.claude/rules/tech.md` §2.2 (`POSTGRES_DB=sdd_3`, `POSTGRES_USER=agente`, `POSTGRES_PASSWORD` com 32 caracteres aleatórios de `secrets`, `DB_PORT=5432`, `DB_URL=jdbc:postgresql://localhost:5432/sdd_3`). Em `.env`, gravar `JWT_SECRET` com `base64.b64encode(secrets.token_bytes(32))` gerado por um script Python que escreve direto no arquivo, sem imprimir o valor. Nunca exibir o conteúdo do `.env`.
- [X] T003 [P] Em `src/main/resources/application.yml`, acrescentar o bloco `app.jwt` com `secret: ${JWT_SECRET}`, `issuer: conduit-blog` e `ttl: 24h`, mantendo o restante do arquivo. Criar `src/main/resources/application-dev.yml` com `spring.config.import: optional:file:.env[.properties]` (research R13).
- [X] T004 [P] Criar `src/test/resources/application.yml` com `app.jwt.secret` igual à codificação Base64 de um texto fictício de exatamente 32 bytes ASCII, claramente de teste (por exemplo, `teste-somente-local-nao-usar-32b`), `app.jwt.issuer: conduit-blog-test` e `app.jwt.ttl: 24h`. Não definir datasource: os testes usam `TestcontainersConfiguration` com `@ServiceConnection`.

**Checkpoint**: `./mvnw -q compile` passa.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: esquema do banco, formato de erro, segurança e emissão de token, usados por todas as histórias.

**⚠️ CRITICAL**: nenhuma história começa antes desta fase.

### Esquema (migrations)

- [X] T005 Criar `src/main/resources/db/migration/V1__criar_tabela_users.sql` com a tabela `public.users` (data-model.md): `id bigint generated always as identity` (`constraint pk_users primary key`), `username text not null`, `email text not null`, `password_hash text not null`, `bio text` (anulável), `image text` (anulável), `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`. Restrições nomeadas: `uq_users_username unique (username)`, `uq_users_email unique (email)`, `ck_users_username_not_blank check (btrim(username) <> '')`, `ck_users_email_not_blank check (btrim(email) <> '')`, `ck_users_email_lowercase check (email = lower(email))`. Sem `IF NOT EXISTS`.
- [X] T006 Criar `src/main/resources/db/migration/V2__criar_tabela_follows.sql` com a tabela `public.follows`: `follower_id bigint not null`, `followed_id bigint not null`, `created_at timestamptz not null default now()`; `constraint pk_follows primary key (follower_id, followed_id)`, `constraint fk_follows_follower foreign key (follower_id) references public.users (id) on delete cascade`, `constraint fk_follows_followed foreign key (followed_id) references public.users (id) on delete cascade`, `constraint ck_follows_not_self check (follower_id <> followed_id)`; e `create index ix_follows_followed_id on public.follows (followed_id)`.

### Formato de resposta e erros (`shared`)

- [X] T007 [P] Criar `src/main/java/com/example/blog/shared/web/MediaTypes.java`: classe `final` com construtor privado e a constante `public static final String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8"` (research R7).
- [X] T008 [P] Criar `src/main/java/com/example/blog/shared/error/ErrorResponse.java`: record `ErrorResponse(Map<String, List<String>> errors)` com construtor compacto que faz cópia imutável preservando a ordem de inserção, e fábrica `static ErrorResponse of(String chave, String mensagem)`.
- [X] T009 [P] Criar `src/main/java/com/example/blog/shared/error/ApiException.java`: `abstract class ApiException extends RuntimeException` com campos `final HttpStatus status` e `final Map<String, List<String>> errors` (cópia imutável), construtor `(HttpStatus status, Map<String, List<String>> errors, String mensagemDeLog)` e getters `status()` e `errors()`. Javadoc explicando que exceções de domínio estendem esta classe para o `shared` não depender das funcionalidades.
- [X] T010 Criar `src/main/java/com/example/blog/shared/error/GlobalExceptionHandler.java` (`@RestControllerAdvice`), devolvendo `ResponseEntity<ErrorResponse>` com `contentType(MediaType.parseMediaType(MediaTypes.APPLICATION_JSON_UTF8))`, conforme a tabela de research R6:
  - `ApiException` → `ex.status()` e `ex.errors()`;
  - `MethodArgumentNotValidException` → `422`, uma chave por campo com o **último segmento** do caminho (`user.email` → `email`), juntando as mensagens do mesmo campo; violação no próprio componente `user` (por exemplo, `@NotNull` em `user`) → chave `body` com `can't be empty`;
  - `HttpMessageNotReadableException` → `422` `{"body":["can't be empty"]}`;
  - `HttpMediaTypeNotSupportedException` → `422` `{"body":["is invalid"]}`;
  - `NoResourceFoundException` → `404` `{"resource":["not found"]}`;
  - `HttpRequestMethodNotSupportedException` → `405` `{"method":["not allowed"]}`;
  - `Exception` → `500` `{"server":["internal error"]}`, registrando com `log.error("Erro inesperado em {} {}", metodo, caminho, ex)`, sem expor detalhes ao cliente.
  Depende de T007–T009.
- [X] T011 [P] Criar `src/main/java/com/example/blog/shared/time/ClockConfig.java`: `@Configuration(proxyBeanMethods = false)` com `@Bean Clock clock()` retornando `Clock.systemUTC()`.

### Segurança e token (`shared/security`)

- [X] T012 [P] Criar `src/main/java/com/example/blog/shared/security/JwtProperties.java`: record `@ConfigurationProperties("app.jwt") @Validated JwtProperties(@NotBlank String secret, @NotBlank String issuer, @NotNull Duration ttl)` com método `SecretKey chave()` que decodifica o Base64 e lança `IllegalStateException("app.jwt.secret deve ter ao menos 32 bytes")` se tiver menos de 32 bytes (sem incluir o valor na mensagem). Habilitar com `@ConfigurationPropertiesScan` em `src/main/java/com/example/blog/BlogApplication.java`.
- [X] T013 [P] Criar `src/main/java/com/example/blog/shared/security/TokenHeaderResolver.java`: implementa `org.springframework.security.oauth2.server.resource.web.BearerTokenResolver`. Sem cabeçalho `Authorization` → `null`. Cabeçalho começando com `Token ` (prefixo exato, com espaço) e com token não vazio → devolve o token. Qualquer outro valor (`Bearer ...`, `Token` sem valor, vazio) → lança `OAuth2AuthenticationException(new BearerTokenError("invalid_token", HttpStatus.UNAUTHORIZED, "Invalid token header", null))` (research R2).
- [X] T014 [P] Criar `src/main/java/com/example/blog/shared/security/ErrorAuthenticationEntryPoint.java`: implementa `AuthenticationEntryPoint`; escreve `401`, `Content-Type: application/json;charset=UTF-8` e corpo `{"errors":{"token":["is invalid"]}}` quando a exceção (ou sua causa) for `OAuth2AuthenticationException`/`InvalidBearerTokenException`, e `{"errors":{"token":["is missing"]}}` nos demais casos. Serializar `ErrorResponse` com o `JsonMapper` do Jackson 3 injetado pelo construtor.
- [X] T015 [P] Criar `src/main/java/com/example/blog/shared/security/ErrorAccessDeniedHandler.java`: implementa `AccessDeniedHandler`; escreve `403` com `{"errors":{"authorization":["forbidden"]}}` e o mesmo `Content-Type`.
- [X] T016 Criar `src/main/java/com/example/blog/shared/security/TokenService.java` (`@Service`): construtor recebe `JwtEncoder`, `JwtProperties` e `Clock`; método `String emitir(long userId)` monta `JwtClaimsSet` com `iss = issuer`, `sub = String.valueOf(userId)`, `iat = clock.instant()`, `exp = iat + ttl`, cabeçalho `JwsHeader.with(MacAlgorithm.HS256)`, e devolve `tokenValue` (research R1). Depende de T012.
- [X] T017 Criar `src/main/java/com/example/blog/shared/security/SecurityConfig.java` (`@Configuration`, `@EnableWebSecurity`), com os beans:
  - `PasswordEncoder` = `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (research R5);
  - `JwtEncoder` = `new NimbusJwtEncoder(new ImmutableSecret<>(props.chave()))`;
  - `JwtDecoder` = `NimbusJwtDecoder.withSecretKey(props.chave()).macAlgorithm(MacAlgorithm.HS256).build()`, com `setJwtValidator(JwtValidators.createDefaultWithIssuer(props.issuer()))` (assinatura, `exp` e `iss`);
  - `SecurityFilterChain`: `csrf` desabilitado, `sessionManagement` `STATELESS`, `httpBasic` e `formLogin` desabilitados; `permitAll` em `POST /api/users`, `POST /api/users/login`, `GET /api/profiles/*`, `GET /actuator/health`, `GET /actuator/info`; `anyRequest().authenticated()`; `oauth2ResourceServer` com `bearerTokenResolver(new TokenHeaderResolver())`, `jwt` usando o `JwtDecoder`, `authenticationEntryPoint` e `accessDeniedHandler` de T014/T015; `exceptionHandling` com os mesmos handlers.
  Depende de T012–T015.
- [X] T018 [P] Criar `src/main/java/com/example/blog/shared/security/UsuarioDoTokenInexistenteException.java`, estendendo `ApiException` com `401` e `{"token":["is invalid"]}` (research R4).
- [X] T019 Criar `src/main/java/com/example/blog/shared/security/UsuarioAtual.java` (`@Component`) com `long idObrigatorio()`, que lê o `Jwt` do `SecurityContextHolder` e devolve `Long.parseLong(jwt.getSubject())` (lança `UsuarioDoTokenInexistenteException` se não houver `Jwt` ou o `sub` não for numérico), e `Optional<Long> idOpcional()`, que devolve vazio para requisição anônima. Depende de T018.

### Testes da fundação

- [X] T020 [P] Criar `src/test/java/com/example/blog/shared/security/TokenServiceTest.java` (unitário, sem Spring): com `Clock.fixed(...)`, `JwtProperties` de teste e um `NimbusJwtEncoder`/`NimbusJwtDecoder` reais, verificar que o token decodificado tem `sub` = id, `iss` = issuer, `exp` = `iat + 24h`; e que um token expirado (relógio do decoder avançado) é rejeitado com `JwtValidationException`.
- [X] T021 [P] Criar `src/test/java/com/example/blog/shared/security/TokenHeaderResolverTest.java` (unitário, `MockHttpServletRequest`): sem cabeçalho → `null`; `Token abc.def.ghi` → `abc.def.ghi`; `Bearer abc` → `OAuth2AuthenticationException`; `Token ` vazio → `OAuth2AuthenticationException`.
- [X] T022 [P] Criar `src/test/java/com/example/blog/MigrationsIT.java`: `@SpringBootTest` + `@Import(TestcontainersConfiguration.class)`; o contexto sobe (Flyway aplica `V1` e `V2` em banco vazio e `ddl-auto: validate` passa); verificar por `JdbcTemplate` que `flyway_schema_history` tem as versões `1` e `2` com `success = true`.

**Checkpoint**: `./mvnw -q verify` passa com T020–T022; a aplicação sobe sem endpoints de negócio.

---

## Phase 3: User Story 1 - Cadastro e login (Priority: P1) 🎯 MVP

**Goal**: pessoa visitante se cadastra (`POST /api/users`) e entra (`POST /api/users/login`), recebendo o objeto `user` com `token`.

**Independent Test**: cadastrar um usuário, fazer login e verificar `201`/`200` com `{"user":{email, token, username, bio:null, image:null}}`; verificar `409`, `422` e `401` nos casos de erro.

### Tests for User Story 1 ⚠️

> Escrever primeiro e confirmar que falham antes da implementação.

- [X] T023 [P] [US1] Criar `src/test/java/com/example/blog/user/service/UserServiceTest.java` (Mockito, sem Spring) cobrindo `cadastrar` e `autenticar`: cadastro normaliza e-mail (`" Jake@Jake.JAKE "` → `jake@jake.jake`) e username (`trim`), grava `passwordHash` vindo do `PasswordEncoder` e devolve token de `TokenService`; username existente → `CampoJaUsadoException` com `status 409` e `{"username":["has already been taken"]}`; e-mail existente → idem com `email`; ambos existentes → as duas chaves; login com e-mail em maiúsculas encontra o usuário; senha errada → `CredenciaisInvalidasException`; e-mail inexistente → `CredenciaisInvalidasException`.
- [X] T024 [P] [US1] Criar `src/test/java/com/example/blog/user/web/UserControllerTest.java` com `@WebMvcTest(UserController.class)` (conferir o pacote no Boot 4.1.2, previsto `org.springframework.boot.webmvc.test.autoconfigure`), `@Import({SecurityConfig.class, GlobalExceptionHandler.class, ClockConfig.class, ErrorAuthenticationEntryPoint.class, ErrorAccessDeniedHandler.class})`, `@EnableConfigurationProperties(JwtProperties.class)` e `@MockitoBean` em `UserService`, `TokenService` e `UsuarioAtual`. Casos: `POST /api/users` válido → `201` com corpo do contrato e `Content-Type` `application/json;charset=UTF-8`; sem `email` → `422` `{"errors":{"email":["can't be blank"]}}`; `email` `"jake"` → `422` `{"errors":{"email":["is invalid"]}}`; corpo `{}` → `422` com chave `body`; JSON malformado → `422` `{"errors":{"body":["can't be empty"]}}`; `POST /api/users/login` válido → `200`; sem `password` → `422`; service lançando `CredenciaisInvalidasException` → `401` `{"errors":{"credentials":["invalid"]}}`; resposta nunca contém `password` nem `passwordHash`.
- [X] T025 [P] [US1] Criar `src/test/java/com/example/blog/user/UserApiIT.java`: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Import(TestcontainersConfiguration.class)`; `@BeforeEach` apaga `follows` e `users` (via `JdbcTemplate`, `DELETE` com tabela inteira, banco descartável do teste). Cobrir os cenários de aceitação 1–5 de US1 em spec.md, mais: cadastro com username **e** e-mail repetidos → `409` com as duas chaves; e-mail em maiúsculas no login → `200`; o token recebido no cadastro é aceito em uma rota protegida (verificado em US2; aqui, apenas que o `token` não é vazio e tem três partes separadas por `.`).

### Implementation for User Story 1

- [X] T026 [P] [US1] Criar `src/main/java/com/example/blog/user/persistence/User.java`: `@Entity @Table(name = "users")`, campos privados `id` (`@Id @GeneratedValue(strategy = IDENTITY)`, `Long`), `username`, `email`, `passwordHash` (`@Column(name = "password_hash")`), `bio`, `image` (anuláveis), `createdAt`, `updatedAt` (`Instant`, `@Column(name = "created_at")`/`updated_at`). Construtor `protected` sem argumentos para o JPA; construtor `User(String username, String email, String passwordHash, Instant agora)` que preenche `createdAt`/`updatedAt`; getters somente leitura; métodos de comportamento `alterarEmail(String)`, `alterarUsername(String)`, `alterarSenha(String novoHash)`, `alterarBio(String)`, `alterarImagem(String)`, `marcarAtualizado(Instant)`; `equals`/`hashCode` por `id`, com entidade não persistida (`id == null`) diferente de qualquer outra e `hashCode` constante da classe. Respeitar as colunas de data-model.md: só `bio` e `image` anuláveis.
- [X] T027 [P] [US1] Criar `src/main/java/com/example/blog/user/persistence/UserRepository.java`: `interface UserRepository extends JpaRepository<User, Long>` com `Optional<User> findByEmail(String email)`, `Optional<User> findByUsername(String username)`, `boolean existsByEmail(String email)`, `boolean existsByUsername(String username)`, `boolean existsByEmailAndIdNot(String email, Long id)`, `boolean existsByUsernameAndIdNot(String username, Long id)`.
- [X] T028 [P] [US1] Criar em `src/main/java/com/example/blog/user/service/` as exceções `CampoJaUsadoException.java` (estende `ApiException`; fábricas `static CampoJaUsadoException noCadastro(Set<String> campos)` → `409` e `static CampoJaUsadoException naAtualizacao(Set<String> campos)` → `422`, cada campo com `["has already been taken"]`, ordem `username` antes de `email`) e `CredenciaisInvalidasException.java` (`401`, `{"credentials":["invalid"]}`).
- [X] T029 [P] [US1] Criar em `src/main/java/com/example/blog/user/service/` os records `NovoUsuario.java` (`username`, `email`, `password`), `Credenciais.java` (`email`, `password`) e `UsuarioAutenticado.java` (`email`, `token`, `username`, `bio`, `image`; `bio` e `image` podem ser `null`).
- [X] T030 [US1] Criar `src/main/java/com/example/blog/user/service/UserService.java` (`@Service`), construtor com `UserRepository`, `PasswordEncoder`, `TokenService` e `Clock`:
  - `@Transactional UsuarioAutenticado cadastrar(NovoUsuario)`: normaliza `email` (`strip().toLowerCase(Locale.ROOT)`) e `username` (`strip()`); acumula conflitos (`existsByUsername`, `existsByEmail`) e lança `CampoJaUsadoException.noCadastro(...)` se houver; grava com `passwordEncoder.encode`; usa `saveAndFlush` e converte `DataIntegrityViolationException` das restrições `uq_users_username`/`uq_users_email` em `CampoJaUsadoException.noCadastro` com o campo correspondente (research R10); devolve `UsuarioAutenticado` com `tokenService.emitir(id)`; registra `log.info("Usuário {} cadastrado", id)`.
  - `@Transactional(readOnly = true) UsuarioAutenticado autenticar(Credenciais)`: busca por e-mail normalizado; ausente ou `!passwordEncoder.matches(...)` → `CredenciaisInvalidasException` (mesma exceção nos dois casos, FR-008); devolve novo token.
  Depende de T026–T029.
- [X] T031 [P] [US1] Criar em `src/main/java/com/example/blog/user/web/` os records de entrada: `NewUserRequest(@NotNull @Valid NewUserData user)`; `NewUserData(@NotBlank(message = "can't be blank") String username, @NotBlank(message = "can't be blank") @Email(message = "is invalid") String email, @NotBlank(message = "can't be blank") String password)`; `LoginRequest(@NotNull @Valid LoginData user)`; `LoginData(@NotBlank(message = "can't be blank") @Email(message = "is invalid") String email, @NotBlank(message = "can't be blank") String password)`; e os de saída `UserView(String email, String token, String username, String bio, String image)` com `static UserView de(UsuarioAutenticado)` e `UserResponse(UserView user)`. Garantir que `bio` e `image` sejam serializados como `null` (não omitidos).
- [X] T032 [US1] Criar `src/main/java/com/example/blog/user/web/UserController.java` (`@RestController`, `@RequestMapping("/api")`): `@PostMapping(path = "/users", consumes = APPLICATION_JSON, produces = MediaTypes.APPLICATION_JSON_UTF8)` → `ResponseEntity.status(201)` com `UserResponse` (FR-001); `@PostMapping(path = "/users/login", ...)` → `200` com `UserResponse` (FR-006). Controller só converte DTO ↔ record do service; sem regra de negócio, sem `@Transactional`. Depende de T030, T031.

**Checkpoint**: T023–T025 passam; US1 funciona sozinha (cadastro e login).

---

## Phase 4: User Story 2 - Consultar e atualizar o usuário autenticado (Priority: P2)

**Goal**: `GET /api/user` e `PUT /api/user` com `Authorization: Token <jwt>`.

**Independent Test**: com token obtido no cadastro, `GET /api/user` → `200`; `PUT /api/user` altera só os campos enviados; sem token → `401 is missing`; token `Bearer` ou inválido → `401 is invalid`.

### Tests for User Story 2 ⚠️

- [X] T033 [P] [US2] Acrescentar a `src/test/java/com/example/blog/user/service/UserServiceTest.java` os casos de `buscarAtual` e `atualizar`: id inexistente → `UsuarioDoTokenInexistenteException`; atualização só de `bio` mantém os demais campos; `bio = ""` → `null` gravado; `image = "  "` → `null`; todos os campos `null` → `AtualizacaoVaziaException`; nova senha → `alterarSenha` com o hash do encoder; e-mail de outro usuário → `CampoJaUsadoException` com `status 422`; trocar para o próprio username atual não é conflito (usa `existsByUsernameAndIdNot`); `updatedAt` = instante do `Clock` fixo.
- [X] T034 [P] [US2] Acrescentar a `src/test/java/com/example/blog/user/web/UserControllerTest.java`: `GET /api/user` sem cabeçalho → `401` `{"errors":{"token":["is missing"]}}`; com `Authorization: Bearer x` → `401` `{"errors":{"token":["is invalid"]}}`; com usuário autenticado (`SecurityMockMvcRequestPostProcessors.jwt()` com `subject("1")`) → `200`; `PUT /api/user` com `{"user":{"bio":null}}` → service recebe `bio = ""`; `{"user":{}}` com service lançando `AtualizacaoVaziaException` → `422`; `{"user":{"email":"invalido"}}` → `422` `{"errors":{"email":["is invalid"]}}`; `{"user":{"username":"  "}}` → `422` `{"errors":{"username":["can't be blank"]}}`.
- [X] T035 [P] [US2] Acrescentar a `src/test/java/com/example/blog/user/UserApiIT.java` os cenários de aceitação 1–5 de US2 em spec.md, mais: token assinado com outra chave → `401 is invalid`; token válido de usuário apagado do banco → `401 is invalid`; `PUT` com `{"user":{"bio":null}}` → `bio: null`; `PUT` com `{"user":{}}` → `422`; troca de `username` para um já usado por outro → `422` `{"errors":{"username":["has already been taken"]}}`; o token antigo continua válido após trocar o `username` (o `sub` é o id).

### Implementation for User Story 2

- [X] T036 [P] [US2] Criar `src/main/java/com/example/blog/user/service/AtualizacaoVaziaException.java` (`ApiException`, `422`, `{"body":["can't be empty"]}`) e `src/main/java/com/example/blog/user/service/AtualizacaoUsuario.java` (record `email`, `username`, `password`, `bio`, `image`, todos anuláveis; Javadoc: `null` = não alterar; `""` em `bio`/`image` = limpar).
- [X] T037 [US2] Em `src/main/java/com/example/blog/user/service/UserService.java`, adicionar:
  - `@Transactional(readOnly = true) UsuarioAutenticado buscarAtual(long id)`: `findById` ou `UsuarioDoTokenInexistenteException`; devolve novo token.
  - `@Transactional UsuarioAutenticado atualizar(long id, AtualizacaoUsuario)`: se todos os campos forem `null` → `AtualizacaoVaziaException` (FR-017); carrega o usuário (ou `UsuarioDoTokenInexistenteException`); normaliza `email`/`username` como no cadastro; acumula conflitos com `existsByEmailAndIdNot`/`existsByUsernameAndIdNot` e lança `CampoJaUsadoException.naAtualizacao(...)` (FR-019); aplica só os campos não nulos; `bio`/`image` em branco (`isBlank()`) → `null` (FR-016); `password` → `alterarSenha(passwordEncoder.encode(...))` (FR-015); `marcarAtualizado(clock.instant())`; `saveAndFlush` convertendo `DataIntegrityViolationException` de unicidade em `CampoJaUsadoException.naAtualizacao`; devolve novo token.
  Depende de T036.
- [X] T038 [P] [US2] Criar em `src/main/java/com/example/blog/user/web/` os records `UpdateUserRequest(@NotNull @Valid UpdateUserData user)` e `UpdateUserData(@Pattern(regexp = ".*\\S.*", message = "can't be blank") @Email(message = "is invalid") String email, @Pattern(regexp = ".*\\S.*", message = "can't be blank") String username, @Pattern(regexp = ".*\\S.*", message = "can't be blank") String password, @JsonSetter(nulls = Nulls.AS_EMPTY) String bio, @JsonSetter(nulls = Nulls.AS_EMPTY) String image)` — `@JsonSetter`/`Nulls` de `com.fasterxml.jackson.annotation` (research R8). Método `AtualizacaoUsuario paraAtualizacao()`. **Desvio na implementação**: `@JsonSetter(nulls = AS_EMPTY)` também converte campo *ausente* em `""` em parâmetros de construtor de record (o Jackson usa o mesmo provedor de nulos para ausência), o que limparia `image` ao enviar só `bio`; substituído por `@JsonDeserialize(using = NuloComoVazioDeserializer.class)` (`null` explícito → `""`, ausente → `null`).
- [X] T039 [US2] Em `src/main/java/com/example/blog/user/web/UserController.java`, adicionar `@GetMapping(path = "/user", produces = MediaTypes.APPLICATION_JSON_UTF8)` → `200` com `userService.buscarAtual(usuarioAtual.idObrigatorio())` (FR-013) e `@PutMapping(path = "/user", consumes = APPLICATION_JSON, produces = MediaTypes.APPLICATION_JSON_UTF8)` com `@Valid @RequestBody UpdateUserRequest` → `200` (FR-014). Injetar `UsuarioAtual` pelo construtor. Depende de T037, T038.

**Checkpoint**: US1 e US2 passam juntas e cada uma isoladamente.

---

## Phase 5: User Story 3 - Consultar perfil público (Priority: P3)

**Goal**: `GET /api/profiles/{username}` com autenticação opcional e `following` calculado.

**Independent Test**: cadastrar um usuário; consultar o perfil sem token → `following: false`; inexistente → `404`; token inválido → `401`; com token de quem segue (relação inserida diretamente no teste) → `following: true`.

### Tests for User Story 3 ⚠️

- [X] T040 [P] [US3] Criar `src/test/java/com/example/blog/profile/service/ProfileServiceTest.java` (Mockito) para `buscar`: username inexistente → `PerfilNaoEncontradoException`; sem viewer → `following = false` sem consultar `FollowRepository`; viewer que segue → `true`; viewer que não segue → `false`; `bio`/`image` nulos repassados como `null`.
- [X] T041 [P] [US3] Criar `src/test/java/com/example/blog/profile/web/ProfileControllerTest.java` (`@WebMvcTest(ProfileController.class)`, mesmos `@Import`/`@EnableConfigurationProperties` de T024, `@MockitoBean` em `ProfileService` e `UsuarioAtual`): `GET /api/profiles/jake` anônimo → `200` `{"profile":{"username":"jake","bio":null,"image":null,"following":false}}` com `Content-Type` `application/json;charset=UTF-8`; service lançando `PerfilNaoEncontradoException` → `404` `{"errors":{"profile":["not found"]}}`; `Authorization: Token invalido` → `401` `{"errors":{"token":["is invalid"]}}`.
- [X] T042 [P] [US3] Criar `src/test/java/com/example/blog/profile/ProfileApiIT.java` (mesma base de T025: `@SpringBootTest`, `@AutoConfigureMockMvc`, Testcontainers, limpeza em `@BeforeEach`; usuários criados por `POST /api/users`) com os cenários de aceitação 1–4 de US3 em spec.md. Para o cenário 3, inserir a relação com `JdbcTemplate` (`INSERT INTO follows (follower_id, followed_id) VALUES (?, ?)`), para não depender de US4. Verificar também que o username diferencia maiúsculas (`GET /api/profiles/JAKE` → `404` quando só existe `jake`).

### Implementation for User Story 3

- [X] T043 [P] [US3] Criar `src/main/java/com/example/blog/profile/persistence/FollowId.java` (`@Embeddable`, record ou classe com `followerId` → `@Column(name = "follower_id")`, `followedId` → `@Column(name = "followed_id")`, ambos `Long` não nulos; `equals`/`hashCode` pelos dois campos) e `src/main/java/com/example/blog/profile/persistence/Follow.java` (`@Entity @Table(name = "follows")`, `@EmbeddedId FollowId id`, `Instant createdAt` em `created_at`; construtor protegido sem argumentos; sem `@ManyToOne`, conforme data-model.md).
- [X] T044 [P] [US3] Criar `src/main/java/com/example/blog/profile/persistence/FollowRepository.java`: `interface FollowRepository extends JpaRepository<Follow, FollowId>` (usa `existsById` herdado para calcular `following`).
- [X] T045 [P] [US3] Criar em `src/main/java/com/example/blog/profile/service/` o record `Perfil.java` (`username`, `bio`, `image`, `boolean following`) e `PerfilNaoEncontradoException.java` (`ApiException`, `404`, `{"profile":["not found"]}`; mensagem de log com o username).
- [X] T046 [US3] Criar `src/main/java/com/example/blog/profile/service/ProfileService.java` (`@Service`), construtor com `UserRepository` e `FollowRepository`; `@Transactional(readOnly = true) Perfil buscar(String username, Optional<Long> viewerId)`: `findByUsername` ou `PerfilNaoEncontradoException` (FR-023); `following` = `viewerId.map(v -> followRepository.existsById(new FollowId(v, user.getId()))).orElse(false)` (FR-021, FR-022). Depende de T043–T045.
- [X] T047 [P] [US3] Criar em `src/main/java/com/example/blog/profile/web/` os records `ProfileView(String username, String bio, String image, boolean following)` com `static ProfileView de(Perfil)` e `ProfileResponse(ProfileView profile)`; `bio`/`image` serializados como `null`, não omitidos.
- [X] T048 [US3] Criar `src/main/java/com/example/blog/profile/web/ProfileController.java` (`@RestController`, `@RequestMapping("/api/profiles")`): `@GetMapping(path = "/{username}", produces = MediaTypes.APPLICATION_JSON_UTF8)` → `200` com `profileService.buscar(username, usuarioAtual.idOpcional())` (FR-020). Depende de T046, T047.

**Checkpoint**: US3 funciona sozinha sobre usuários cadastrados por US1.

---

## Phase 6: User Story 4 - Seguir e deixar de seguir (Priority: P4)

**Goal**: `POST` e `DELETE /api/profiles/{username}/follow`, idempotentes.

**Independent Test**: com dois usuários A e B, A segue B (2×) → `200 following: true`; A deixa de seguir B (2×) → `200 following: false`; sem token → `401`; inexistente → `404`; A segue A → `422`.

### Tests for User Story 4 ⚠️

- [X] T049 [P] [US4] Acrescentar a `src/test/java/com/example/blog/profile/service/ProfileServiceTest.java`: `seguir` chama `followRepository.seguir(viewer, alvo)` e devolve `following = true`; `seguir` a si mesmo → `SeguirASiMesmoException` sem tocar o repository; alvo inexistente → `PerfilNaoEncontradoException`; `deixarDeSeguir` chama `followRepository.deixarDeSeguir(viewer, alvo)` e devolve `following = false`.
- [X] T050 [P] [US4] Acrescentar a `src/test/java/com/example/blog/profile/web/ProfileControllerTest.java`: `POST /api/profiles/jake/follow` sem cabeçalho → `401 is missing`; com `jwt()` → `200` com `following: true`; `DELETE` com `jwt()` → `200` com `following: false`; service lançando `SeguirASiMesmoException` → `422` `{"errors":{"profile":["cannot follow yourself"]}}`.
- [X] T051 [P] [US4] Acrescentar a `src/test/java/com/example/blog/profile/ProfileApiIT.java` os cenários de aceitação 1–6 de US4 em spec.md, mais: seguir a si mesmo → `422`; após seguir 2×, `SELECT count(*) FROM follows WHERE follower_id = ? AND followed_id = ?` = `1` (SC-005); `GET /api/profiles/B` com token de A reflete `following` após cada operação.

### Implementation for User Story 4

- [X] T052 [US4] Em `src/main/java/com/example/blog/profile/persistence/FollowRepository.java`, adicionar `@Modifying @Query(value = "INSERT INTO follows (follower_id, followed_id) VALUES (:followerId, :followedId) ON CONFLICT DO NOTHING", nativeQuery = true) void seguir(@Param("followerId") long followerId, @Param("followedId") long followedId)` e `@Modifying @Query(value = "DELETE FROM follows WHERE follower_id = :followerId AND followed_id = :followedId", nativeQuery = true) void deixarDeSeguir(...)` (research R11).
- [X] T053 [P] [US4] Criar `src/main/java/com/example/blog/profile/service/SeguirASiMesmoException.java` (`ApiException`, `422`, `{"profile":["cannot follow yourself"]}`).
- [X] T054 [US4] Em `src/main/java/com/example/blog/profile/service/ProfileService.java`, adicionar `@Transactional Perfil seguir(long viewerId, String username)`: busca o alvo (ou `PerfilNaoEncontradoException`); se `alvo.getId() == viewerId` → `SeguirASiMesmoException` (FR-027); `followRepository.seguir(...)`; devolve `Perfil` com `following = true` (FR-024, FR-025). E `@Transactional Perfil deixarDeSeguir(long viewerId, String username)`: busca o alvo (ou 404); `followRepository.deixarDeSeguir(...)`; devolve `following = false` (FR-026). Depende de T052, T053.
- [X] T055 [US4] Em `src/main/java/com/example/blog/profile/web/ProfileController.java`, adicionar `@PostMapping(path = "/{username}/follow", produces = MediaTypes.APPLICATION_JSON_UTF8)` e `@DeleteMapping(path = "/{username}/follow", produces = MediaTypes.APPLICATION_JSON_UTF8)`, ambos → `200` com `ProfileResponse`, usando `usuarioAtual.idObrigatorio()`. Depende de T054.

**Checkpoint**: as quatro histórias passam juntas.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: contrato transversal, documentação e validação final.

- [X] T056 [P] Criar `src/test/java/com/example/blog/shared/error/GlobalExceptionHandlerTest.java` (`@WebMvcTest` com um controller de teste interno, ou integração via `UserApiIT`): rota inexistente autenticada → `404` `{"errors":{"resource":["not found"]}}`; método não suportado → `405`; exceção inesperada → `500` `{"errors":{"server":["internal error"]}}`, sem stack trace no corpo (FR-031); todas com `Content-Type` `application/json;charset=UTF-8` (FR-028).
- [X] T057 [P] Criar `README.md` na raiz: o que é a API (Recorte A da Conduit), pré-requisitos (JDK 25, Docker), criação do `.env` a partir de `.env.example` (incluindo `JWT_SECRET`, sem valores reais), comandos `docker compose up -d db`, `./mvnw -q verify` e `./mvnw -q spring-boot:run -Dspring-boot.run.profiles=dev`, lista dos 7 endpoints e link para `specs/001-conduit-auth-profiles/contracts/openapi.yaml`.
- [X] T058 Revisar o código de `src/main/java/com/example/blog/` contra o checklist de `.claude/rules/tech.md` §15 e §27: sem `System.out`, sem imports não usados nem curinga, sem `@Autowired` em campo, sem `@Transactional` em controller, nenhum log com senha, hash, token ou e-mail, `shared` sem importar `user` ou `profile`, nenhuma entidade em resposta HTTP.
- [X] T059 Rodar `./mvnw -q verify` e corrigir falhas até ficar verde (mostrar ao usuário só erros e as últimas 50 linhas de log).
- [X] T060 Executar `specs/001-conduit-auth-profiles/quickstart.md` (seções 2 e 3): conferir `.env`, `docker compose up -d db`, aguardar `healthy` em `docker compose ps`, subir a aplicação no perfil `dev` e percorrer os 18 cenários com `curl`, usando dados fictícios. Se o Docker não estiver disponível, parar e informar o usuário. Não recriar o banco (`docker compose down -v`) sem confirmação explícita.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências.
- **Foundational (Phase 2)**: depende do Setup; bloqueia todas as histórias.
- **US1 (Phase 3)**: depende da Foundational. Cria `User` e `UserRepository`, usados por US2–US4.
- **US2 (Phase 4)**: depende de US1 (usa `UserService`, `User`, `UserController` e precisa de cadastro para obter token).
- **US3 (Phase 5)**: depende de US1 (`User`, `UserRepository`, cadastro nos testes). Independente de US2 e US4.
- **US4 (Phase 6)**: depende de US3 (`ProfileService`, `ProfileController`, `FollowRepository`).
- **Polish (Phase 7)**: depende das histórias desejadas.

### User Story Dependencies

```text
Setup → Foundational → US1 ─┬─> US2
                            └─> US3 ──> US4
```

US2 e US3 podem andar em paralelo depois de US1.

### Within Each User Story

- Testes primeiro, falhando antes da implementação.
- Entidades/repositórios → exceções e records do service → service → DTOs web → controller.
- Arquivos compartilhados entre fases (`UserService.java`, `UserController.java`, `UserServiceTest.java`, `UserControllerTest.java`, `UserApiIT.java`, `ProfileService.java`, `ProfileController.java`, `FollowRepository.java`, `ProfileServiceTest.java`, `ProfileControllerTest.java`, `ProfileApiIT.java`) são editados em sequência, nunca em paralelo.

### Parallel Opportunities

- Setup: T002, T003, T004.
- Foundational: T007, T008, T009, T011, T012, T013, T014, T015, T018 em paralelo; depois T010, T016, T017, T019; testes T020, T021, T022 em paralelo.
- US1: testes T023, T024, T025; implementação T026, T027, T028, T029, T031.
- US2: testes T033, T034, T035; implementação T036, T038.
- US3: testes T040, T041, T042; implementação T043, T044, T045, T047.
- US4: testes T049, T050, T051; T053.
- US2 inteira em paralelo com US3 inteira (arquivos distintos).
- Polish: T056, T057.

---

## Parallel Example: User Story 1

```bash
# Testes de US1 juntos:
Task: "UserServiceTest (cadastrar/autenticar) em src/test/java/com/example/blog/user/service/UserServiceTest.java"
Task: "UserControllerTest (POST /api/users, /api/users/login) em src/test/java/com/example/blog/user/web/UserControllerTest.java"
Task: "UserApiIT (cenários US1) em src/test/java/com/example/blog/user/UserApiIT.java"

# Base de US1 junta:
Task: "Entidade User em src/main/java/com/example/blog/user/persistence/User.java"
Task: "UserRepository em src/main/java/com/example/blog/user/persistence/UserRepository.java"
Task: "Exceções em src/main/java/com/example/blog/user/service/"
Task: "Records do service em src/main/java/com/example/blog/user/service/"
Task: "DTOs em src/main/java/com/example/blog/user/web/"
```

## Parallel Example: User Story 3

```bash
Task: "FollowId e Follow em src/main/java/com/example/blog/profile/persistence/"
Task: "FollowRepository em src/main/java/com/example/blog/profile/persistence/FollowRepository.java"
Task: "Perfil e PerfilNaoEncontradoException em src/main/java/com/example/blog/profile/service/"
Task: "ProfileView e ProfileResponse em src/main/java/com/example/blog/profile/web/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup.
2. Phase 2: Foundational (bloqueia tudo).
3. Phase 3: US1.
4. **Parar e validar**: `./mvnw -q verify` e cenários 1–5 do quickstart.

### Incremental Delivery

1. Setup + Foundational → base pronta (aplicação sobe, migrations aplicadas).
2. US1 → cadastro e login (MVP).
3. US2 → conta do usuário autenticado.
4. US3 → perfil público.
5. US4 → seguir e deixar de seguir.
6. Polish → README, revisão e validação ponta a ponta.

---

## Notes

- [P] = arquivos diferentes, sem dependência pendente.
- [USn] liga a tarefa à história para rastreabilidade.
- Confirmar que os testes falham antes de implementar.
- Não fazer commit sem pedido do usuário.
- Evitar: tarefas vagas, edição concorrente do mesmo arquivo, dependências que quebrem a independência das histórias.
