---

description: "Lista de tarefas da feature Conduit — Recorte A"
---

# Tasks: Conduit — Recorte A (usuários, autenticação e perfis)

**Input**: Design documents from `specs/001-conduit-usuarios-perfis/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

**Tests**: Incluídos. As regras do projeto (`.claude/rules/tech.md` §11, §15, §26 e §8 de migrations) exigem cobertura de comportamento novo, teste de migrations em banco vazio e Testcontainers. Escreva os testes de cada história antes da implementação e confirme que falham.

**Organization**: Tarefas agrupadas por história de usuário (US1–US4 da spec).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[Story]**: história a que a tarefa pertence (US1, US2, US3, US4)

## Path Conventions

- `MAIN` = `src/main/java/com/example/blog`
- `TEST` = `src/test/java/com/example/blog`
- `RES` = `src/main/resources`
- Código, mensagens e nomes seguem `plan.md` (Project Structure) e `.claude/rules/tech.md`. Indentação com 4 espaços, imports explícitos, injeção por construtor, DTOs como records, Jackson 3 (`tools.jackson.*`), `jakarta.*`.
- Rode o Maven sempre com `-q` (`./mvnw -q verify`) e mostre só erros e as últimas 50 linhas.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: dependências, configuração e suporte de testes.

- [X] T001 Em `pom.xml`, adicionar a dependência `org.springframework.boot:spring-boot-starter-security-oauth2-resource-server` (sem versão; confirme o nome do starter na documentação do Boot 4.1.x — research R1) e a dependência de teste `org.springframework.boot:spring-boot-starter-security-test` (escopo `test`, se existir no Boot 4; senão `org.springframework.security:spring-security-test`); em `<build><plugins>`, adicionar `org.apache.maven.plugins:maven-failsafe-plugin` (versão gerenciada pelo parent) com os goals `integration-test` e `verify`
- [X] T002 [P] Em `.env.example`, acrescentar `JWT_SECRET=<segredo_aleatorio_de_pelo_menos_32_bytes>` (valor claramente fictício); se o `.env` local existir, acrescentar `JWT_SECRET` com valor gerado por `python -c "import secrets; print(secrets.token_urlsafe(48))"` gravado direto no arquivo, sem imprimir o valor
- [X] T003 [P] Em `RES/application.yml`, acrescentar `conduit.jwt.segredo: ${JWT_SECRET}`, `conduit.jwt.validade: PT24H`, `conduit.jwt.emissor: conduit` e `server.servlet.encoding.charset: UTF-8`, `server.servlet.encoding.enabled: true`, `server.servlet.encoding.force-response: true` (research R7, R12)
- [X] T004 [P] Em `TEST/TestcontainersConfiguration.java`, registrar um bean `DynamicPropertyRegistrar` que define `conduit.jwt.segredo` com um valor aleatório gerado em tempo de execução (`SecureRandom`, 48 bytes em Base64), para que nenhum segredo de teste fique versionado (research R11)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: esquema de usuário, entidade, tratamento de erros e segurança — base de todas as histórias.

**⚠️ CRITICAL**: nenhuma história começa antes desta fase.

- [X] T005 Criar `RES/db/migration/V1__criar_tabela_usuario.sql` com a tabela `public.usuario`: `id bigint generated always as identity` (`pk_usuario`); `username varchar(255) NOT NULL` com `uq_usuario_username`; `email varchar(255) NOT NULL`; `senha_hash varchar(100) NOT NULL`; `bio text NULL`; `image text NULL`; `criado_em timestamptz NOT NULL DEFAULT now()`; `atualizado_em timestamptz NOT NULL DEFAULT now()`; `ck_usuario_username_nao_vazio CHECK (btrim(username) <> '')`; `ck_usuario_email_nao_vazio CHECK (btrim(email) <> '')`; e `CREATE UNIQUE INDEX uq_usuario_email_lower ON public.usuario (lower(email))`
- [X] T006 [P] Criar a entidade JPA `MAIN/usuario/persistence/Usuario.java` mapeando a tabela `usuario` (colunas de T005; `criado_em`/`atualizado_em` como `Instant`), construtor protegido sem argumentos, construtor público `(username, email, senhaHash, agora)`, getters apenas de leitura e métodos de negócio `alterarEmail`, `alterarUsername`, `alterarSenhaHash`, `alterarBio`, `alterarImage` (cada um atualiza `atualizadoEm` com o `Instant` recebido); `equals`/`hashCode` pelo `id` (entidade sem id nunca é igual a outra; `hashCode` constante da classe)
- [X] T007 Criar `MAIN/usuario/persistence/UsuarioRepository.java` (`JpaRepository<Usuario, Long>`) com `Optional<Usuario> findByUsername(String)`, `Optional<Usuario> findByEmailIgnoreCase(String)`, `boolean existsByUsername(String)`, `boolean existsByEmailIgnoreCase(String)`, `boolean existsByUsernameAndIdNot(String, Long)`, `boolean existsByEmailIgnoreCaseAndIdNot(String, Long)` (depende de T006)
- [X] T008 [P] Criar `MAIN/shared/erro/ErroResponse.java`: record `ErroResponse(Map<String, List<String>> errors)` com fábrica estática `de(String chave, String mensagem)`; cópia imutável do mapa no construtor compacto
- [X] T009 [P] Criar as exceções de domínio não verificadas em `MAIN/shared/erro/`: `RecursoNaoEncontradoException`, `ConflitoException`, `ValidacaoException`, `CredenciaisInvalidasException`, `TokenInvalidoException`; cada uma (exceto as duas últimas, de mensagem fixa) recebe `chave` e `mensagem` do erro do contrato e as expõe por getters
- [X] T010 Criar `MAIN/shared/erro/TratadorGlobalDeErros.java` (`@RestControllerAdvice`) que devolve `ErroResponse`: `MethodArgumentNotValidException` → `422`, agrupando mensagens por campo e removendo o prefixo `user.` do caminho do campo (ex.: `user.email` → `email`); `HttpMessageNotReadableException` → `422` `{"errors":{"body":["is invalid"]}}`; `RecursoNaoEncontradoException` → `404`; `ConflitoException` → `409`; `ValidacaoException` → `422`; `CredenciaisInvalidasException` → `401` `{"errors":{"credentials":["invalid"]}}`; `TokenInvalidoException` → `401` `{"errors":{"token":["is invalid"]}}`; `Exception` → `500` `{"errors":{"server":["unexpected error"]}}` com log `ERROR` (SLF4J, exceção como último argumento) e sem detalhes internos na resposta (research R5; depende de T008, T009)
- [X] T011 [P] Criar `MAIN/shared/seguranca/JwtProperties.java`: record `@ConfigurationProperties("conduit.jwt") @Validated` com `@NotBlank String segredo` (validar no construtor compacto que tenha pelo menos 32 bytes em UTF-8), `@NotNull Duration validade`, `@NotBlank String emissor`; habilitar com `@EnableConfigurationProperties` em `SegurancaConfig`
- [X] T012 [P] Criar `MAIN/shared/seguranca/TokenHeaderResolver.java` implementando `BearerTokenResolver`: cabeçalho `Authorization` ausente → `null`; valor iniciando com `Token ` seguido de texto não vazio → devolve o texto; qualquer outro valor → lança `InvalidBearerTokenException` (research R2)
- [X] T013 [P] Criar `MAIN/shared/seguranca/ErroAutenticacaoEntryPoint.java` (`AuthenticationEntryPoint`): responde `401`, `Content-Type: application/json;charset=UTF-8`, corpo `{"errors":{"token":["is missing"]}}` quando a requisição não tem cabeçalho `Authorization`, e `{"errors":{"token":["is invalid"]}}` nos demais casos; serializar com o `JsonMapper` do Jackson 3 injetado (research R4)
- [X] T014 Criar `MAIN/shared/seguranca/SegurancaConfig.java` (`@Configuration`): beans `PasswordEncoder` (`BCryptPasswordEncoder`), `Clock` (`Clock.systemUTC()`), `JwtEncoder` (`NimbusJwtEncoder` com `ImmutableSecret` da chave HS256 de `JwtProperties.segredo`), `JwtDecoder` (`NimbusJwtDecoder.withSecretKey(...).macAlgorithm(HS256)` com validadores de timestamp e emissor `JwtValidators.createDefaultWithIssuer(emissor)`) e `SecurityFilterChain`: CSRF desabilitado, sessão `STATELESS`, `permitAll` para `POST /api/users`, `POST /api/users/login`, `GET /api/profiles/*`, `GET /actuator/health`, `GET /actuator/info`; `authenticated()` para o resto; `oauth2ResourceServer(jwt)` com `TokenHeaderResolver` e `ErroAutenticacaoEntryPoint`; `exceptionHandling().authenticationEntryPoint(ErroAutenticacaoEntryPoint)` (research R1–R4; depende de T011–T013)
- [X] T015 Criar `MAIN/shared/seguranca/TokenService.java` (`@Service`): `String emitir(long idUsuario)` gera JWT com `sub` = id, `iss` = emissor, `iat` = `Instant.now(clock)`, `exp` = `iat + validade`, cabeçalho `alg=HS256`; e `long idDoUsuario(Jwt jwt)` que converte `sub` para `long` (lança `TokenInvalidoException` se não for numérico) (depende de T014)
- [X] T016 [P] Criar teste unitário `TEST/shared/seguranca/TokenHeaderResolverTest.java`: sem cabeçalho → `null`; `Token abc` → `abc`; `Bearer abc`, `Token ` e `Token` → exceção
- [X] T017 [P] Criar teste unitário `TEST/shared/seguranca/TokenServiceTest.java` com `Clock` fixo: o token emitido decodifica com o `JwtDecoder` de `SegurancaConfig`, `sub` igual ao id, `exp` = `iat + 24h`; token com assinatura alterada ou expirado é rejeitado
- [X] T018 Criar `TEST/MigracoesIT.java`: `@SpringBootTest` + `@Import(TestcontainersConfiguration.class)` que sobe o contexto (aplica todas as migrations em banco vazio com `ddl-auto: validate`) e verifica, via `JdbcTemplate`, que `flyway_schema_history` tem todas as versões com `success = true`

**Checkpoint**: `./mvnw -q verify` passa; base pronta.

---

## Phase 3: User Story 1 - Cadastro e login (Priority: P1) 🎯 MVP

**Goal**: `POST /api/users` (201) e `POST /api/users/login` (200) devolvendo `{"user": {...}}` com token.

**Independent Test**: cadastrar um usuário, fazer login com as mesmas credenciais e conferir `user.token` nas duas respostas.

### Tests for User Story 1 ⚠️

- [X] T019 [P] [US1] Criar `TEST/usuario/service/UsuarioServiceTest.java` (Mockito, `Clock` fixo) cobrindo `cadastrar`: sucesso grava hash (nunca a senha) e devolve token de `TokenService`; `username` existente → `ConflitoException("username", "has already been taken")`; e-mail existente sem diferenciar maiúsculas → `ConflitoException("email", "has already been taken")`; e `autenticar`: sucesso; e-mail inexistente e senha errada → `CredenciaisInvalidasException`
- [X] T020 [P] [US1] Criar `TEST/usuario/web/UsuarioControllerCadastroLoginTest.java` (`@WebMvcTest(UsuarioController.class)`, `@Import` de `SegurancaConfig`/`TratadorGlobalDeErros`, `@MockitoBean UsuarioService`): `POST /api/users` válido → `201` e JSON `user.{email,token,username,bio:null,image:null}`; campos ausentes/brancos → `422` com `errors.<campo>` = `["can't be blank"]`; e-mail sem formato → `422` `errors.email` = `["is invalid"]`; JSON malformado → `422`; sem objeto `user` → `422`; `POST /api/users/login` válido → `200`; credenciais inválidas → `401` `errors.credentials`
- [X] T021 [P] [US1] Criar `TEST/usuario/UsuarioApiIT.java` (`@SpringBootTest(webEnvironment = RANDOM_PORT)` + Testcontainers, cliente HTTP real `RestTestClient` do Spring Framework 7) com dados fictícios únicos por teste: cadastro → `201`; cadastro repetido → `409` `errors.username`; cadastro com e-mail que difere só em maiúsculas → `409` `errors.email`; login → `200` com token; senha errada → `401`; e todas as respostas com `Content-Type` `application/json;charset=UTF-8`

### Implementation for User Story 1

- [X] T022 [P] [US1] Criar `MAIN/usuario/web/CadastroRequest.java`: record `CadastroRequest(@Valid @NotNull(message = "can't be blank") Dados user)` com record aninhado `Dados(@NotBlank(message = "can't be blank") String username, @NotBlank(message = "can't be blank") @Email(message = "is invalid") String email, @NotBlank(message = "can't be blank") String password)`
- [X] T023 [P] [US1] Criar `MAIN/usuario/web/LoginRequest.java`: record `LoginRequest(@Valid @NotNull(message = "can't be blank") Dados user)` com `Dados(@NotBlank(message = "can't be blank") String email, @NotBlank(message = "can't be blank") String password)`
- [X] T024 [P] [US1] Criar `MAIN/usuario/service/UsuarioAutenticado.java`: record `(String email, String token, String username, String bio, String image)` e `MAIN/usuario/web/UsuarioResponse.java`: record `UsuarioResponse(Dados user)` com `Dados(email, token, username, bio, image)` e fábrica `de(UsuarioAutenticado)`; `bio`/`image` nulos devem ser serializados como `null` (não omitidos)
- [X] T025 [US1] Criar `MAIN/usuario/service/UsuarioService.java` (`@Service`, injeção por construtor de `UsuarioRepository`, `PasswordEncoder`, `TokenService`, `Clock`) com `@Transactional UsuarioAutenticado cadastrar(String username, String email, String senha)` (verifica `existsByUsername` → `ConflitoException("username","has already been taken")` e `existsByEmailIgnoreCase` → `ConflitoException("email","has already been taken")`; grava com `PasswordEncoder.encode`; captura `DataIntegrityViolationException` de corrida e converte no mesmo `ConflitoException` conforme a restrição `uq_usuario_username`/`uq_usuario_email_lower`) e `@Transactional(readOnly = true) UsuarioAutenticado autenticar(String email, String senha)` (busca `findByEmailIgnoreCase`; ausente ou `matches` falso → `CredenciaisInvalidasException`); log `INFO` "Usuário {} cadastrado" só com o id (depende de T007, T015, T024)
- [X] T026 [US1] Criar `MAIN/usuario/web/UsuarioController.java` (`@RestController`, `@RequestMapping("/api")`) com `POST /users` (`@Valid @RequestBody CadastroRequest`) → `ResponseEntity.status(201)` com `UsuarioResponse` e `POST /users/login` (`@Valid @RequestBody LoginRequest`) → `200` com `UsuarioResponse` (depende de T022–T025)

**Checkpoint**: US1 funcional e testada isoladamente (T019–T021 passam).

---

## Phase 4: User Story 2 - Consultar e atualizar o próprio usuário (Priority: P2)

**Goal**: `GET /api/user` e `PUT /api/user` autenticados.

**Independent Test**: com o token do cadastro, `GET /api/user`, `PUT /api/user` alterando `bio`/`image`, e novo `GET` refletindo as alterações.

### Tests for User Story 2 ⚠️

- [X] T027 [P] [US2] Criar `TEST/usuario/web/AlteracaoUsuarioMapperTest.java`: campo ausente → não presente; `bio: null` → presente com valor `null`; `email`/`username`/`password` `null` ou em branco → `ValidacaoException` com chave do campo e `can't be blank`; e-mail sem formato → `ValidacaoException("email","is invalid")`; `{"user":{}}` ou só campos desconhecidos → `ValidacaoException("user","must have at least one field")`; ausência do objeto `user` → `ValidacaoException("user","can't be blank")`
- [X] T028 [P] [US2] Ampliar `TEST/usuario/service/UsuarioServiceTest.java` com `buscarAtual` (id inexistente → `TokenInvalidoException`) e `atualizar`: altera só os campos presentes; `bio`/`image` presentes com `null` apagam; nova senha grava novo hash; `username`/`email` de outro usuário → `ValidacaoException("<campo>","has already been taken")`; o mesmo valor do próprio usuário é aceito
- [X] T029 [P] [US2] Criar `TEST/usuario/web/UsuarioControllerAtualTest.java` (`@WebMvcTest`, `@MockitoBean UsuarioService`, JWT de teste via `SecurityMockMvcRequestPostProcessors.jwt()` ou cabeçalho `Token` real): `GET /api/user` sem cabeçalho → `401` `{"errors":{"token":["is missing"]}}`; com `Bearer x` → `401` `is invalid`; autenticado → `200`; `PUT /api/user` válido → `200`; `{"user":{}}` → `422`
- [X] T030 [P] [US2] Ampliar `TEST/usuario/UsuarioApiIT.java`: `GET /api/user` com token do cadastro → `200` com o mesmo token; `PUT` de `bio`/`image` reflete em novo `GET`; `PUT` com `image: null` devolve `image: null`; troca de senha → login com a antiga `401` e com a nova `200`; `PUT` com e-mail de outro usuário → `422` `errors.email` = `["has already been taken"]`; token expirado/assinatura inválida → `401`

### Implementation for User Story 2

- [X] T031 [P] [US2] Criar `MAIN/usuario/service/AlteracaoUsuario.java`: record com um componente por campo aceito (`email`, `username`, `password`, `bio`, `image`), cada um do tipo record aninhado `Campo(boolean presente, String valor)` com constante `AUSENTE`; método `boolean isVazia()` (research R6)
- [X] T032 [US2] Criar `MAIN/usuario/web/AlteracaoUsuarioMapper.java` (`@Component`) que converte o `JsonNode` do corpo (Jackson 3, `tools.jackson.databind.JsonNode`) em `AlteracaoUsuario`, aplicando as regras de formato: objeto `user` obrigatório; `email`/`username`/`password`, se presentes, não nulos e não brancos (`can't be blank`), `email` com formato válido (`is invalid`, mesma validação do `@Email` via `Validator`); `bio`/`image` aceitam texto ou `null`; ignora campos desconhecidos; nenhum campo aceito → `ValidacaoException("user","must have at least one field")`; tipo não texto → `ValidacaoException("<campo>","is invalid")` (depende de T031)
- [X] T033 [US2] Em `MAIN/usuario/service/UsuarioService.java`, acrescentar `@Transactional(readOnly = true) UsuarioAutenticado buscarAtual(long id, String token)` (usuário inexistente → `TokenInvalidoException`) e `@Transactional UsuarioAutenticado atualizar(long id, String token, AlteracaoUsuario alteracao)` que aplica somente os campos presentes, verificando `existsByUsernameAndIdNot`/`existsByEmailIgnoreCaseAndIdNot` → `ValidacaoException("<campo>","has already been taken")` (status `422`, não `409`), codificando a nova senha e usando `Instant.now(clock)`; ambos devolvem o token recebido (research R10)
- [X] T034 [US2] Em `MAIN/usuario/web/UsuarioController.java`, acrescentar `GET /user` e `PUT /user` (`@RequestBody JsonNode`), obtendo `@AuthenticationPrincipal Jwt jwt`, id via `TokenService.idDoUsuario(jwt)` e token via `jwt.getTokenValue()`; ambos `200` com `UsuarioResponse` (depende de T032, T033)

**Checkpoint**: US1 e US2 funcionam de forma independente.

---

## Phase 5: User Story 3 - Consultar perfil público (Priority: P3)

**Goal**: `GET /api/profiles/{username}` com autenticação opcional e `following` correto.

**Independent Test**: cadastrar um usuário e chamar `GET /api/profiles/{username}` sem token → `following: false`; nome inexistente → `404`.

### Tests for User Story 3 ⚠️

- [X] T035 [P] [US3] Criar `TEST/perfil/service/PerfilServiceTest.java` (Mockito): perfil existente sem usuário atual → `following=false`; com usuário atual que segue → `true`; que não segue → `false`; username inexistente → `RecursoNaoEncontradoException("profile","not found")`
- [X] T036 [P] [US3] Criar `TEST/perfil/web/PerfilControllerTest.java` (`@WebMvcTest(PerfilController.class)`, `@MockitoBean PerfilService`): `GET /api/profiles/ana` sem token → `200` `{"profile":{"username","bio","image","following":false}}` com `bio`/`image` `null` serializados; com token inválido → `401`; inexistente → `404` `{"errors":{"profile":["not found"]}}`
- [X] T037 [P] [US3] Criar `TEST/perfil/PerfilApiIT.java` (`RANDOM_PORT` + Testcontainers): perfil anônimo → `200` `following:false`; `Authorization: Token invalido` → `401`; inexistente → `404`; `Content-Type` `application/json;charset=UTF-8`

### Implementation for User Story 3

- [X] T038 [US3] Criar `RES/db/migration/V2__criar_tabela_seguimento.sql` com `public.seguimento`: `seguidor_id bigint NOT NULL` (`fk_seguimento_seguidor` → `public.usuario(id)` `ON DELETE CASCADE`), `seguido_id bigint NOT NULL` (`fk_seguimento_seguido` → `public.usuario(id)` `ON DELETE CASCADE`), `criado_em timestamptz NOT NULL DEFAULT now()`, `pk_seguimento PRIMARY KEY (seguidor_id, seguido_id)`, `ck_seguimento_nao_si_mesmo CHECK (seguidor_id <> seguido_id)` e `CREATE INDEX ix_seguimento_seguido_id ON public.seguimento (seguido_id)`
- [X] T039 [P] [US3] Criar `MAIN/perfil/persistence/SeguimentoId.java` (classe `@Embeddable`, `Serializable`, campos `seguidorId` e `seguidoId` `long`, construtor sem argumentos protegido, `equals`/`hashCode` pelos dois campos) e `MAIN/perfil/persistence/Seguimento.java` (entidade da tabela `seguimento` com `@EmbeddedId SeguimentoId id` e `Instant criadoEm`; sem associações `@ManyToOne`)
- [X] T040 [US3] Criar `MAIN/perfil/persistence/SeguimentoRepository.java` (`JpaRepository<Seguimento, SeguimentoId>`) — `existsById` cobre a consulta de `following` (depende de T039)
- [X] T041 [P] [US3] Em `MAIN/usuario/service/`, criar record `DadosPublicosUsuario(long id, String username, String bio, String image)` e acrescentar a `UsuarioService` `@Transactional(readOnly = true) DadosPublicosUsuario buscarPorUsername(String username)` (inexistente → `RecursoNaoEncontradoException("profile","not found")`)
- [X] T042 [US3] Criar `MAIN/perfil/service/Perfil.java` (record `username, bio, image, boolean following`) e `MAIN/perfil/service/PerfilService.java` (`@Service`, depende de `UsuarioService` e `SeguimentoRepository`) com `@Transactional(readOnly = true) Perfil buscar(String username)` (anônimo, `following=false`) e `@Transactional(readOnly = true) Perfil buscarComo(long idUsuarioAtual, String username)` (depende de T040, T041)
- [X] T043 [US3] Criar `MAIN/perfil/web/PerfilResponse.java` (record `PerfilResponse(Dados profile)`, fábrica `de(Perfil)`) e `MAIN/perfil/web/PerfilController.java` (`@RestController`, `@RequestMapping("/api/profiles")`) com `GET /{username}` usando `@AuthenticationPrincipal Jwt jwt` (nulo quando anônimo) para escolher `buscar` ou `buscarComo` (depende de T042)

**Checkpoint**: US3 funcional; `MigracoesIT` cobre V1 e V2.

---

## Phase 6: User Story 4 - Seguir e deixar de seguir (Priority: P4)

**Goal**: `POST` e `DELETE /api/profiles/{username}/follow`, idempotentes.

**Independent Test**: cadastrar dois usuários; com o token do primeiro, seguir e deixar de seguir o segundo, conferindo `following` em cada resposta.

### Tests for User Story 4 ⚠️

- [X] T044 [P] [US4] Ampliar `TEST/perfil/service/PerfilServiceTest.java` com `seguir` (cria relação; já existente → não duplica e devolve `following=true`; a si mesmo → `ValidacaoException("profile","cannot follow yourself")`; alvo inexistente → `404`) e `deixarDeSeguir` (remove; inexistente → `following=false` sem erro; alvo inexistente → `404`)
- [X] T045 [P] [US4] Ampliar `TEST/perfil/web/PerfilControllerTest.java`: `POST`/`DELETE /api/profiles/{u}/follow` sem token → `401` `is missing`; autenticado → `200` com `profile.following` `true`/`false`; seguir a si mesmo → `422` `{"errors":{"profile":["cannot follow yourself"]}}`
- [X] T046 [P] [US4] Ampliar `TEST/perfil/PerfilApiIT.java`: `POST .../follow` duas vezes → `200` `following:true` nas duas; `GET /api/profiles/{u}` com o token do seguidor → `following:true`; `DELETE .../follow` duas vezes → `200` `following:false`; seguir a si mesmo → `422`; alvo inexistente → `404`; sem token → `401`

### Implementation for User Story 4

- [X] T047 [US4] Em `MAIN/perfil/service/PerfilService.java`, acrescentar `@Transactional Perfil seguir(long idUsuarioAtual, String username)` (alvo via `UsuarioService.buscarPorUsername`; mesmo id → `ValidacaoException("profile","cannot follow yourself")`; grava `Seguimento` só se `!existsById`, tratando `DataIntegrityViolationException` de corrida como já seguido) e `@Transactional Perfil deixarDeSeguir(long idUsuarioAtual, String username)` (`deleteById` só se existir); ambos devolvem o perfil do alvo com `following` atualizado
- [X] T048 [US4] Em `MAIN/perfil/web/PerfilController.java`, acrescentar `POST /{username}/follow` e `DELETE /{username}/follow` (autenticados por `SegurancaConfig`), com id via `TokenService.idDoUsuario(jwt)`, respondendo `200` com `PerfilResponse` (depende de T047)

**Checkpoint**: todas as histórias funcionais e testadas.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T049 [P] Criar `README.md` na raiz com: pré-requisitos, criação do `.env` a partir do `.env.example` (incluindo `JWT_SECRET`), `docker compose up -d db`, `./mvnw -q verify`, como executar a aplicação e a lista dos 7 endpoints com link para `specs/001-conduit-usuarios-perfis/contracts/openapi.yaml`
- [X] T050 [P] Escrever Javadoc nas classes públicas de serviço (`UsuarioService`, `PerfilService`, `TokenService`) descrevendo contrato e exceções lançadas
- [X] T051 Revisar o código com o checklist de `tech.md` §15 e §27: sem imports não usados, sem `System.out`, nenhum log com senha/token/e-mail, nenhuma entidade exposta na API, `@Transactional` só em serviços
- [X] T052 Rodar `./mvnw -q verify` e corrigir falhas até passar (mostrar só erros e as últimas 50 linhas)
- [X] T053 Executar os 16 cenários de `specs/001-conduit-usuarios-perfis/quickstart.md` contra a aplicação rodando com o banco Docker local (após `docker compose ps` saudável) e relatar o resultado em uma ou duas frases

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências.
- **Foundational (Phase 2)**: depende do Setup; bloqueia todas as histórias.
- **US1 (Phase 3)**: depende da Phase 2.
- **US2 (Phase 4)**: depende da Phase 2 e de `UsuarioService`/`UsuarioController` de US1 (mesmos arquivos); os testes de US2 usam o cadastro para obter token.
- **US3 (Phase 5)**: depende da Phase 2; os testes de integração usam o cadastro de US1 para criar usuários.
- **US4 (Phase 6)**: depende de US3 (`PerfilService`, `PerfilController`, tabela `seguimento`).
- **Polish (Phase 7)**: depende de todas as histórias.

### Within Each User Story

- Testes escritos primeiro e falhando.
- DTOs/records e entidades → serviço → controller.
- Tarefas que alteram o mesmo arquivo (`UsuarioService.java`, `UsuarioController.java`, `PerfilService.java`, `PerfilController.java`) rodam em sequência.

### Parallel Opportunities

- Setup: T002, T003, T004 em paralelo após T001.
- Foundational: T006, T008, T009, T011, T012, T013 em paralelo; depois T007, T010, T014; depois T015; testes T016, T017 em paralelo.
- Cada história: todos os testes `[P]` juntos; DTOs `[P]` juntos.
- US2 e US3 podem andar em paralelo após US1, desde que as edições em `UsuarioService.java` (T033 e T041) não sejam simultâneas.

---

## Parallel Example: User Story 1

```text
# Testes de US1 juntos:
Task: "T019 UsuarioServiceTest em src/test/java/com/example/blog/usuario/service/UsuarioServiceTest.java"
Task: "T020 UsuarioControllerCadastroLoginTest em src/test/java/com/example/blog/usuario/web/"
Task: "T021 UsuarioApiIT em src/test/java/com/example/blog/usuario/UsuarioApiIT.java"

# DTOs de US1 juntos:
Task: "T022 CadastroRequest em src/main/java/com/example/blog/usuario/web/CadastroRequest.java"
Task: "T023 LoginRequest em src/main/java/com/example/blog/usuario/web/LoginRequest.java"
Task: "T024 UsuarioAutenticado e UsuarioResponse"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (Setup) → Phase 2 (Foundational).
2. Phase 3 (US1).
3. **Parar e validar**: `./mvnw -q verify` e cenários 1–5 do quickstart.

### Incremental Delivery

1. Setup + Foundational → base pronta.
2. US1 → validar (MVP: cadastro e login).
3. US2 → validar (cenários 6–9).
4. US3 → validar (cenários 10, 15, 16).
5. US4 → validar (cenários 11–14).
6. Polish → `verify` completo e quickstart inteiro.

---

## Notes

- `[P]` = arquivos diferentes, sem dependência pendente.
- Nunca editar uma migration já aplicada fora do banco local; correções vão em nova migration (`tech.md` §6.3).
- Não usar `docker compose down -v` sem confirmação explícita do usuário.
- Dados de teste sempre fictícios e criados pelo próprio teste.
