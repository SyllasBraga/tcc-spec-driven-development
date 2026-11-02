# Research: Recorte A da API Conduit

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-11-01

Cada item registra a decisão, o motivo e as alternativas avaliadas. Todos os "NEEDS CLARIFICATION" do Technical Context foram resolvidos aqui.

---

## R1. Emissão e validação do JWT

- **Decision**: Usar Spring Security com o suporte oficial de *resource server* (`spring-boot-starter-security` + `spring-boot-starter-oauth2-resource-server`). Tokens assinados com HMAC-SHA256 (`HS256`), emitidos por `NimbusJwtEncoder` e validados por `NimbusJwtDecoder.withSecretKey(...)`, com validadores de assinatura, expiração (`exp`) e emissor (`iss`). Claims: `sub` = id do usuário, `iss`, `iat`, `exp`. Validade padrão de 24 h.
- **Rationale**: `tech.md` §24 manda preferir o suporte oficial do Spring Security a implementações manuais e validar assinatura, expiração e emissor. HS256 com chave simétrica basta para um único serviço que emite e valida os próprios tokens. Usar o id (imutável) no `sub` mantém o token válido quando o usuário troca `username` ou `email` por `PUT /api/user`.
- **Alternatives considered**: biblioteca JJWT com filtro manual (rejeitada: implementação manual, contraria §24); RS256 com par de chaves (rejeitada: complexidade sem benefício, não há terceiros validando o token); `username` no `sub` (rejeitada: token quebraria após troca de nome).

## R2. Cabeçalho `Authorization: Token <jwt>`

- **Decision**: Registrar um `BearerTokenResolver` próprio que lê o cabeçalho `Authorization` e aceita somente o prefixo `Token `. Sem cabeçalho → retorna `null` (requisição anônima). Cabeçalho presente com outro esquema ou valor vazio → lança `OAuth2AuthenticationException` com `invalid_token`.
- **Rationale**: o contrato usa `Token`, não `Bearer`. O resolver é o ponto de extensão previsto pelo resource server. Lançar erro para esquema errado atende FR-012 (esquema diferente → 401 "is invalid").
- **Alternatives considered**: filtro customizado que reescreve o cabeçalho para `Bearer` (rejeitada: gambiarra frágil); aceitar `Token` e `Bearer` (rejeitada: amplia o contrato).

## R3. Autenticação obrigatória, opcional e respostas 401

- **Decision**: `SecurityFilterChain` *stateless*, CSRF desabilitado, sessão `STATELESS`.
  - Públicas: `POST /api/users`, `POST /api/users/login`, `GET /api/profiles/{username}`, `GET /actuator/health`, `GET /actuator/info`.
  - Autenticadas: `GET|PUT /api/user`, `POST|DELETE /api/profiles/{username}/follow` e qualquer outra rota (negação por padrão).
  - `AuthenticationEntryPoint` próprio escreve `401` no formato `{"errors":{"token":[...]}}`: `"is invalid"` quando a causa é `OAuth2AuthenticationException` (token presente e inválido), `"is missing"` caso contrário.
  - `AccessDeniedHandler` escreve `403` no mesmo formato (não esperado no recorte, mas mantém o formato único).
- **Rationale**: o `BearerTokenAuthenticationFilter` tenta autenticar sempre que há token, inclusive em rotas `permitAll`; com token inválido ele já chama o entry point → `GET /api/profiles/{username}` com token inválido retorna `401` (cenário US3-4), e sem token segue anônimo (autenticação opcional). Não há sessão nem cookies, então CSRF pode ser desabilitado (`tech.md` §24).
- **Alternatives considered**: tratar autenticação opcional no controller lendo o cabeçalho à mão (rejeitada: duplica a validação do token).
- **CORS**: não configurado. A API não tem front-end no escopo (`product.md`), e a configuração padrão do Spring Security recusa origens cruzadas. Se um cliente web surgir, CORS entra com origens explícitas.

## R4. Usuário do token que não existe mais

- **Decision**: o service carrega o usuário pelo `sub`. Se não existir, lança `UsuarioDoTokenInexistenteException` (pacote `shared`), que o tratador global converte em `401 {"errors":{"token":["is invalid"]}}`.
- **Rationale**: FR-012. Não há exclusão de usuário no recorte, mas o caso pode ocorrer se o banco for recriado com tokens ainda válidos.

## R5. Hash de senha

- **Decision**: `PasswordEncoder` via `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (BCrypt por padrão, com prefixo `{bcrypt}`), declarado como `@Bean`.
- **Rationale**: `tech.md` §14 e §24. O formato delegante permite trocar o algoritmo no futuro sem migrar dados.
- **Alternatives considered**: Argon2 (exige dependência extra do Bouncy Castle); BCrypt puro (sem prefixo, troca futura mais difícil).

## R6. Formato de erro e status

- **Decision**: um `@RestControllerAdvice` em `shared/error` converte tudo para `{"errors":{"<chave>":["<mensagem>"]}}` (record `ErrorResponse(Map<String, List<String>> errors)`). `ProblemDetail` **não** é usado, porque o contrato fixa outro formato (`tech.md` §21 permite isso).

| Origem | Status | Corpo |
|---|---|---|
| `MethodArgumentNotValidException` (Bean Validation) | 422 | uma chave por campo (`email`, `username`, `password`, `bio`, `image`); violação no próprio objeto `user` (ausente) → chave `body` |
| `HttpMessageNotReadableException` (corpo ausente ou JSON malformado) | 422 | `{"body":["can't be empty"]}` |
| `HttpMediaTypeNotSupportedException` | 422 | `{"body":["is invalid"]}` |
| `CampoJaUsadoException` no cadastro | 409 | `{"<campo>":["has already been taken"]}` (um ou dois campos) |
| `CampoJaUsadoException` na atualização | 422 | idem |
| `CredenciaisInvalidasException` | 401 | `{"credentials":["invalid"]}` |
| `UsuarioDoTokenInexistenteException` | 401 | `{"token":["is invalid"]}` |
| `PerfilNaoEncontradoException` | 404 | `{"profile":["not found"]}` |
| `SeguirASiMesmoException` | 422 | `{"profile":["cannot follow yourself"]}` |
| `AtualizacaoVaziaException` | 422 | `{"body":["can't be empty"]}` |
| `NoResourceFoundException` | 404 | `{"resource":["not found"]}` |
| `HttpRequestMethodNotSupportedException` | 405 | `{"method":["not allowed"]}` |
| `Exception` (fronteira) | 500 | `{"server":["internal error"]}`, registrado com `log.error` |

- **Implementação do mapeamento**: as exceções de domínio (`CampoJaUsadoException`, `CredenciaisInvalidasException`, `PerfilNaoEncontradoException` etc.) estendem `shared.error.ApiException`, que carrega o status HTTP e o mapa de erros. Assim o advice trata uma única classe base e o pacote `shared` não depende dos pacotes de funcionalidade (`structure.md`).
- **Rationale**: FR-029, FR-031 e o catálogo do contrato. A distinção 409 (cadastro) × 422 (atualização) segue a OpenAPI, conforme Assumptions da spec. A exceção de conflito carrega o contexto (cadastro ou atualização) para o advice escolher o status sem regra de negócio no controller.

## R7. `Content-Type: application/json; charset=utf-8`

- **Decision**: constante `MediaTypes.APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8"` em `shared/web`, usada em `produces` de todos os mapeamentos e definida explicitamente nas respostas do advice, do entry point e do access-denied handler.
- **Rationale**: o Spring MVC, por padrão, devolve `application/json` sem `charset`. Declarar `produces` é explícito e não depende de propriedades de codificação, cujos nomes mudaram entre versões do Spring Boot. Clientes HTTP tratam `;charset=UTF-8` e `; charset=utf-8` como equivalentes (parâmetros de mídia não diferenciam maiúsculas e o espaço é opcional, RFC 9110 §8.3.1).
- **Alternatives considered**: forçar a codificação pela propriedade do servidor (rejeitada: nome da propriedade sujeito a mudança no Boot 4 e não cobre o entry point do Security).

## R8. Atualização parcial e `null` em `bio`/`image`

- **Decision**: `UpdateUserData` é um record com `email`, `username`, `password`, `bio`, `image`. Em `bio` e `image`, `@JsonSetter(nulls = Nulls.AS_EMPTY)`: `null` explícito vira `""`; ausência continua `null`. Regra no service:
  - campo `null` → não alterar;
  - `bio`/`image` igual a `""` (ou só espaços) → gravar `NULL` e devolver `null` (FR-016);
  - todos os cinco campos `null` → `AtualizacaoVaziaException` (FR-017);
  - `email`, `username`, `password` enviados como `null` explícito são tratados como ausentes.
- **Rationale**: mantém DTOs como records (`tech.md` §5, §21) e distingue "ausente" de "nulo" com uma anotação, sem dependência extra. As anotações do Jackson 3 continuam no pacote `com.fasterxml.jackson.annotation` (só o *databind* mudou para `tools.jackson`).
- **Alternatives considered**: receber `JsonNode` e validar à mão (rejeitada: perde Bean Validation declarativa); `JsonNullable` (dependência extra); `Optional` em campos (proibido por `tech.md` §5).

## R9. Validação de entrada

- **Decision**: Jakarta Bean Validation nos records de entrada.
  - Cadastro e login: `@NotBlank(message = "can't be blank")` nos obrigatórios; `@Email(message = "is invalid")` no e-mail.
  - Atualização: `@Pattern(regexp = ".*\\S.*", message = "can't be blank")` em `email`, `username`, `password` (aceita `null`, recusa vazio e só espaços); `@Email(message = "is invalid")` no e-mail.
  - Envelope: `@NotNull @Valid` no componente `user`.
- **Normalização**: `email` → `trim` + minúsculas (`Locale.ROOT`); `username` → `trim`. A senha não é alterada.
- **Rationale**: FR-002, FR-003, FR-007, FR-018, FR-035.

## R10. Unicidade e concorrência

- **Decision**: o service verifica `existsByUsername` / `existsByEmail` antes de gravar (para devolver os dois campos em conflito, quando houver) e o banco garante com `uq_users_username` e `uq_users_email`. Uma `DataIntegrityViolationException` em uma dessas restrições, causada por corrida, também vira `CampoJaUsadoException`.
- **Rationale**: integridade no banco (`tech.md` §6.3, §22) e mensagem correta mesmo sob concorrência.

## R11. Seguir de forma idempotente

- **Decision**: `INSERT INTO follows (follower_id, followed_id) VALUES (?, ?) ON CONFLICT DO NOTHING` por `@Query(nativeQuery = true)` + `@Modifying`; deixar de seguir com `DELETE ... WHERE follower_id = ? AND followed_id = ?`. Verificação de "seguir a si mesmo" no service (422) e `CHECK` no banco.
- **Rationale**: SC-005 (sem duplicatas nem erros sob requisições repetidas ou concorrentes) sem ler antes de escrever.
- **Alternatives considered**: `existsById` + `save` (rejeitada: corrida gera violação de PK).

## R12. Testes

- **Decision**:
  - **Unitários (`*Test`, Surefire)**: services com Mockito (`UserServiceTest`, `ProfileServiceTest`), emissor de token com `Clock` fixo (`TokenServiceTest`).
  - **Fatia web (`*Test`)**: `@WebMvcTest` dos controllers com `@MockitoBean` nos services, validando status, JSON, `Content-Type` e mensagens de erro. Pacote no Boot 4: `org.springframework.boot.webmvc.test.autoconfigure` — **conferir na documentação 4.1.2 antes de usar**.
  - **Integração (`*IT`, Failsafe)**: `@SpringBootTest` + `MockMvc` (`@AutoConfigureMockMvc`) + `@Import(TestcontainersConfiguration.class)` com `postgres:18.6` e `@ServiceConnection`; cobre todos os cenários de aceitação da spec ponta a ponta, incluindo `MigrationsIT` (contexto sobe com `ddl-auto: validate` em banco vazio).
  - Isolamento: cada IT limpa `follows` e `users` em `@BeforeEach` via repository (dados próprios e fictícios).
- **Build**: adicionar `maven-failsafe-plugin` (versão gerenciada pelo parent) com os goals `integration-test` e `verify`, para `./mvnw verify` rodar os `*IT`.
- **Dependência de teste**: `spring-boot-starter-security-test` (fornece `spring-security-test`) — conferir o nome do starter no Boot 4.1.2.

## R13. Configuração e segredos

- **Decision**: record `JwtProperties(@NotBlank String secret, @NotBlank String issuer, @NotNull Duration ttl)` com `@ConfigurationProperties("app.jwt")` e `@Validated`. Em `application.yml`: `secret: ${JWT_SECRET}`, `issuer: conduit-blog`, `ttl: 24h`. O segredo é uma string Base64 de no mínimo 32 bytes (256 bits); a inicialização falha se for menor.
- `.env.example` ganha `JWT_SECRET=<base64_de_32_bytes_aleatorios>`. O `.env` local recebe um valor gerado com `secrets` do Python.
- `application-dev.yml` importa o `.env` com `spring.config.import: optional:file:.env[.properties]`, para rodar localmente sem exportar variáveis.
- Testes: `src/test/resources/application.yml` (ou propriedade no teste) define um `app.jwt.secret` fictício de teste.
- **Rationale**: `tech.md` §1, §20 (segredos fora de arquivos versionados; `@ConfigurationProperties` com record).

## R14. Desempenho (SC-004)

- **Decision**: nenhum cache. Índices: únicos em `username` e `email` (buscas de login e perfil); PK composta em `follows` (consulta de `following`) e índice em `followed_id` (FK). Custo do BCrypt padrão (10) ≈ 50–100 ms por login/cadastro, dentro da meta de 500 ms p95 com 50 requisições simultâneas.
- **Rationale**: escopo pequeno; consultas por chave única.
