---

description: "Lista de tarefas do Recorte A da API Conduit (usuários, autenticação e perfis)"
---

# Tasks: Recorte A da API Conduit — usuários, autenticação e perfis

**Input**: documentos de design em `specs/001-conduit-usuarios-perfis/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ (openapi.yaml, README.md), quickstart.md

**Tests**: INCLUÍDOS. A spec exige (SC-002) que todo cenário de aceitação e caso de borda seja coberto por teste automatizado que passa em `./mvnw verify`. Em cada fase, escreva os testes primeiro e confirme que falham antes de implementar.

**Organization**: tarefas agrupadas por história de usuário (US1 = P1, US2 = P2, US3 = P3), cada uma testável de forma independente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência de tarefa incompleta)
- **[Story]**: história de usuário da tarefa (US1, US2, US3)

## Path Conventions

Módulo Maven único, pacote base `com.example.blog`. Abreviações usadas nas descrições:

- `MAIN` = `src/main/java/com/example/blog`
- `TEST` = `src/test/java/com/example/blog`
- `RES` = `src/main/resources`

Regras transversais para TODAS as tarefas (de `CLAUDE.md` e `.claude/rules/`):

- Spring Boot 4.1.2 / Jackson 3 (`tools.jackson.*`) / `jakarta.*`; nunca APIs do Boot 3 (`@MockBean`, `javax.*`, `com.fasterxml.jackson.databind`). Em dúvida, conferir a documentação do Boot 4.1.
- Injeção por construtor com campos `final`; DTOs como records; entidades JPA nunca expostas na API.
- `@Transactional` só em serviços (`readOnly = true` em leituras); nunca em controllers.
- Dependência `web` → `service` → `persistence`; `shared` não importa nada de `usuario` nem `perfil`; `perfil` pode depender de `usuario.service`, nunca o contrário.
- Indentação de 4 espaços, sem tabulação, imports explícitos, uma classe pública por arquivo (records aninhados permitidos).
- Mensagens de erro do contrato (fixas, em inglês): `can't be blank`, `is invalid`, `has already been taken`, `invalid`, `is missing`, `not found`, `cannot follow yourself`, `at least one field is required`, `unexpected error`.
- Maven sempre com `-q` (por exemplo, `./mvnw -q verify`); mostrar só erros e as últimas 50 linhas.
- Testes usam dados fictícios e geram `username`/`email` únicos por teste (por exemplo, sufixo UUID), sem depender de ordem.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: dependências, configuração e variáveis de ambiente

- [X] T001 Editar `pom.xml`: adicionar `spring-boot-starter-security` e `spring-boot-starter-security-oauth2-resource-server` (compile) e `spring-boot-starter-security-test` (scope `test`), todos SEM `<version>`; adicionar `maven-failsafe-plugin` (versão gerenciada pelo parent) em `<build><plugins>` com os goals `integration-test` e `verify`, para rodar `*IT`. Confirmar os nomes exatos dos starters na documentação do Spring Boot 4.1 antes de salvar; rodar `./mvnw -q -DskipTests compile` para validar
- [X] T002 [P] Acrescentar `JWT_SECRET=<segredo_jwt_ficticio_com_pelo_menos_32_bytes>` ao `.env.example` (valor claramente fictício, sem segredo real)
- [X] T003 [P] Criar `.env` na raiz a partir de `.env.example`, se ainda não existir (regra `tech.md` §2.2): `POSTGRES_DB=sdd_1`, `POSTGRES_USER=agente`, `POSTGRES_PASSWORD` com 32 caracteres aleatórios gerados com o módulo `secrets` do Python e gravados direto no arquivo (nunca exibidos), `DB_PORT=5432`, `DB_URL=jdbc:postgresql://localhost:5432/sdd_1`, `JWT_SECRET` aleatório de pelo menos 32 bytes (por exemplo, `secrets.token_urlsafe(48)`). Confirmar que `.env` consta no `.gitignore` (já consta)
- [X] T004 [P] Editar `RES/application.yml`: adicionar `spring.servlet.encoding.charset: UTF-8` e `spring.servlet.encoding.force-response: true` (R12; no Boot 4 o prefixo é `spring.servlet.encoding`, não `server.servlet.encoding`) e o bloco `conduit.jwt` com `secret: ${JWT_SECRET}`, `issuer: conduit-blog` e `validade: PT24H` (R3); manter o restante do arquivo intacto
- [X] T005 [P] Criar `RES/application-dev.yml` com `spring.config.import: optional:file:.env[.properties]`, para o perfil `dev` ler as variáveis do `.env`
- [X] T006 [P] Criar `src/test/resources/application-test.yml` com `conduit.jwt.secret` fictício de pelo menos 32 bytes (valor de teste, não secreto), `conduit.jwt.issuer: conduit-blog` e `conduit.jwt.validade: PT24H`; os testes de fatia e de integração usam `@ActiveProfiles("test")`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: tratamento de erros, JWT, segurança e relógio, usados por todas as histórias

**⚠️ CRITICAL**: nenhuma história começa antes desta fase terminar

### Testes da fundação (escrever primeiro)

- [X] T007 [P] Criar teste unitário `TEST/shared/security/TokenServiceTest.java`: com `Clock` fixo e `NimbusJwtEncoder`/`NimbusJwtDecoder` reais (HS256, segredo de teste), verificar que `emitir(42L)` gera JWT com `sub` = `"42"`, `iss` = emissor configurado, `iat` = instante do relógio e `exp` = `iat` + validade; e que o decoder rejeita token expirado, com assinatura alterada e com outro emissor
- [X] T008 [P] Criar teste unitário `TEST/shared/web/TratadorGlobalDeErrosTest.java`: chamar cada handler diretamente e verificar status e corpo `{"errors": {...}}` conforme a tabela de T013 (422 de validação com chave do último segmento do campo, `user` nulo vira chave `body`, 422 de `HttpMessageNotReadableException` com chave `body`, `ValidacaoException` 422, `ConflitoException` 409, `CredenciaisInvalidasException` 401 `credentials: ["invalid"]`, `RecursoNaoEncontradoException` 404, `TokenInvalidoException` 401 `token: ["is invalid"]`, `Exception` 500 `server: ["unexpected error"]` sem detalhes internos)

### Implementação da fundação

- [X] T009 [P] Criar `MAIN/shared/config/ClockConfig.java`: `@Configuration` com bean `Clock` = `Clock.systemUTC()`
- [X] T010 [P] Criar `MAIN/shared/web/ErroResponse.java`: record `ErroResponse(Map<String, List<String>> errors)` com cópia imutável no construtor compacto e fábrica estática `de(String chave, String mensagem)`
- [X] T011 [P] Criar as exceções de domínio não verificadas em `MAIN/shared/web/` (um arquivo cada): `ValidacaoException` (guarda `Map<String, List<String>> erros`; construtor de conveniência `(String campo, String mensagem)`) → 422; `ConflitoException(String campo, String mensagem)` → 409; `CredenciaisInvalidasException` → 401; `RecursoNaoEncontradoException(String recurso)` → 404 com mensagem `not found`
- [X] T012 [P] Criar `MAIN/shared/security/TokenInvalidoException.java`: exceção não verificada lançada quando o `sub` do token aponta para usuário inexistente (FR-013)
- [X] T013 Criar `MAIN/shared/web/TratadorGlobalDeErros.java` (`@RestControllerAdvice`), único ponto de conversão de erros, sempre com corpo `ErroResponse` e `Content-Type` JSON: `MethodArgumentNotValidException` → 422, uma entrada por campo com chave = último segmento do caminho (`user.email` → `email`) e, quando o próprio `user` for nulo, chave `body`; `HttpMessageNotReadableException` → 422 `{"body": ["is invalid"]}` (sem mensagem interna); `ValidacaoException` → 422 com o mapa da exceção; `ConflitoException` → 409; `CredenciaisInvalidasException` → 401 `{"credentials": ["invalid"]}`; `RecursoNaoEncontradoException` → 404 `{"<recurso>": ["not found"]}`; `TokenInvalidoException` → 401 `{"token": ["is invalid"]}`; exceções do Spring que já carregam status (`ErrorResponse`, por exemplo `NoResourceFoundException`, `HttpRequestMethodNotSupportedException`) → mesmo status com chave `request`; `Exception` → 500 `{"server": ["unexpected error"]}` com `log.error` (SLF4J, exceção como último argumento). Nunca expor stack trace nem nomes de tabela (depende de T010–T012)
- [X] T014 [P] Criar `MAIN/shared/security/JwtProperties.java`: record `@ConfigurationProperties("conduit.jwt")` `@Validated` com `@NotBlank String secret` (validar no construtor compacto que tem pelo menos 32 bytes em UTF-8), `@NotBlank String issuer` e `@NotNull Duration validade`
- [X] T015 Criar `MAIN/shared/security/TokenService.java` (`@Service`): `String emitir(Long usuarioId)` usando `JwtEncoder` com cabeçalho `MacAlgorithm.HS256` e claims `sub` = id, `iss` = `issuer`, `iat` = `Instant.now(clock)`, `exp` = `iat + validade` (R3) (depende de T014)
- [X] T016 [P] Criar `MAIN/shared/security/TokenAuthorizationResolver.java` implementando `BearerTokenResolver`: aceita SOMENTE `Authorization: Token <jwt>`; cabeçalho ausente, esquema diferente (por exemplo `Bearer`) ou valor vazio → retorna `null` (tratado como sem token) (R2)
- [X] T017 [P] Criar `MAIN/shared/security/ErroAutenticacaoEntryPoint.java` (`AuthenticationEntryPoint`) e `MAIN/shared/security/ErroAcessoNegadoHandler.java` (`AccessDeniedHandler`): escrevem JSON com `Content-Type: application/json;charset=UTF-8` usando o `JsonMapper` do Jackson 3 injetado. Entry point: se a exceção for `OAuth2AuthenticationException`/`InvalidBearerTokenException` → 401 `{"errors":{"token":["is invalid"]}}`; caso contrário → 401 `{"errors":{"token":["is missing"]}}`. Access denied → 403 `{"errors":{"authorization":["forbidden"]}}`. Sem cabeçalho `WWW-Authenticate` obrigatório (R5)
- [X] T018 Criar `MAIN/shared/security/SecurityConfig.java` (`@Configuration`, `@EnableConfigurationProperties(JwtProperties.class)`): bean `SecurityFilterChain` com sessão `STATELESS`, CSRF desabilitado, sem CORS; rotas públicas `POST /api/users`, `POST /api/users/login`, `GET /api/profiles/*`, `GET /actuator/health`, `GET /actuator/info`; demais autenticadas; `oauth2ResourceServer(jwt)` com `bearerTokenResolver` = `TokenAuthorizationResolver` e `authenticationEntryPoint` = `ErroAutenticacaoEntryPoint`; `exceptionHandling` com o mesmo entry point e `ErroAcessoNegadoHandler`. Beans: `SecretKey` HMAC-SHA256 do `secret`; `JwtEncoder` (`NimbusJwtEncoder` com `ImmutableSecret`); `JwtDecoder` (`NimbusJwtDecoder.withSecretKey(...).macAlgorithm(HS256)`) com validadores `JwtTimestampValidator` usando o `Clock` e `JwtIssuerValidator(issuer)`; `PasswordEncoder` = `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (R1, R4, R11) (depende de T009, T014, T016, T017)
- [X] T019 Rodar `./mvnw -q verify` e garantir que T007 e T008 passam e que o contexto existente continua subindo

**Checkpoint**: fundação pronta — histórias podem começar

---

## Phase 3: User Story 1 - Cadastro e login com JWT (Priority: P1) 🎯 MVP

**Goal**: visitante cria conta (`POST /api/users`, 201) e entra (`POST /api/users/login`, 200), recebendo `{"user": {email, token, username, bio, image}}`.

**Independent Test**: `POST /api/users` seguido de `POST /api/users/login` com as mesmas credenciais; ambos devolvem `{"user": {...}}` com `token` preenchido; senha errada → 401; username repetido → 409; sem `email` → 422.

### Testes da US1 (escrever primeiro e ver falhar)

- [X] T020 [P] [US1] Criar `TEST/MigrationsIT.java`: `@SpringBootTest` + `@Import(TestcontainersConfiguration.class)` + `@ActiveProfiles("test")`; contexto sobe em banco vazio `postgres:18.6` (Flyway aplica todas as migrations, Hibernate `ddl-auto: validate` passa) e `flyway_schema_history` contém `V1` com `success = true`
- [X] T021 [P] [US1] Criar `TEST/usuario/service/UsuarioServiceTest.java` (Mockito, sem Spring) para `registrar` e `entrar`: cadastro normaliza e-mail (`"  Jake@X.com "` → `"jake@x.com"`) e username só com `trim`; grava *hash* via `PasswordEncoder`, nunca a senha; e-mail já usado → `ConflitoException("email", "has already been taken")`; username já usado → `ConflitoException("username", ...)`; `DataIntegrityViolationException` com restrição `uq_usuario_email`/`uq_usuario_username` → mesma `ConflitoException`; login correto devolve `UsuarioAutenticado` com token de `TokenService`; e-mail inexistente ou senha errada → `CredenciaisInvalidasException` (mesmo erro nos dois casos)
- [X] T022 [P] [US1] Criar `TEST/usuario/web/UsuarioControllerTest.java` (`@WebMvcTest(UsuarioController.class)`, `@Import(SecurityConfig.class, TratadorGlobalDeErros.class, ...)`, `@MockitoBean UsuarioService`, `@ActiveProfiles("test")`): `POST /api/users` válido → 201 com corpo User e `bio`/`image` presentes como `null`; sem `email` → 422 `{"errors":{"email":["can't be blank"]}}`; `email` `"x"` → 422 `{"errors":{"email":["is invalid"]}}`; sem os três campos → 422 com as três chaves; JSON malformado → 422 chave `body`; corpo `{}` (sem `user`) → 422 chave `body`; `ConflitoException` → 409; `POST /api/users/login` válido → 200; sem `password` → 422; `CredenciaisInvalidasException` → 401 `{"errors":{"credentials":["invalid"]}}`
- [X] T023 [P] [US1] Criar `TEST/usuario/UsuarioApiIT.java` (`@SpringBootTest(webEnvironment = RANDOM_PORT)` + Testcontainers + `@ActiveProfiles("test")`, cliente HTTP `RestClient` na `@LocalServerPort` sem lançar exceção em 4xx) com os cenários 1–5 da US1: cadastro 201 com `token`, `bio` e `image` nulos; login 200; login com senha errada 401; username repetido 409 `{"errors":{"username":["has already been taken"]}}`; sem `email` 422; e-mail com caixa diferente (`Jake@x.com` vs `jake@x.com`) → 409 `email`; login com e-mail em maiúsculas → 200; `Content-Type` com tipo `application/json` e `charset=utf-8` (comparação sem caixa nem espaço); a coluna `senha_hash` não contém a senha em texto puro (consulta via `JdbcTemplate` só dessa coluna, começa com `{bcrypt}`)

### Implementação da US1

- [X] T024 [P] [US1] Criar `RES/db/migration/V1__criar_tabela_usuario.sql` exatamente conforme data-model.md: tabela `public.usuario` com `id bigint generated always as identity` (`constraint pk_usuario primary key`), `email varchar(255) not null` (`constraint uq_usuario_email unique`), `username varchar(255) not null` (`constraint uq_usuario_username unique`), `senha_hash varchar(255) not null`, `bio text` NULL, `image text` NULL, `criado_em timestamptz not null default now()`, `atualizado_em timestamptz not null default now()`; `constraint ck_usuario_email_nao_vazio check (btrim(email) <> '')` e `constraint ck_usuario_username_nao_vazio check (btrim(username) <> '')`. Sem `IF NOT EXISTS`; identificadores em `snake_case` sem aspas
- [X] T025 [P] [US1] Criar a entidade `MAIN/usuario/persistence/Usuario.java` (`@Entity @Table(name = "usuario", schema = "public")`, classe, não record): `id` `Long` com `@GeneratedValue(strategy = IDENTITY)`; `email`, `username`, `senhaHash` (coluna `senha_hash`) `nullable = false, length = 255`; `bio` e `image` `@Column(columnDefinition = "text")` anuláveis; `criadoEm` e `atualizadoEm` `Instant` (`timestamptz`, `nullable = false`); construtor `protected` sem argumentos; construtor de criação `(email, username, senhaHash, Instant agora)`; métodos de negócio `alterarEmail`, `alterarUsername`, `alterarSenhaHash`, `alterarBio`, `alterarImage` (cada um recebe também o `Instant` para `atualizadoEm`) em vez de setters; getters apenas do necessário; `equals`/`hashCode` pelo `id`, tratando entidade não persistida (id nulo → não igual; `hashCode` constante da classe)
- [X] T026 [US1] Criar `MAIN/usuario/persistence/UsuarioRepository.java` (`JpaRepository<Usuario, Long>`): `Optional<Usuario> findByEmail(String)`, `Optional<Usuario> findByUsername(String)`, `boolean existsByEmail(String)`, `boolean existsByUsername(String)`, `boolean existsByEmailAndIdNot(String, Long)`, `boolean existsByUsernameAndIdNot(String, Long)` (depende de T025)
- [X] T027 [P] [US1] Criar `MAIN/usuario/service/UsuarioAutenticado.java`: record `(String email, String token, String username, String bio, String image)` — resultado do serviço com o token
- [X] T028 [US1] Criar `MAIN/usuario/service/UsuarioService.java` (`@Service`, injeta `UsuarioRepository`, `PasswordEncoder`, `TokenService`, `Clock`) com: `@Transactional UsuarioAutenticado registrar(String username, String email, String senha)` — e-mail com `trim` + `toLowerCase(Locale.ROOT)`, username com `trim`; checa `existsByEmail`/`existsByUsername` → `ConflitoException(campo, "has already been taken")`; grava com `saveAndFlush` e converte `DataIntegrityViolationException` em `ConflitoException` identificando o campo pelo nome da restrição (`uq_usuario_email`/`uq_usuario_username`) (R9); emite token com `TokenService.emitir(id)`; `@Transactional(readOnly = true) UsuarioAutenticado entrar(String email, String senha)` — normaliza e-mail, busca, confere com `passwordEncoder.matches`; falha → `CredenciaisInvalidasException` sem indicar a causa (FR-009). Nunca registrar senha ou token em log (depende de T026, T027, T015)
- [X] T029 [P] [US1] Criar DTOs em `MAIN/usuario/web/`: `RegistroRequest(@Valid @NotNull UsuarioNovo user)` com record aninhado `UsuarioNovo(@NotBlank(message = "can't be blank") String username, @NotBlank(message = "can't be blank") @Email(message = "is invalid") String email, @NotBlank(message = "can't be blank") String password)`; `LoginRequest(@Valid @NotNull Credenciais user)` com `Credenciais(@NotBlank(message = "can't be blank") String email, @NotBlank(message = "can't be blank") String password)`; `UsuarioResponse(UsuarioDados user)` com `UsuarioDados(String email, String token, String username, String bio, String image)` e fábrica `de(UsuarioAutenticado)`. `bio` e `image` serializados como `null`, nunca omitidos (R13)
- [X] T030 [US1] Criar `MAIN/usuario/web/UsuarioController.java` (`@RestController @RequestMapping("/api/users")`): `POST` → `@Valid @RequestBody RegistroRequest`, chama `registrar`, responde `201` com `UsuarioResponse` (com cabeçalho `Location: /api/user`); `POST /login` → `@Valid @RequestBody LoginRequest`, chama `entrar`, responde `200`. Sem regra de negócio no controller (depende de T028, T029)
- [X] T031 [US1] Rodar `./mvnw -q verify`; T020–T023 devem passar

**Checkpoint**: US1 funcional e testável sozinha (MVP)

---

## Phase 4: User Story 2 - Consultar e atualizar o usuário autenticado (Priority: P2)

**Goal**: `GET /api/user` e `PUT /api/user` com `Authorization: Token <jwt>`; o `PUT` altera só os campos presentes; `bio`/`image` `null` limpam.

**Independent Test**: com token da US1, `GET /api/user` → 200; `PUT /api/user` com `{"user":{"bio":"..."}}` → 200 e novo `GET` reflete a mudança; sem `Authorization` → 401.

### Testes da US2 (escrever primeiro e ver falhar)

- [X] T032 [P] [US2] Criar `TEST/usuario/web/AtualizacaoUsuarioMapperTest.java` (unitário, `Validator` de `Validation.buildDefaultValidatorFactory()`, `JsonMapper` do Jackson 3 para montar `ObjectNode`): `{}` → `ValidacaoException` `user: ["at least one field is required"]`; só campos desconhecidos → mesmo erro; `{"bio": null}` → `bio` informado com valor `null`; `bio` ausente → não informado; `{"email": ""}`, `{"username": null}`, `{"password": "  "}` → `can't be blank` na chave do campo; `{"email": "x"}` → `email: ["is invalid"]`; vários campos inválidos → todas as chaves no mesmo erro; valor não textual (`{"bio": 1}`) → `is invalid`
- [X] T033 [P] [US2] Acrescentar a `TEST/usuario/service/UsuarioServiceTest.java` os casos de `buscarAtual` e `atualizar`: id inexistente → `TokenInvalidoException`; atualizar só `bio` preserva os demais campos; `bio` informado `null` limpa; novo `password` regrava o *hash*; e-mail normalizado; e-mail ou username de outro usuário → `ValidacaoException(campo, "has already been taken")` (422, não 409); usar o próprio e-mail/username atual não é conflito; `DataIntegrityViolationException` na gravação → mesma `ValidacaoException`; `atualizadoEm` vem do `Clock`
- [X] T034 [P] [US2] Criar `TEST/usuario/web/UsuarioAtualControllerTest.java` (`@WebMvcTest(UsuarioAtualController.class)`, segurança importada, `@MockitoBean UsuarioService`, token gerado com `TokenService` do perfil de teste ou `SecurityMockMvcRequestPostProcessors.jwt()`): `GET /api/user` sem `Authorization` → 401 `{"errors":{"token":["is missing"]}}`; com `Authorization: Bearer <jwt>` → 401 `is missing`; com `Token abc` → 401 `{"errors":{"token":["is invalid"]}}`; válido → 200 com `token` igual ao recebido; `PUT` com `{"user":{}}` → 422 `user`; corpo sem `user` → 422 `body`; `TokenInvalidoException` do serviço → 401 `is invalid`
- [X] T035 [P] [US2] Criar `TEST/usuario/UsuarioAtualApiIT.java` (mesma base de T023) com os cenários 1–4 da US2 e casos de borda: `GET` com token válido → 200 do dono; `PUT` com `bio` → 200, `bio` nova e demais preservados, novo `GET` confirma; troca de senha → login com nova senha 200 e com antiga 401; `GET` sem `Authorization` → 401; token expirado (emitido com `Clock` no passado ou validade curta) → 401 `is invalid`; token de usuário inexistente (`sub` = id sem linha) → 401 `is invalid`; `PUT` com `bio: null` → `bio` `null`; `PUT` `{"user":{}}` → 422; `PUT` com username de outro usuário → 422 `has already been taken`; token continua válido após trocar o username

### Implementação da US2

- [X] T036 [P] [US2] Criar `MAIN/usuario/service/AtualizacaoUsuario.java`: record de comando com componentes `Campo email, Campo username, Campo password, Campo bio, Campo image`, onde `Campo` é record aninhado `(boolean isInformado, String valor)` com fábricas `ausente()` e `informado(String valor)` (sem `Optional`, R8); método `boolean hasAlgumCampo()`
- [X] T037 [US2] Acrescentar a `MAIN/usuario/service/UsuarioService.java`: `@Transactional(readOnly = true) UsuarioAutenticado buscarAtual(Long usuarioId, String token)` — usuário inexistente → `TokenInvalidoException` (FR-013); devolve o mesmo token recebido; `@Transactional UsuarioAutenticado atualizar(Long usuarioId, String token, AtualizacaoUsuario comando)` — aplica só campos informados: e-mail normalizado (`trim` + minúsculas `Locale.ROOT`) e checado com `existsByEmailAndIdNot`; username com `trim` e `existsByUsernameAndIdNot`; duplicidade → `ValidacaoException(campo, "has already been taken")`; senha → `passwordEncoder.encode`; `bio`/`image` gravam o valor (inclusive `null`); `atualizadoEm` = `Instant.now(clock)`; `saveAndFlush` convertendo `DataIntegrityViolationException` pela restrição em `ValidacaoException` (R9) (depende de T036)
- [X] T038 [P] [US2] Criar `MAIN/usuario/web/AtualizacaoUsuarioRequest.java`: record `(tools.jackson.databind.node.ObjectNode user)` (R8)
- [X] T039 [US2] Criar `MAIN/usuario/web/AtualizacaoUsuarioMapper.java` (`@Component`, injeta `jakarta.validation.Validator`): `AtualizacaoUsuario converter(ObjectNode user)` — `user` nulo → `ValidacaoException("body", "is invalid")`; aceita só `email`, `username`, `password`, `bio`, `image`, ignorando os demais; nenhum aceito presente → `ValidacaoException("user", "at least one field is required")` (FR-016); `email`/`username`/`password` presentes nulos, não textuais ou em branco → `can't be blank` (FR-017); `email` com formato inválido → `is invalid`, conferido com `validator.validateValue(RegistroRequest.UsuarioNovo.class, "email", valor)` para reaproveitar a mesma regra `@Email`; `bio`/`image` devem ser texto ou `null`, senão `is invalid`; acumula todos os erros num único `ValidacaoException` (depende de T036, T038)
- [X] T040 [US2] Criar `MAIN/usuario/web/UsuarioAtualController.java` (`@RestController @RequestMapping("/api/user")`): `GET` com `@AuthenticationPrincipal Jwt jwt` → `buscarAtual(Long.valueOf(jwt.getSubject()), jwt.getTokenValue())`, 200 `UsuarioResponse`; `PUT` com `@RequestBody AtualizacaoUsuarioRequest` → mapper → `atualizar(...)`, 200 `UsuarioResponse` (depende de T037, T039)
- [X] T041 [US2] Rodar `./mvnw -q verify`; T032–T035 e os testes da US1 devem passar

**Checkpoint**: US1 e US2 funcionam de forma independente

---

## Phase 5: User Story 3 - Perfis públicos e seguir/deixar de seguir (Priority: P3)

**Goal**: `GET /api/profiles/{username}` (autenticação opcional) e `POST`/`DELETE /api/profiles/{username}/follow` (autenticados), com `{"profile": {username, bio, image, following}}`.

**Independent Test**: com usuários A e B, A segue B → `GET /api/profiles/B` com token de A mostra `following: true`; após `DELETE`, `following: false`; sem token, `following: false`.

### Testes da US3 (escrever primeiro e ver falhar)

- [X] T042 [P] [US3] Atualizar `TEST/MigrationsIT.java`: `flyway_schema_history` contém `V1` e `V2` com `success = true`; inserir seguimento de um usuário para ele mesmo viola `ck_seguidor_distintos` e par repetido viola `pk_seguidor` (dados fictícios criados no próprio teste)
- [X] T043 [P] [US3] Criar `TEST/perfil/service/PerfilServiceTest.java` (Mockito): `consultar(username)` → `following` falso; `consultar(username, seguidorId)` → `following` conforme `existsById`; username inexistente → `RecursoNaoEncontradoException("profile")`; `seguir` grava só se não existir (idempotente) e devolve `following: true`; `seguir` a si mesmo → `ValidacaoException("profile", "cannot follow yourself")`; `deixarDeSeguir` remove se existir, idempotente, devolve `following: false`; alvo inexistente em seguir/deixar → 404
- [X] T044 [P] [US3] Criar `TEST/perfil/web/PerfilControllerTest.java` (`@WebMvcTest(PerfilController.class)`, segurança importada, `@MockitoBean PerfilService`): `GET /api/profiles/b` sem token → 200 e chama `consultar(username)`; com token válido → chama `consultar(username, id)`; com `Token abc` → 401 `is invalid`; `POST .../follow` sem token → 401 `is missing`; `RecursoNaoEncontradoException` → 404 `{"errors":{"profile":["not found"]}}`; `ValidacaoException` → 422; corpo de sucesso `{"profile":{"username","bio","image","following"}}` com `bio`/`image` `null` presentes
- [X] T045 [P] [US3] Criar `TEST/perfil/PerfilApiIT.java` (mesma base de T023) com os cenários 1–5 da US3 e casos de borda: `GET` sem token → 200 `following: false`; A segue B → 200 `following: true`; repetir → 200 `following: true`; `GET` com token de A → `following: true`; `DELETE` → 200 `following: false`; repetir `DELETE` → 200; seguir a si mesmo → 422 `{"errors":{"profile":["cannot follow yourself"]}}`; perfil inexistente em `GET`, `POST` e `DELETE` → 404 `profile: ["not found"]`; `POST` sem token → 401; `GET` com token inválido → 401

### Implementação da US3

- [X] T046 [P] [US3] Criar `RES/db/migration/V2__criar_tabela_seguidor.sql` conforme data-model.md: tabela `public.seguidor` com `seguidor_id bigint not null` (`constraint fk_seguidor_usuario_seguidor foreign key references public.usuario (id) on delete cascade`), `seguido_id bigint not null` (`constraint fk_seguidor_usuario_seguido foreign key references public.usuario (id) on delete cascade`), `criado_em timestamptz not null default now()`, `constraint pk_seguidor primary key (seguidor_id, seguido_id)`, `constraint ck_seguidor_distintos check (seguidor_id <> seguido_id)`; e `create index ix_seguidor_seguido_id on public.seguidor (seguido_id)`
- [X] T047 [P] [US3] Criar `MAIN/perfil/persistence/SeguimentoId.java`: classe `@Embeddable` (`Serializable`) com `seguidorId` (coluna `seguidor_id`) e `seguidoId` (coluna `seguido_id`), `Long`, `nullable = false`; construtor `protected` sem argumentos e construtor público com os dois ids; `equals`/`hashCode` pelos dois ids
- [X] T048 [US3] Criar `MAIN/perfil/persistence/Seguimento.java` (`@Entity @Table(name = "seguidor", schema = "public")`): `@EmbeddedId SeguimentoId id`; `criadoEm` `Instant` (`criado_em`, `nullable = false`); construtor `protected` sem argumentos e construtor `(SeguimentoId id, Instant agora)`; SEM `@ManyToOne` para `Usuario`; `equals`/`hashCode` pelo id (depende de T047)
- [X] T049 [US3] Criar `MAIN/perfil/persistence/SeguimentoRepository.java` (`JpaRepository<Seguimento, SeguimentoId>`), usando `existsById` e `deleteById` herdados (depende de T048)
- [X] T050 [P] [US3] Criar `MAIN/usuario/service/UsuarioPublico.java`: record `(Long id, String username, String bio, String image)`; acrescentar a `MAIN/usuario/service/UsuarioService.java` o método `@Transactional(readOnly = true) Optional<UsuarioPublico> buscarPublicoPorUsername(String username)` (sem expor a entidade `Usuario` para o pacote `perfil`)
- [X] T051 [P] [US3] Criar `MAIN/perfil/service/Perfil.java`: record de resultado `(String username, String bio, String image, boolean isFollowing)`
- [X] T052 [US3] Criar `MAIN/perfil/service/PerfilService.java` (`@Service`, injeta `UsuarioService`, `SeguimentoRepository`, `Clock`): `@Transactional(readOnly = true) Perfil consultar(String username)` (anônimo, `following` falso); `@Transactional(readOnly = true) Perfil consultar(String username, Long seguidorId)`; `@Transactional Perfil seguir(Long seguidorId, String username)` — alvo inexistente → `RecursoNaoEncontradoException("profile")`; alvo = próprio → `ValidacaoException("profile", "cannot follow yourself")`; grava `Seguimento` só se `!existsById` (idempotente); `@Transactional Perfil deixarDeSeguir(Long seguidorId, String username)` — remove só se existir; ambos respondem com o estado final (depende de T049, T050, T051)
- [X] T053 [P] [US3] Criar `MAIN/perfil/web/PerfilResponse.java`: record `(PerfilDados profile)` com record aninhado `PerfilDados(String username, String bio, String image, boolean following)` e fábrica `de(Perfil)`; `bio`/`image` serializados como `null`
- [X] T054 [US3] Criar `MAIN/perfil/web/PerfilController.java` (`@RestController @RequestMapping("/api/profiles/{username}")`): `GET` com `@AuthenticationPrincipal Jwt jwt` (nulo quando anônimo) → `consultar(username)` ou `consultar(username, id)`; `POST /follow` → `seguir`; `DELETE /follow` → `deixarDeSeguir`; todos 200 com `PerfilResponse` (depende de T052, T053)
- [X] T055 [US3] Rodar `./mvnw -q verify`; T042–T045 e os testes de US1/US2 devem passar

**Checkpoint**: todas as histórias funcionam de forma independente

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: documentação, conferências finais e validação ponta a ponta

- [X] T056 [P] Criar `README.md` na raiz: pré-requisitos (JDK 25, Docker), criação do `.env` a partir de `.env.example` (sem valores reais), `docker compose up -d db`, `./mvnw -q verify`, `./mvnw -q spring-boot:run -Dspring-boot.run.profiles=dev`, tabela de endpoints e link para `specs/001-conduit-usuarios-perfis/contracts/`
- [X] T057 [P] Revisar o código novo contra `tech.md`: sem `System.out`, sem imports curinga ou não usados, sem tabulação, sem `@Autowired` em campo, sem senha/token/e-mail em logs, `shared` sem import de `usuario`/`perfil` (conferir com busca por `import com.example.blog.usuario` e `import com.example.blog.perfil` em `MAIN/shared`), Javadoc nos serviços públicos
- [X] T058 [P] Conferir FR-031: nenhuma rota além das 7 operações do contrato (`grep` por `@*Mapping` em `MAIN`) e nenhum segredo em arquivo versionado (`.env.example` só com valores fictícios; `application*.yml` só com `${...}`)
- [X] T059 Validar manualmente pelo `quickstart.md`: `docker compose up -d db`, aguardar `healthy` em `docker compose ps`, subir com perfil `dev`, confirmar Flyway aplicando `V1` e `V2` e executar os cenários 1–21 com `curl` (dados fictícios); relatar ao usuário só as falhas
- [X] T060 Rodar `./mvnw -q verify` final; mostrar só erros e as últimas 50 linhas

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências
- **Foundational (Phase 2)**: depende do Setup (T001 obrigatório para compilar segurança) — BLOQUEIA todas as histórias
- **US1 (Phase 3)**: depende da Fase 2
- **US2 (Phase 4)**: depende da Fase 2 e da tabela/entidade/serviço de usuário da US1 (T024–T028)
- **US3 (Phase 5)**: depende da Fase 2 e de T024–T028 (usuários existentes); não depende da US2
- **Polish (Phase 6)**: depende das histórias desejadas

### User Story Dependencies

- **US1 (P1)**: independente após a fundação
- **US2 (P2)**: usa `Usuario`, `UsuarioRepository` e `UsuarioService` da US1; testável sozinha com usuários criados no próprio teste
- **US3 (P3)**: usa `UsuarioService` da US1 (`buscarPublicoPorUsername`); pode correr em paralelo com a US2 depois da US1, cuidando de edições concorrentes em `UsuarioService.java` (T037 e T050)

### Within Each User Story

- Testes primeiro, falhando antes da implementação
- Migration → entidade → repository → serviço → DTOs → controller
- Checkpoint `./mvnw -q verify` ao fim de cada fase

### Parallel Opportunities

- Setup: T002–T006 em paralelo após T001 (ou junto, pois são arquivos diferentes)
- Fundação: T007, T008 (testes) e T009–T012, T014, T016, T017 em paralelo; T013, T015 e T018 em sequência depois das dependências
- US1: T020–T023 em paralelo; T024, T025, T027, T029 em paralelo
- US2: T032–T035 em paralelo; T036 e T038 em paralelo
- US3: T042–T045 em paralelo; T046, T047, T050, T051, T053 em paralelo
- Polish: T056–T058 em paralelo

---

## Parallel Example: User Story 1

```text
# Testes da US1 juntos:
Task: "T020 MigrationsIT em src/test/java/com/example/blog/MigrationsIT.java"
Task: "T021 UsuarioServiceTest em src/test/java/com/example/blog/usuario/service/UsuarioServiceTest.java"
Task: "T022 UsuarioControllerTest em src/test/java/com/example/blog/usuario/web/UsuarioControllerTest.java"
Task: "T023 UsuarioApiIT em src/test/java/com/example/blog/usuario/UsuarioApiIT.java"

# Arquivos independentes da US1 juntos:
Task: "T024 V1__criar_tabela_usuario.sql em src/main/resources/db/migration/"
Task: "T025 entidade Usuario em src/main/java/com/example/blog/usuario/persistence/Usuario.java"
Task: "T027 record UsuarioAutenticado em src/main/java/com/example/blog/usuario/service/UsuarioAutenticado.java"
Task: "T029 DTOs RegistroRequest, LoginRequest, UsuarioResponse em src/main/java/com/example/blog/usuario/web/"
```

## Parallel Example: User Story 3

```text
Task: "T046 V2__criar_tabela_seguidor.sql em src/main/resources/db/migration/"
Task: "T047 SeguimentoId em src/main/java/com/example/blog/perfil/persistence/SeguimentoId.java"
Task: "T051 record Perfil em src/main/java/com/example/blog/perfil/service/Perfil.java"
Task: "T053 PerfilResponse em src/main/java/com/example/blog/perfil/web/PerfilResponse.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Fase 1: Setup
2. Fase 2: Fundação (bloqueia tudo)
3. Fase 3: US1
4. **PARAR E VALIDAR**: `./mvnw -q verify` e cenários 1–6 do `quickstart.md`

### Incremental Delivery

1. Setup + Fundação → base pronta
2. US1 → validar → MVP (cadastro e login)
3. US2 → validar (usuário atual)
4. US3 → validar (perfis e seguimento)
5. Polish → README, revisão e validação completa

---

## Notes

- [P] = arquivos diferentes, sem dependência pendente
- O contrato `contracts/openapi.yaml` é obrigatório e não deve ser editado
- Migrations aplicadas são imutáveis: corrigir com nova migration se já tiverem saído do banco local
- Nunca usar `docker compose down -v`, `flyway clean` ou `repair` sem confirmação explícita do usuário
