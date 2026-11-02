# Research: Conduit — Recorte A (usuários, autenticação e perfis)

**Fase 0 do plano** · Data: 2026-11-01 · Spec: [spec.md](spec.md)

Não restaram itens `NEEDS CLARIFICATION` no contexto técnico: a stack está fixada no `pom.xml` e em `.claude/rules/tech.md`. Este documento registra as decisões técnicas que a especificação deixou em aberto.

---

## R1. Emissão e validação do token (JWT)

- **Decisão**: usar o suporte oficial do Spring Security — `spring-boot-starter-security` + `spring-boot-starter-oauth2-resource-server` (Nimbus). Tokens JWT assinados com **HS256** usando `NimbusJwtEncoder`/`NimbusJwtDecoder` com chave simétrica. Claims: `sub` = id do usuário, `iss` = `conduit`, `iat`, `exp` (iat + 24 h). O decodificador valida assinatura, expiração e emissor.
- **Racional**: `tech.md` §24 pede "o suporte oficial do Spring Security, resource server, a implementações manuais" e validação de assinatura, expiração e emissor. HS256 dispensa infraestrutura de chaves e basta para um único serviço.
- **Alternativas**: biblioteca `jjwt` com filtro manual (rejeitada: implementação manual, contrária a §24); RS256 com par de chaves (rejeitada: complexidade sem benefício para um único emissor/consumidor).

## R2. Esquema `Token` no cabeçalho `Authorization`

- **Decisão**: um `BearerTokenResolver` próprio (`shared.seguranca.TokenHeaderResolver`) que lê `Authorization` e aceita somente o prefixo `Token ` (seguido do token). Cabeçalho ausente → resolver devolve `null` (requisição anônima). Cabeçalho presente com outro esquema ou vazio → lança `InvalidBearerTokenException` (→ 401 `token: is invalid`).
- **Racional**: o contrato usa `Token <jwt>`, não `Bearer`. O resolver é o ponto de extensão oficial do resource server.
- **Alternativas**: filtro próprio de autenticação (rejeitada: reimplementa o que o resource server já faz).

## R3. Respostas 401 nos formatos do contrato

- **Decisão**: `AuthenticationEntryPoint` próprio, registrado tanto em `oauth2ResourceServer().authenticationEntryPoint(...)` quanto em `exceptionHandling()`. Se a exceção for `OAuth2AuthenticationException`/`InvalidBearerTokenException` → `{"errors":{"token":["is invalid"]}}`; se for ausência de autenticação (`InsufficientAuthenticationException`) → `{"errors":{"token":["is missing"]}}`. `AccessDeniedHandler` → 403 no mesmo formato (`{"errors":{"authorization":["forbidden"]}}`), embora não haja cenário de 403 no recorte.
- **Comportamento em rota de autenticação opcional** (`GET /api/profiles/{username}`): a rota é `permitAll`, mas o `BearerTokenAuthenticationFilter` tenta autenticar sempre que o resolver devolve um token; token inválido gera 401 antes de chegar ao controller. Sem cabeçalho, a requisição segue anônima. Isso atende ao caso de borda da spec.
- **Usuário do token inexistente**: o JWT pode ser válido e o usuário não existir mais (não há exclusão no recorte, mas o banco local pode ser recriado). A camada de serviço busca o usuário pelo `sub`; se não existir, lança `TokenInvalidoException` (shared), convertida em 401 `token: is invalid` pelo `@RestControllerAdvice`.

## R4. Senhas

- **Decisão**: `PasswordEncoder` via `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (BCrypt por padrão, com prefixo `{bcrypt}`), declarado como `@Bean` em `shared.seguranca`.
- **Racional**: `tech.md` §24 (BCrypt/Argon2 pelo `PasswordEncoder` do Spring); o delegating permite trocar de algoritmo no futuro sem migração.

## R5. Configuração do token

- **Decisão**: `@ConfigurationProperties(prefix = "conduit.token")` em um record `TokenProperties(String segredo, Duration validade, String emissor)` com `@Validated` (`@NotBlank`, `@Size(min = 32)` no segredo). `application.yml`: `segredo: ${JWT_SECRET}`, `validade: 24h`, `emissor: conduit`. `JWT_SECRET` entra no `.env.example` com valor fictício. Nos testes, o segredo é definido em `src/test/resources/application-test.yml` (valor fictício de teste), ativado com `@ActiveProfiles("test")`; um `application.yml` em `src/test/resources` não é usado porque esconderia o `application.yml` principal no classpath.
- **Racional**: `tech.md` §20 (`@ConfigurationProperties` com record e segredos vindos do ambiente). HS256 exige chave de pelo menos 256 bits.
- Um `Clock` (`Clock.systemUTC()`) é exposto como `@Bean` para tornar `iat`/`exp` testáveis.

## R6. Envelopes JSON (`{"user": ...}`, `{"profile": ...}`)

- **Decisão**: records explícitos de envelope: `UsuarioEnvelope(UsuarioResponse user)`, `PerfilEnvelope(PerfilResponse profile)`, `CadastroRequest(@Valid @NotNull NovoUsuario user)`, `LoginRequest(@Valid @NotNull Credenciais user)`. Sem `WRAP_ROOT_VALUE` global.
- **Racional**: explícito, sem configuração global do Jackson 3 que afetaria as respostas de erro.
- `bio` e `image` nulos devem aparecer como `null` (não omitidos): manter a inclusão padrão do Jackson (`ALWAYS`); não configurar `NON_NULL`.

## R7. Atualização parcial (`PUT /api/user`) com distinção entre ausente e `null`

- **Decisão**: o controller recebe `AtualizacaoRequest(JsonNode user)` (Jackson 3, `tools.jackson.databind.JsonNode`). Um mapeador na camada web (`AtualizacaoUsuarioMapper`) converte o nó em um record de comando `AtualizacaoUsuario` que guarda, para cada campo aceito, se ele foi enviado e o valor (tipo `CampoAtualizado<T>` — record `(boolean presente, T valor)` com fábricas `ausente()` e `de(valor)`). O mapeador produz os erros 422 (`can't be blank`, `is invalid`, `user: can't be blank` quando nenhum campo aceito vier).
- **Racional**: records comuns não distinguem "campo ausente" de "campo `null`", e `bio: null` precisa limpar o campo (FR-015) enquanto a ausência mantém o valor (FR-013). `Optional` em campos é vedado por `tech.md` §5.
- **Alternativas**: `JsonNullable` (biblioteca extra, sem necessidade); `Optional` em campos de record (proibido pelas regras); `Map<String,Object>` (perde tipagem).
- Campos desconhecidos dentro de `user` são ignorados. `bio`/`image` com tipo não textual → 422 `is invalid`.

## R8. Validação e formato de erro 422

- **Decisão**: Jakarta Bean Validation nos records de cadastro/login com mensagens do contrato: `@NotBlank(message = "can't be blank")`, `@Email(message = "is invalid")`. Um único `@RestControllerAdvice` (`shared.erro.ManipuladorDeErros`) converte:
  - `MethodArgumentNotValidException` → 422, chave = nome do campo sem o prefixo `user.` (ex.: `user.email` → `email`), mensagens agrupadas por campo (`LinkedHashMap<String, List<String>>`), sem duplicatas;
  - `HttpMessageNotReadableException` (corpo ausente/JSON inválido) e objeto `user` nulo → 422 `{"errors":{"body":["can't be empty"]}}`;
  - `ValidacaoException` (shared; lançada pelo mapeador do PUT e pelo serviço) → 422 com o mapa de erros;
  - `ConflitoException` (shared) → 409 com o mapa de erros;
  - `CredenciaisInvalidasException` → 401 `{"errors":{"email or password":["is invalid"]}}`;
  - `RecursoNaoEncontradoException` → 404 `{"errors":{"profile":["not found"]}}`;
  - `TokenInvalidoException` → 401 `token: is invalid`;
  - qualquer outra exceção → 500 `{"errors":{"server":["internal error"]}}`, registrada em log uma vez, sem detalhes ao cliente.
- **Racional**: o contrato fixa o formato `GenericErrorModel`, que prevalece sobre o `ProblemDetail` (`tech.md` §21 só usa `ProblemDetail` quando o contrato não fixa formato).
- `@NotBlank` cobre ausente, `null`, vazio e só espaços. `@Email` ignora valores vazios, então campo vazio gera só `can't be blank`.
- Ordem: validação (422) → unicidade (409 no cadastro / 422 no PUT) → persistência.
- Os textos das mensagens ficam em constantes (`shared.erro.MensagensErro`), não espalhados.

## R9. Unicidade de e-mail e nome de usuário

- **Decisão**:
  - e-mail armazenado como enviado (após `trim`), unicidade garantida por **índice único funcional** `uq_usuario_email` em `lower(email)`; consultas por `existsByEmailIgnoreCase` / `findByEmailIgnoreCase`;
  - username armazenado após `trim`, `UNIQUE` sensível a maiúsculas (`uq_usuario_username`).
  - O serviço verifica antes de gravar (para responder com todas as chaves em conflito) e, para corridas, traduz `DataIntegrityViolationException` pelo nome da restrição (`uq_usuario_email` → `email`, `uq_usuario_username` → `username`) em `ConflitoException` (cadastro) ou `ValidacaoException` (atualização). `saveAndFlush` garante que a violação aconteça dentro do método de serviço.
- **Racional**: integridade no banco (`tech.md` §6.3, §22) e regra de comparação da spec (e-mail case-insensitive, username case-sensitive).
- **Alternativa**: normalizar e-mail para minúsculas (rejeitada: altera o dado do usuário; o índice funcional preserva o valor original).

## R10. Relação de seguir

- **Decisão**: tabela `seguimento (seguidor_id, seguido_id, criado_em)` com PK composta, FKs para `usuario`, `CHECK (seguidor_id <> seguido_id)` e índice em `seguido_id` (o PK cobre `seguidor_id`). Entidade `Seguimento` com `@EmbeddedId SeguimentoId(seguidorId, seguidoId)` — mapeia ids, sem associações JPA (evita carregamentos desnecessários). Repositório com `existsById`, `deleteById`.
- `POST follow` idempotente: se já existe, não insere; corrida concorrente que viola o PK é ignorada (o estado final é "seguindo"). `DELETE follow`: `deleteById` se existir.
- Seguir a si mesmo: o serviço verifica antes (422 `profile: cannot follow yourself`); o `CHECK` no banco é a proteção final.

## R11. `Content-Type: application/json; charset=utf-8`

- **Decisão**: `server.servlet.encoding.charset: UTF-8` e `server.servlet.encoding.force: true` no `application.yml`, que faz o filtro de codificação definir o charset em todas as respostas, inclusive as escritas pelo `AuthenticationEntryPoint`. O entry point e o access denied handler escrevem o corpo com `MediaType.APPLICATION_JSON` e `UTF-8` explícitos. Os testes de integração verificam o tipo de mídia `application/json` e o parâmetro `charset` igual a `utf-8` (comparação sem diferenciar maiúsculas, como define a RFC 9110).
- **Alternativa**: `produces = "application/json;charset=UTF-8"` em cada mapeamento (rejeitada: não cobre respostas de erro de segurança e se repete).

## R12. Organização de pacotes e dependências entre funcionalidades

- **Decisão**: pacote raiz `com.example.blog` (classe existente `BlogApplication`, mantida). Funcionalidades:
  - `usuario` (`web`, `service`, `persistence`): cadastro, login, usuário atual, entidade `Usuario`;
  - `perfil` (`web`, `service`, `persistence`): perfil público e seguir/deixar de seguir, entidade `Seguimento`. `perfil.service` usa `usuario.persistence.UsuarioRepository` para localizar usuários (dependência entre funcionalidades no sentido perfil → usuario, sem ciclo);
  - `shared` (`seguranca`, `erro`): configuração de segurança, emissão de token, exceções base e o `@RestControllerAdvice`. Não depende de funcionalidades.
- O id do usuário autenticado chega aos controllers por `@AuthenticationPrincipal Jwt` → `Long.valueOf(jwt.getSubject())`, encapsulado em `shared.seguranca.UsuarioAutenticado` (método estático de extração ou resolvedor de argumento) para não repetir a conversão.

## R13. Testes

- **Decisão**:
  - **Unitários (`*Test`, Surefire)**: `UsuarioServiceTest`, `PerfilServiceTest` (Mockito), `AtualizacaoUsuarioMapperTest`, `TokenServiceTest` (com `Clock` fixo), `TokenHeaderResolverTest`.
  - **Fatia web (`*Test`)**: `@WebMvcTest` dos controllers com `@MockitoBean` dos serviços e importação da configuração de segurança, cobrindo status, JSON e erros 401/422.
  - **Integração (`*IT`, Failsafe)**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@Import(TestcontainersConfiguration.class)` (PostgreSQL 18.6 via `@ServiceConnection`), exercitando os 7 endpoints ponta a ponta com `RestTestClient`/`MockMvcTester`, incluindo o cabeçalho `Content-Type`; `MigracoesIT` aplica todas as migrations em banco vazio e valida o mapeamento (`ddl-auto: validate`). Isolamento: cada teste usa nomes/e-mails únicos ou limpa as tabelas antes de executar.
  - Adicionar `maven-failsafe-plugin` (versão gerenciada pelo parent) com os goals `integration-test` e `verify`, e `spring-boot-starter-security-test` (escopo `test`).
- **Racional**: `tech.md` §11, §26 e `structure.md` (unitários `Test`, integração `IT`; Testcontainers, nunca H2).
- Os pacotes de `@WebMvcTest` e utilitários de teste mudaram no Spring Boot 4 (módulos `spring-boot-webmvc-test`); conferir na documentação da 4.1.2 durante a implementação.

## R14. Desempenho

- **Decisão**: sem cache nem otimizações específicas. Índices: `uq_usuario_username`, `uq_usuario_email` (funcional) e `ix_seguimento_seguido_id`; cada requisição faz no máximo 3 consultas simples. BCrypt com custo padrão (10) mantém o login bem abaixo de 1 s.
- **Racional**: SC-005 (95% < 1 s com 50 simultâneas) é atendido com folga por consultas indexadas.
