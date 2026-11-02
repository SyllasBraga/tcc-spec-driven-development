---

description: "Lista de tarefas — Conduit Recorte A (usuários, autenticação e perfis)"
---

# Tasks: Conduit — Recorte A (usuários, autenticação e perfis)

**Input**: Documentos de design em `specs/001-conduit-usuarios-perfis/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/openapi.yaml](contracts/openapi.yaml), [contracts/erros.md](contracts/erros.md), [quickstart.md](quickstart.md)

**Tests**: Incluídos. As regras do projeto (`.claude/rules/tech.md` §11, §15, §26 e Banco §8) exigem cobertura por testes, um teste de migrations em banco vazio e Testcontainers; o SC-001 da spec exige uma suíte que cubra todos os cenários de aceitação. Escreva os testes de cada história antes da implementação e confirme que falham.

**Organization**: Tarefas agrupadas por história de usuário (US1–US5 da spec), cada uma testável de forma independente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência de tarefa incompleta)
- **[Story]**: história de usuário da tarefa (US1…US5)

## Convenções válidas para todas as tarefas

- Pacote raiz `com.example.blog`; código em `src/main/java/com/example/blog/`, testes em `src/test/java/com/example/blog/` (espelhando o main). Unitários e de fatia terminam em `Test`; integração em `IT`.
- Spring Boot 4.1.2: `jakarta.*`, Jackson 3 (`tools.jackson.*`), `@MockitoBean` (nunca `@MockBean`). Confira na documentação da 4.1.2 os pacotes de `@WebMvcTest`, `MockMvcTester`/`RestTestClient` e `@AutoConfigureMockMvc`, que mudaram com a modularização.
- Injeção por construtor, campos `final`, sem `@Autowired` em campos; `@Transactional` só em serviços (`readOnly = true` em leituras); entidades JPA nunca saem do pacote `service`.
- Mensagens de erro: sempre as constantes de `MensagensErro` (T010), com os textos exatos de [contracts/erros.md](contracts/erros.md).
- Todos os ITs: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@Import(TestcontainersConfiguration.class)`, `@ActiveProfiles("test")`, limpeza do banco no `@BeforeEach` via `ApoioDeTeste` (T021). Em toda resposta verificada, conferir também o `Content-Type` (tipo `application/json`, parâmetro `charset` igual a `utf-8` sem diferenciar maiúsculas).
- Rode o Maven sempre com `-q` (`./mvnw -q verify`).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: dependências, configuração e variáveis de ambiente

- [X] T001 Em `pom.xml`, adicionar sem versão (gerenciadas pelo parent 4.1.2) as dependências `org.springframework.boot:spring-boot-starter-security`, `org.springframework.boot:spring-boot-starter-oauth2-resource-server` e, com `<scope>test</scope>`, `org.springframework.boot:spring-boot-starter-security-test`; em `<build><plugins>`, adicionar `org.apache.maven.plugins:maven-failsafe-plugin` (versão do parent) com os goals `integration-test` e `verify`
- [X] T002 [P] Em `.env.example`, acrescentar a linha `JWT_SECRET=<segredo_local_aleatorio_com_32_ou_mais_caracteres>` (valor claramente fictício)
- [X] T003 [P] Se `.env` ainda não existir, criá-lo a partir de `.env.example` conforme `tech.md` §2.2 (`POSTGRES_DB=sdd_5`, `POSTGRES_USER=agente`, `POSTGRES_PASSWORD` de 32 caracteres gerada com `secrets` do Python, `DB_PORT=5432`, `DB_URL=jdbc:postgresql://localhost:5432/sdd_5`); se já existir, apenas acrescentar `JWT_SECRET` com 48 caracteres gerados com `secrets` do Python, gravados direto no arquivo `.env`, sem exibir o valor
- [X] T004 [P] Em `src/main/resources/application.yml`, acrescentar `conduit.token.segredo: ${JWT_SECRET}`, `conduit.token.validade: 24h`, `conduit.token.emissor: conduit`, `server.port: 8080`, `server.servlet.encoding.charset: UTF-8` e `server.servlet.encoding.force: true`, mantendo as chaves existentes (`ddl-auto: validate`, `open-in-view: false`, `shutdown: graceful`)
- [X] T005 [P] Criar `src/test/resources/application-test.yml` com `conduit.token.segredo` igual a um valor fictício de teste de 48 caracteres (ex.: `segredo-ficticio-apenas-para-testes-0123456789ab`) — nunca um segredo real

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: tabela de usuários, erros, segurança/token e apoio de teste — usados por todas as histórias

**⚠️ CRITICAL**: nenhuma história começa antes desta fase

### Esquema e persistência de usuário

- [X] T006 Criar `src/main/resources/db/migration/V1__criar_tabela_usuario.sql`: tabela `public.usuario` com `id bigint generated always as identity` (`constraint pk_usuario primary key`), `username text not null`, `email text not null`, `senha_hash text not null`, `bio text null`, `imagem text null`, `criado_em timestamptz not null default now()`, `atualizado_em timestamptz not null default now()`; restrições `uq_usuario_username unique (username)` (sensível a maiúsculas), `ck_usuario_username_nao_vazio check (btrim(username) <> '')`, `ck_usuario_email_nao_vazio check (btrim(email) <> '')`; e `create unique index uq_usuario_email on public.usuario (lower(email))`. Sem `IF NOT EXISTS`
- [X] T007 [P] Criar a entidade `Usuario` em `src/main/java/com/example/blog/usuario/persistence/Usuario.java` mapeando a tabela `usuario` (campos `Long id` identity, `username`, `email`, `senhaHash`→`senha_hash`, `bio` anulável, `imagem` anulável, `Instant criadoEm`, `Instant atualizadoEm`); construtor sem argumentos `protected`; construtor `Usuario(String username, String email, String senhaHash, Instant agora)` com `bio`/`imagem` nulos; métodos de negócio `alterarEmail`, `alterarUsername`, `alterarSenhaHash`, `alterarBio(String nullable)`, `alterarImagem(String nullable)` e `registrarAlteracao(Instant)`; getters somente leitura; `equals`/`hashCode` pelo `id` (não persistidas não são iguais); sem setters genéricos
- [X] T008 Criar `UsuarioRepository extends JpaRepository<Usuario, Long>` em `src/main/java/com/example/blog/usuario/persistence/UsuarioRepository.java` com `Optional<Usuario> findByUsername(String)`, `Optional<Usuario> findByEmailIgnoreCase(String)`, `boolean existsByUsername(String)`, `boolean existsByEmailIgnoreCase(String)`, `boolean existsByUsernameAndIdNot(String, Long)`, `boolean existsByEmailIgnoreCaseAndIdNot(String, Long)` (depende de T007)

### Erros (formato `{"errors":{...}}`)

- [X] T009 [P] Criar o record `ErrosResponse(Map<String, List<String>> errors)` em `src/main/java/com/example/blog/shared/erro/ErrosResponse.java`, com fábrica `de(String chave, String mensagem)` e cópia imutável do mapa preservando a ordem de inserção
- [X] T010 [P] Criar `src/main/java/com/example/blog/shared/erro/MensagensErro.java` (classe final, construtor privado) com as constantes: `NAO_PODE_SER_VAZIO = "can't be blank"`, `CORPO_VAZIO = "can't be empty"`, `INVALIDO = "is invalid"`, `JA_EM_USO = "has already been taken"`, `AUSENTE = "is missing"`, `NAO_ENCONTRADO = "not found"`, `NAO_PODE_SEGUIR_A_SI_MESMO = "cannot follow yourself"`, `PROIBIDO = "forbidden"`, `ERRO_INTERNO = "internal error"` e as chaves `CHAVE_BODY = "body"`, `CHAVE_USER = "user"`, `CHAVE_TOKEN = "token"`, `CHAVE_PROFILE = "profile"`, `CHAVE_CREDENCIAIS = "email or password"`, `CHAVE_AUTORIZACAO = "authorization"`, `CHAVE_SERVIDOR = "server"`
- [X] T011 [P] Criar as exceções não verificadas em `src/main/java/com/example/blog/shared/erro/`: `ValidacaoException.java` e `ConflitoException.java` (ambas carregam um `Map<String, List<String>>` imutável de erros, com fábrica `de(chave, mensagem)`), `CredenciaisInvalidasException.java`, `RecursoNaoEncontradoException.java` (guarda a chave do recurso, ex.: `profile`) e `TokenInvalidoException.java` — uma classe pública por arquivo, mensagens sem dados sensíveis
- [X] T012 Criar `@RestControllerAdvice` `ManipuladorDeErros` em `src/main/java/com/example/blog/shared/erro/ManipuladorDeErros.java` (depende de T009–T011), sempre respondendo `ErrosResponse` com `application/json`: `MethodArgumentNotValidException` → 422 agrupando por campo (remover o prefixo `user.` do caminho; campo `user` nulo → `{"body":["can't be empty"]}`; sem mensagens repetidas; ordem estável); `HttpMessageNotReadableException` → 422 `{"body":["can't be empty"]}`; `ValidacaoException` → 422; `ConflitoException` → 409; `CredenciaisInvalidasException` → 401 `{"email or password":["is invalid"]}`; `RecursoNaoEncontradoException` → 404 `{"<chave>":["not found"]}`; `TokenInvalidoException` → 401 `{"token":["is invalid"]}`; `Exception` genérica → 500 `{"server":["internal error"]}` registrando o erro uma única vez com SLF4J (exceção como último argumento), sem expor detalhes

### Segurança e token

- [X] T013 [P] Criar o record `TokenProperties(@NotBlank @Size(min = 32) String segredo, @NotNull Duration validade, @NotBlank String emissor)` com `@ConfigurationProperties(prefix = "conduit.token")` e `@Validated` em `src/main/java/com/example/blog/shared/seguranca/TokenProperties.java`
- [X] T014 [P] Criar `TokenHeaderResolver implements BearerTokenResolver` em `src/main/java/com/example/blog/shared/seguranca/TokenHeaderResolver.java`: sem cabeçalho `Authorization` → `null`; valor no formato `Token <jwt>` (prefixo exato `Token ` seguido de token não vazio) → devolve o token; qualquer outro valor (ex.: `Bearer x`, `Token ` vazio) → lança `InvalidBearerTokenException`
- [X] T015 [P] Criar `ErroAutenticacaoHandler` implementando `AuthenticationEntryPoint` e `AccessDeniedHandler` em `src/main/java/com/example/blog/shared/seguranca/ErroAutenticacaoHandler.java`: `OAuth2AuthenticationException` (inclui `InvalidBearerTokenException`) → 401 `{"errors":{"token":["is invalid"]}}`; demais `AuthenticationException` (sem credenciais) → 401 `{"errors":{"token":["is missing"]}}`; acesso negado → 403 `{"errors":{"authorization":["forbidden"]}}`; escrever o corpo com o `JsonMapper` do Jackson 3 injetado, `Content-Type` `application/json` e charset UTF-8; usar as constantes de `MensagensErro`
- [X] T016 Criar `TokenService` em `src/main/java/com/example/blog/shared/seguranca/TokenService.java` (depende de T013) com `String emitir(Long usuarioId)`: usa `JwtEncoder`, `Clock` e `TokenProperties`; claims `sub` = id, `iss` = `emissor`, `iat` = agora, `exp` = agora + `validade`; cabeçalho `alg` HS256
- [X] T017 Criar `SegurancaConfig` (`@Configuration`, `@EnableConfigurationProperties(TokenProperties.class)`) em `src/main/java/com/example/blog/shared/seguranca/SegurancaConfig.java` (depende de T013–T015): beans `Clock` (`Clock.systemUTC()`), `PasswordEncoder` (`PasswordEncoderFactories.createDelegatingPasswordEncoder()`), `SecretKey` HS256 a partir de `segredo`, `JwtEncoder` (`NimbusJwtEncoder` com chave secreta), `JwtDecoder` (`NimbusJwtDecoder.withSecretKey(...).macAlgorithm(HS256)` com validadores de expiração/timestamp e de emissor via `JwtValidators.createDefaultWithIssuer(emissor)`), `TokenHeaderResolver`; `SecurityFilterChain` com CSRF desabilitado, sessão `STATELESS`, `permitAll` para `POST /api/users`, `POST /api/users/login`, `GET /api/profiles/*`, `GET /actuator/health`, `GET /actuator/info` e `/error`; `anyRequest().authenticated()`; `oauth2ResourceServer` com `jwt`, `bearerTokenResolver` e `authenticationEntryPoint` = `ErroAutenticacaoHandler`; `exceptionHandling` com o mesmo handler como entry point e access denied handler
- [X] T018 [P] Criar `UsuarioAutenticado` em `src/main/java/com/example/blog/shared/seguranca/UsuarioAutenticado.java` (classe final, construtor privado) com `static Long id(Jwt jwt)` → `Long.valueOf(jwt.getSubject())` (subject não numérico → `TokenInvalidoException`) e `static Long idOuNulo(Jwt jwt)` para rotas de autenticação opcional (`jwt` nulo → `null`)

### Testes e apoio fundamentais

- [X] T019 [P] Criar `MigracoesIT` em `src/test/java/com/example/blog/MigracoesIT.java`: sobe o contexto completo sobre PostgreSQL 18.6 vazio (Testcontainers), o que valida `ddl-auto: validate`, e consulta `flyway_schema_history` com `JdbcTemplate` afirmando que todas as migrations de `db/migration` constam com `success = true` (nesta fase, `V1`)
- [X] T020 [P] Criar testes unitários em `src/test/java/com/example/blog/shared/seguranca/TokenServiceTest.java` (com `Clock.fixed`, `NimbusJwtEncoder`/`NimbusJwtDecoder` reais e segredo fictício: `sub`, `iss`, `exp` = iat + 24h; token expirado e de outro emissor rejeitados pelo decoder) e `src/test/java/com/example/blog/shared/seguranca/TokenHeaderResolverTest.java` (ausente → null; `Token abc` → `abc`; `Bearer abc`, `Token `, `abc` → `InvalidBearerTokenException`)
- [X] T021 Criar o componente de apoio `ApoioDeTeste` em `src/test/java/com/example/blog/ApoioDeTeste.java` (registrado nos ITs via `@Import`): `limparBanco()` apagando, nesta ordem, `seguimento` (se existir) e `usuario` com `JdbcTemplate`/`JdbcTestUtils`; `Usuario criarUsuario(String username, String email, String senha)` gravando com `PasswordEncoder` via `UsuarioRepository`; `String tokenDe(Usuario)` via `TokenService`; `String tokenExpirado(Long id)` e `String tokenComOutroSegredo(Long id)` para casos de 401 (depende de T008, T016, T017)

**Checkpoint**: `./mvnw -q verify` passa com `MigracoesIT` e os testes de token; a base está pronta para as histórias

---

## Phase 3: User Story 1 - Cadastro de usuário (Priority: P1) 🎯 MVP

**Goal**: `POST /api/users` cria a conta e devolve `201` com `{"user":{email, token, username, bio: null, image: null}}` (FR-001–FR-004, FR-026–FR-032)

**Independent Test**: enviar `POST /api/users` com dados válidos → `201` com o objeto `user` completo e token aceito por `TokenService`/decoder; duplicados → `409`; campos vazios → `422`

### Tests for User Story 1 ⚠️

> Escreva primeiro e confirme que falham

- [X] T022 [P] [US1] Criar `CadastroIT` em `src/test/java/com/example/blog/usuario/CadastroIT.java` cobrindo: cadastro válido → 201 com `email`, `username`, `token` não vazio, `bio` e `image` presentes como `null`, sem `password`/hash no corpo; username já existente → 409 `{"errors":{"username":["has already been taken"]}}`; e-mail existente com maiúsculas diferentes (`JAKE@jake.jake`) → 409 `{"errors":{"email":["has already been taken"]}}`; ambos duplicados → 409 com as duas chaves; `password` ausente → 422 `{"errors":{"password":["can't be blank"]}}`; `username`/`email`/`password` vazios ou só espaços → 422 com as três chaves; e-mail `nao-e-email` → 422 `{"errors":{"email":["is invalid"]}}`; corpo ausente, JSON malformado e corpo sem `user` → 422 `{"errors":{"body":["can't be empty"]}}`; `Content-Type` com `charset=utf-8` em sucesso e erro
- [X] T023 [P] [US1] Criar `UsuarioServiceCadastroTest` em `src/test/java/com/example/blog/usuario/service/UsuarioServiceCadastroTest.java` (Mockito, sem Spring): grava com senha codificada pelo `PasswordEncoder` e devolve token de `TokenService`; aplica `trim` em username/e-mail; conflito de username e/ou e-mail → `ConflitoException` com as chaves corretas; `DataIntegrityViolationException` com restrição `uq_usuario_email`/`uq_usuario_username` → `ConflitoException` com a chave correspondente

### Implementation for User Story 1

- [X] T024 [P] [US1] Criar records de entrada em `src/main/java/com/example/blog/usuario/web/`: `NovoUsuario.java` (`@NotBlank(message = MensagensErro.NAO_PODE_SER_VAZIO) String username`, `@NotBlank(...) @Email(message = MensagensErro.INVALIDO) String email`, `@NotBlank(...) String password`) e `CadastroRequest.java` (`@Valid @NotNull(message = MensagensErro.CORPO_VAZIO) NovoUsuario user`)
- [X] T025 [P] [US1] Criar records de saída em `src/main/java/com/example/blog/usuario/web/`: `UsuarioResponse.java` (`email`, `token`, `username`, `bio`, `image` — nulos serializados como `null`, nunca omitidos) e `UsuarioEnvelope.java` (`UsuarioResponse user`), com fábrica `de(UsuarioAutenticadoDto)`
- [X] T026 [P] [US1] Criar o record de serviço `UsuarioAutenticadoDto(Long id, String email, String username, String bio, String imagem, String token)` em `src/main/java/com/example/blog/usuario/service/UsuarioAutenticadoDto.java`
- [X] T027 [US1] Criar `UsuarioService` (`@Service`) em `src/main/java/com/example/blog/usuario/service/UsuarioService.java` com `@Transactional UsuarioAutenticadoDto cadastrar(String username, String email, String senha)`: aplica `trim`; acumula conflitos (`existsByUsername`, `existsByEmailIgnoreCase`) e lança `ConflitoException` com todas as chaves em conflito; cria `Usuario` com `passwordEncoder.encode(senha)` e `Instant.now(clock)`; `saveAndFlush`; traduz `DataIntegrityViolationException` cuja restrição seja `uq_usuario_email`/`uq_usuario_username` em `ConflitoException` (`email`/`username`); devolve o DTO com `tokenService.emitir(id)` (depende de T008, T016, T026)
- [X] T028 [US1] Criar `UsuarioController` (`@RestController`) em `src/main/java/com/example/blog/usuario/web/UsuarioController.java` com `@PostMapping("/api/users")` recebendo `@Valid @RequestBody CadastroRequest` e respondendo `ResponseEntity.status(201)` com `UsuarioEnvelope` (depende de T024, T025, T027)
- [X] T029 [US1] Rodar `./mvnw -q verify` e corrigir até `CadastroIT` e `UsuarioServiceCadastroTest` passarem

**Checkpoint**: cadastro completo e verificável de forma independente (MVP)

---

## Phase 4: User Story 2 - Login (Priority: P1)

**Goal**: `POST /api/users/login` devolve `200` com o `User` e token válido; credenciais inválidas → `401` (FR-005–FR-007)

**Independent Test**: criar usuário via `ApoioDeTeste.criarUsuario` (sem depender do endpoint de cadastro), fazer login → `200` com token; senha errada ou e-mail inexistente → mesmo `401`

### Tests for User Story 2 ⚠️

- [X] T030 [P] [US2] Criar `LoginIT` em `src/test/java/com/example/blog/usuario/LoginIT.java`: credenciais corretas → 200 com `user` completo e `token` não vazio; e-mail com maiúsculas diferentes → 200; senha errada e e-mail inexistente → 401 com corpo idêntico `{"errors":{"email or password":["is invalid"]}}`; `email` ausente → 422 `{"errors":{"email":["can't be blank"]}}`; `email` e `password` vazios → 422 com as duas chaves; corpo sem `user` → 422 `body`; `Content-Type` com `charset=utf-8`
- [X] T031 [P] [US2] Criar `UsuarioServiceLoginTest` em `src/test/java/com/example/blog/usuario/service/UsuarioServiceLoginTest.java` (Mockito): usuário encontrado e senha confere → DTO com token; usuário inexistente ou `matches` falso → `CredenciaisInvalidasException`

### Implementation for User Story 2

- [X] T032 [P] [US2] Criar records `Credenciais.java` (`@NotBlank(message = MensagensErro.NAO_PODE_SER_VAZIO) String email`, `@NotBlank(...) String password`) e `LoginRequest.java` (`@Valid @NotNull(message = MensagensErro.CORPO_VAZIO) Credenciais user`) em `src/main/java/com/example/blog/usuario/web/`
- [X] T033 [US2] Adicionar a `UsuarioService` (`src/main/java/com/example/blog/usuario/service/UsuarioService.java`) o método `@Transactional(readOnly = true) UsuarioAutenticadoDto autenticar(String email, String senha)`: `findByEmailIgnoreCase(email.trim())` + `passwordEncoder.matches`; falha em qualquer um → `CredenciaisInvalidasException` (mesma resposta para os dois casos); sucesso → DTO com token novo
- [X] T034 [US2] Adicionar a `UsuarioController` (`src/main/java/com/example/blog/usuario/web/UsuarioController.java`) `@PostMapping("/api/users/login")` com `@Valid @RequestBody LoginRequest` → `200` com `UsuarioEnvelope` (depende de T032, T033)
- [X] T035 [US2] Rodar `./mvnw -q verify` e corrigir até `LoginIT` e `UsuarioServiceLoginTest` passarem

**Checkpoint**: US1 e US2 funcionam de forma independente

---

## Phase 5: User Story 3 - Consultar e atualizar o usuário autenticado (Priority: P2)

**Goal**: `GET /api/user` e `PUT /api/user` com autenticação obrigatória e atualização parcial (FR-008–FR-018)

**Independent Test**: com usuário e token criados por `ApoioDeTeste`, `GET /api/user` → 200; `PUT /api/user` com `bio` → 200 refletido no `GET` seguinte; sem token → 401 `is missing`

### Tests for User Story 3 ⚠️

- [X] T036 [P] [US3] Criar `UsuarioAtualIT` em `src/test/java/com/example/blog/usuario/UsuarioAtualIT.java`: `GET /api/user` com `Authorization: Token <t>` → 200 com dados e `token`; sem cabeçalho → 401 `{"errors":{"token":["is missing"]}}` (GET e PUT); `Bearer <t>`, `Token abc.def.ghi`, token expirado, token com outro segredo e token de usuário apagado → 401 `{"errors":{"token":["is invalid"]}}`; PUT com `bio` e `image` → 200 e demais campos inalterados; PUT com `bio: null` → `bio` volta `null`; PUT com `password` → login com a nova senha 200 e com a antiga 401; PUT com `email`/`username` de outra conta → 422 `has already been taken` na chave do campo; PUT com o próprio e-mail/username → 200; PUT com `{"user":{}}` → 422 `{"errors":{"user":["can't be blank"]}}`; PUT com `email: ""`, `username: null`, `password: "  "` → 422 `can't be blank` nas três chaves; PUT com e-mail inválido → 422 `is invalid`; PUT sem corpo → 422 `body`; `Content-Type` com `charset=utf-8` inclusive nos 401
- [X] T037 [P] [US3] Criar `AtualizacaoUsuarioMapperTest` em `src/test/java/com/example/blog/usuario/web/AtualizacaoUsuarioMapperTest.java`: campo ausente → `CampoAtualizado.ausente()`; `bio: null` → presente com valor nulo; nenhum campo aceito (só campos desconhecidos ou objeto vazio) → `ValidacaoException` `user: can't be blank`; `email`/`username`/`password` nulos, vazios ou só espaços → `can't be blank`; e-mail sem formato válido → `is invalid`; `bio`/`image` não textuais → `is invalid`; `user` nulo ou não objeto → `body: can't be empty`; vários erros acumulados
- [X] T038 [P] [US3] Criar `UsuarioServiceAtualizacaoTest` em `src/test/java/com/example/blog/usuario/service/UsuarioServiceAtualizacaoTest.java` (Mockito, `Clock.fixed`): só os campos presentes mudam; senha recodificada; e-mail/username de outra conta → `ValidacaoException` `has already been taken`; usuário do token inexistente → `TokenInvalidoException`; `atualizadoEm` = instante do `Clock`

### Implementation for User Story 3

- [X] T039 [P] [US3] Criar o record genérico `CampoAtualizado<T>(boolean presente, T valor)` com fábricas `ausente()` e `de(T valor)` em `src/main/java/com/example/blog/usuario/service/CampoAtualizado.java` e o record de comando `AtualizacaoUsuario(CampoAtualizado<String> email, CampoAtualizado<String> username, CampoAtualizado<String> senha, CampoAtualizado<String> bio, CampoAtualizado<String> imagem)` em `src/main/java/com/example/blog/usuario/service/AtualizacaoUsuario.java`
- [X] T040 [P] [US3] Criar o record `AtualizacaoRequest(tools.jackson.databind.JsonNode user)` em `src/main/java/com/example/blog/usuario/web/AtualizacaoRequest.java`
- [X] T041 [US3] Criar `AtualizacaoUsuarioMapper` (`@Component`) em `src/main/java/com/example/blog/usuario/web/AtualizacaoUsuarioMapper.java` convertendo `AtualizacaoRequest` em `AtualizacaoUsuario` conforme research R7: campos aceitos `email`, `username`, `password`, `bio`, `image` (`image` → `imagem`, `password` → `senha`); campos desconhecidos ignorados; aplica `trim` em `email`/`username`; regras e mensagens exatamente como em T037, acumulando todos os erros numa única `ValidacaoException`; validação de formato de e-mail com o `Validator` do Jakarta (`@Email`), mesma regra do cadastro (depende de T039, T040)
- [X] T042 [US3] Adicionar a `UsuarioService` (`src/main/java/com/example/blog/usuario/service/UsuarioService.java`): `@Transactional(readOnly = true) UsuarioAutenticadoDto buscarAtual(Long id)` (inexistente → `TokenInvalidoException`) e `@Transactional UsuarioAutenticadoDto atualizar(Long id, AtualizacaoUsuario)`: inexistente → `TokenInvalidoException`; verifica `existsByEmailIgnoreCaseAndIdNot`/`existsByUsernameAndIdNot` e lança `ValidacaoException` `has already been taken` com todas as chaves em conflito; aplica só campos presentes (senha via `passwordEncoder.encode`); `registrarAlteracao(Instant.now(clock))`; `saveAndFlush`, traduzindo violação de `uq_usuario_email`/`uq_usuario_username` em `ValidacaoException`; devolve DTO com token novo (depende de T039)
- [X] T043 [US3] Adicionar a `UsuarioController` (`src/main/java/com/example/blog/usuario/web/UsuarioController.java`) `@GetMapping("/api/user")` e `@PutMapping("/api/user")` (este com `@RequestBody AtualizacaoRequest` + `AtualizacaoUsuarioMapper`), ambos com `@AuthenticationPrincipal Jwt` → `UsuarioAutenticado.id(jwt)`, respondendo `200` com `UsuarioEnvelope` (depende de T041, T042)
- [X] T044 [US3] Rodar `./mvnw -q verify` e corrigir até `UsuarioAtualIT`, `AtualizacaoUsuarioMapperTest` e `UsuarioServiceAtualizacaoTest` passarem

**Checkpoint**: US1–US3 funcionam de forma independente

---

## Phase 6: User Story 4 - Consultar perfil público (Priority: P2)

**Goal**: `GET /api/profiles/{username}` com autenticação opcional, `following` calculado (FR-019–FR-021)

**Independent Test**: com usuários criados por `ApoioDeTeste` (e uma relação de seguir inserida diretamente pelo repositório), consultar o perfil sem token → `following: false`; com token de quem segue → `true`; inexistente → 404

### Tests for User Story 4 ⚠️

- [X] T045 [P] [US4] Criar `PerfilIT` em `src/test/java/com/example/blog/perfil/PerfilIT.java`: sem token → 200 `{"profile":{"username","bio","image","following":false}}` com `bio`/`image` presentes mesmo nulos; com token de quem segue (relação inserida via `SeguimentoRepository`) → `following: true`; com token de quem não segue → `false`; usuário inexistente → 404 `{"errors":{"profile":["not found"]}}`; `Token abc.def.ghi` e `Bearer <t>` → 401 `{"errors":{"token":["is invalid"]}}`; username com maiúsculas diferentes → 404 (sensível a maiúsculas); `Content-Type` com `charset=utf-8`
- [X] T046 [P] [US4] Criar `PerfilServiceTest` em `src/test/java/com/example/blog/perfil/service/PerfilServiceTest.java` (Mockito) para `buscarPerfil`: anônimo → `seguindo = false` sem consultar `SeguimentoRepository`; autenticado que segue/não segue; inexistente → `RecursoNaoEncontradoException` com chave `profile`

### Implementation for User Story 4

- [X] T047 [US4] Criar `src/main/resources/db/migration/V2__criar_tabela_seguimento.sql`: tabela `public.seguimento` com `seguidor_id bigint not null`, `seguido_id bigint not null`, `criado_em timestamptz not null default now()`; `constraint pk_seguimento primary key (seguidor_id, seguido_id)`; `constraint fk_seguimento_seguidor foreign key (seguidor_id) references public.usuario (id)`; `constraint fk_seguimento_seguido foreign key (seguido_id) references public.usuario (id)`; `constraint ck_seguimento_nao_segue_a_si_mesmo check (seguidor_id <> seguido_id)`; `create index ix_seguimento_seguido_id on public.seguimento (seguido_id)`
- [X] T048 [P] [US4] Criar `SeguimentoId` (`@Embeddable`, `Serializable`, construtor sem argumentos `protected`, campos `seguidorId`→`seguidor_id` e `seguidoId`→`seguido_id`, `equals`/`hashCode` pelos dois ids) em `src/main/java/com/example/blog/perfil/persistence/SeguimentoId.java` e a entidade `Seguimento` (`@EmbeddedId SeguimentoId id`, `Instant criadoEm`→`criado_em`, sem `@ManyToOne`, construtor `Seguimento(SeguimentoId, Instant)`) em `src/main/java/com/example/blog/perfil/persistence/Seguimento.java`
- [X] T049 [US4] Criar `SeguimentoRepository extends JpaRepository<Seguimento, SeguimentoId>` em `src/main/java/com/example/blog/perfil/persistence/SeguimentoRepository.java` (usa `existsById`/`deleteById` herdados) (depende de T048)
- [X] T050 [P] [US4] Criar o record `PerfilDto(String username, String bio, String imagem, boolean seguindo)` em `src/main/java/com/example/blog/perfil/service/PerfilDto.java` e os records `PerfilResponse(String username, String bio, String image, boolean following)` (nulos serializados como `null`) e `PerfilEnvelope(PerfilResponse profile)` com fábrica `de(PerfilDto)` em `src/main/java/com/example/blog/perfil/web/`
- [X] T051 [US4] Criar `PerfilService` (`@Service`) em `src/main/java/com/example/blog/perfil/service/PerfilService.java` com `@Transactional(readOnly = true) PerfilDto buscarPerfil(String username, Long idAutenticadoOuNulo)`: `usuarioRepository.findByUsername` (inexistente → `RecursoNaoEncontradoException("profile")`); se houver autenticado, confirma que ele existe (`existsById`, senão `TokenInvalidoException`) e calcula `seguindo` com `seguimentoRepository.existsById(new SeguimentoId(autenticado, alvo))`; anônimo → `false` (depende de T049, T050)
- [X] T052 [US4] Criar `PerfilController` (`@RestController`) em `src/main/java/com/example/blog/perfil/web/PerfilController.java` com `@GetMapping("/api/profiles/{username}")`, `@AuthenticationPrincipal Jwt jwt` (pode ser nulo) → `UsuarioAutenticado.idOuNulo(jwt)` → `200` com `PerfilEnvelope` (depende de T051)
- [X] T053 [US4] Atualizar `src/test/java/com/example/blog/MigracoesIT.java` para afirmar `V1` e `V2` com `success = true`, e confirmar que `ApoioDeTeste.limparBanco()` apaga `seguimento` antes de `usuario`; rodar `./mvnw -q verify` e corrigir até `PerfilIT`, `PerfilServiceTest` e `MigracoesIT` passarem

**Checkpoint**: US1–US4 funcionam de forma independente

---

## Phase 7: User Story 5 - Seguir e deixar de seguir (Priority: P3)

**Goal**: `POST`/`DELETE /api/profiles/{username}/follow`, idempotentes, autenticação obrigatória (FR-022–FR-025)

**Independent Test**: com dois usuários criados por `ApoioDeTeste`, o primeiro segue (`following: true`, duas vezes sem duplicar) e deixa de seguir (`following: false`, duas vezes)

### Tests for User Story 5 ⚠️

- [X] T054 [P] [US5] Criar `SeguimentoIT` em `src/test/java/com/example/blog/perfil/SeguimentoIT.java`: `POST follow` → 200 `following: true` e 1 linha em `seguimento`; repetir → 200 `true` e ainda 1 linha; `GET` do perfil com o mesmo token → `true`; `DELETE follow` → 200 `false` e 0 linhas; repetir `DELETE` → 200 `false`; `DELETE` sem seguir antes → 200 `false`; alvo inexistente em `POST` e `DELETE` → 404 `{"errors":{"profile":["not found"]}}`; seguir a si mesmo → 422 `{"errors":{"profile":["cannot follow yourself"]}}`; sem cabeçalho → 401 `token: is missing`; token inválido → 401 `token: is invalid`; `Content-Type` com `charset=utf-8`
- [X] T055 [P] [US5] Criar `PerfilServiceSeguimentoTest` em `src/test/java/com/example/blog/perfil/service/PerfilServiceSeguimentoTest.java` (Mockito, `Clock.fixed`): seguir insere só se não existir; seguir a si mesmo → `ValidacaoException` `profile: cannot follow yourself` sem gravar; deixar de seguir remove só se existir; alvo inexistente → `RecursoNaoEncontradoException`; autenticado inexistente → `TokenInvalidoException`

### Implementation for User Story 5

- [X] T056 [US5] Adicionar a `PerfilService` (`src/main/java/com/example/blog/perfil/service/PerfilService.java`): `@Transactional PerfilDto seguir(String username, Long idAutenticado)` — alvo inexistente → 404; autenticado inexistente → `TokenInvalidoException`; alvo == autenticado → `ValidacaoException.de("profile", "cannot follow yourself")`; se `!existsById` grava `new Seguimento(id, Instant.now(clock))` com `saveAndFlush`, tratando violação de `pk_seguimento` por corrida como já seguindo; devolve `seguindo = true` — e `@Transactional PerfilDto deixarDeSeguir(String username, Long idAutenticado)` — mesmas checagens de existência; `deleteById` se existir; devolve `seguindo = false` (depende de T051)
- [X] T057 [US5] Adicionar a `PerfilController` (`src/main/java/com/example/blog/perfil/web/PerfilController.java`) `@PostMapping("/api/profiles/{username}/follow")` e `@DeleteMapping("/api/profiles/{username}/follow")` com `@AuthenticationPrincipal Jwt` → `UsuarioAutenticado.id(jwt)` → `200` com `PerfilEnvelope` (depende de T056)
- [X] T058 [US5] Rodar `./mvnw -q verify` e corrigir até `SeguimentoIT` e `PerfilServiceSeguimentoTest` passarem

**Checkpoint**: todas as histórias funcionam de forma independente

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: fatias web, documentação, revisão de segurança e validação final

- [X] T059 [P] Criar `UsuarioControllerTest` (`@WebMvcTest(UsuarioController.class)`, `@Import` de `SegurancaConfig`, `ErroAutenticacaoHandler`, `ManipuladorDeErros`, `AtualizacaoUsuarioMapper`; `@MockitoBean UsuarioService`; `@ActiveProfiles("test")`) em `src/test/java/com/example/blog/usuario/web/UsuarioControllerTest.java`, verificando forma do JSON (`user` com 5 campos, `bio`/`image` `null` presentes), status 201/200/401/422 e ausência de senha no corpo
- [X] T060 [P] Criar `PerfilControllerTest` (`@WebMvcTest(PerfilController.class)`, mesmas importações, `@MockitoBean PerfilService`) em `src/test/java/com/example/blog/perfil/web/PerfilControllerTest.java`, verificando forma do JSON `profile` (4 campos), 200/401/404/422 e rota opcional sem token
- [X] T061 [P] Criar `README.md` na raiz com: pré-requisitos (JDK 25, Docker), criação do `.env` a partir de `.env.example` (incluindo `JWT_SECRET`, sem valores reais), `docker compose up -d db`, `./mvnw -q spring-boot:run`, `./mvnw -q verify`, lista dos 7 endpoints com link para `specs/001-conduit-usuarios-perfis/contracts/`
- [X] T062 Revisar o código para o checklist de `tech.md` §15/§27: nenhum `System.out`, segredo, senha/hash/token em log ou resposta; nenhum `@Autowired` em campo; `@Transactional` apenas em serviços; imports sem curinga e sem não usados; nenhuma API do Spring Boot 3.x (`@MockBean`, `javax.*`, `com.fasterxml.jackson`); Javadoc nas classes públicas de serviço e segurança
- [X] T063 Rodar `./mvnw -q verify` completo (unitários + ITs com Testcontainers) e mostrar ao usuário somente erros e as últimas 50 linhas de log, se houver falha
- [X] T064 Executar o roteiro de [quickstart.md](quickstart.md) §2–§3 contra o banco local (`docker compose up -d db`, `docker compose ps` saudável, aplicação em 8080) e confirmar status, corpos e `Content-Type` de cada chamada; resumir o resultado em uma ou duas frases

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências
- **Foundational (Phase 2)**: depende do Setup — **bloqueia** todas as histórias
- **US1–US5 (Phases 3–7)**: dependem só da Phase 2; podem seguir em paralelo ou na ordem P1 → P2 → P3
- **Polish (Phase 8)**: depois das histórias desejadas

### User Story Dependencies

- **US1 (P1)**: só Foundational
- **US2 (P1)**: só Foundational (usuários criados por `ApoioDeTeste`); compartilha `UsuarioService`/`UsuarioController` com US1 — se em paralelo, coordenar edições nesses arquivos
- **US3 (P2)**: só Foundational; mesmos arquivos compartilhados de US1/US2
- **US4 (P2)**: só Foundational; cria `V2`, `Seguimento` e `PerfilService`
- **US5 (P3)**: depende de US4 (tabela `seguimento`, `PerfilService`, `PerfilController`)

### Within Each User Story

- Testes primeiro (devem falhar) → records/entidades → serviço → controller → `./mvnw -q verify`
- Arquivos compartilhados (`UsuarioService.java`, `UsuarioController.java`, `PerfilService.java`, `PerfilController.java`) são editados em sequência, nunca em paralelo

### Parallel Opportunities

- Setup: T002, T003, T004, T005
- Foundational: T007, T009, T010, T011, T013, T014, T015, T018 em paralelo; depois T008, T012, T016, T017; testes T019, T020 em paralelo; T021 por último
- Em cada história, as tarefas de teste [P] e os records [P]
- US1/US2/US3 (arquivos de `usuario`) e US4 (arquivos de `perfil`) podem ser feitas por pessoas diferentes após a Phase 2
- Polish: T059, T060, T061

---

## Parallel Example: User Story 1

```bash
# Testes de US1 juntos:
Task: "CadastroIT em src/test/java/com/example/blog/usuario/CadastroIT.java"
Task: "UsuarioServiceCadastroTest em src/test/java/com/example/blog/usuario/service/UsuarioServiceCadastroTest.java"

# Records de US1 juntos:
Task: "NovoUsuario/CadastroRequest em src/main/java/com/example/blog/usuario/web/"
Task: "UsuarioResponse/UsuarioEnvelope em src/main/java/com/example/blog/usuario/web/"
Task: "UsuarioAutenticadoDto em src/main/java/com/example/blog/usuario/service/UsuarioAutenticadoDto.java"
```

## Parallel Example: User Story 4

```bash
Task: "PerfilIT em src/test/java/com/example/blog/perfil/PerfilIT.java"
Task: "PerfilServiceTest em src/test/java/com/example/blog/perfil/service/PerfilServiceTest.java"
Task: "SeguimentoId/Seguimento em src/main/java/com/example/blog/perfil/persistence/"
Task: "PerfilDto/PerfilResponse/PerfilEnvelope"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (Setup) → Phase 2 (Foundational)
2. Phase 3 (US1 — cadastro)
3. **Parar e validar**: `./mvnw -q verify` com `CadastroIT` verde
4. Demonstrar o cadastro via quickstart

### Incremental Delivery

1. Setup + Foundational → base pronta
2. US1 (cadastro) → MVP
3. US2 (login) → contas reutilizáveis
4. US3 (usuário atual) → manutenção da conta
5. US4 (perfil) → visão pública
6. US5 (seguir) → relação social
7. Polish → fatias web, README, validação final

---

## Notes

- [P] = arquivos diferentes, sem dependências pendentes
- Cada história é testável sozinha: os ITs criam seus próprios usuários e tokens com `ApoioDeTeste`, sem depender de endpoints de outras histórias (exceto US5, que usa a estrutura criada em US4)
- Migrations aplicadas são imutáveis: se `V1`/`V2` precisarem mudar depois de commitadas, criar `V3`
- Não use `docker compose down -v` sem confirmação explícita do usuário
- Commit apenas quando o usuário pedir
