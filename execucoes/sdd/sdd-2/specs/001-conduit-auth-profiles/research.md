# Research: Recorte A da API Conduit

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-11-01

Todas as incertezas do Technical Context foram resolvidas abaixo. Cada item segue o formato Decisão / Justificativa / Alternativas.

## R1. Emissão e validação do JWT

- **Decisão**: Spring Security com o suporte oficial de *resource server* (`spring-boot-starter-oauth2-resource-server`), JWT assinado com HMAC-SHA256 (`NimbusJwtEncoder`/`NimbusJwtDecoder` com `SecretKeySpec`). Claims: `sub` = id do usuário, `iss` = emissor configurado, `iat`, `exp` = `iat` + 24 h. O decoder valida assinatura, expiração e emissor. Segredo, emissor e validade vêm de `@ConfigurationProperties` (`app.jwt.secret`, `app.jwt.issuer`, `app.jwt.ttl`) com `@Validated`; o segredo vem da variável de ambiente `JWT_SECRET` (mínimo de 32 bytes).
- **Justificativa**: `tech.md` §24 manda preferir o suporte oficial do resource server a implementações manuais e validar assinatura, expiração e emissor. HS256 com segredo simétrico basta, pois o próprio serviço emite e valida.
- **Alternativas**: biblioteca `jjwt` com filtro manual (rejeitada: implementação manual); RSA (rejeitada: exige gestão de par de chaves sem benefício num serviço único).

## R2. Esquema `Authorization: Token <jwt>`

- **Decisão**: `BearerTokenResolver` próprio que aceita só o prefixo `Token ` e devolve `null` em qualquer outro caso (cabeçalho ausente ou esquema `Bearer`). Nas rotas públicas `POST /api/users` e `POST /api/users/login`, o resolver ignora o cabeçalho, para que um token velho não impeça cadastro ou login.
- **Justificativa**: O contrato exige `Token`, não `Bearer`. Token ausente leva a requisição a seguir anônima: `401 token is missing` nas rotas protegidas e perfil anônimo em `GET /api/profiles/{username}` (spec, Edge Cases).
- **Alternativas**: filtro que reescreve o cabeçalho para `Bearer` (rejeitada: menos explícito).

## R3. Respostas 401 com `is missing` × `is invalid`

- **Decisão**: `AuthenticationEntryPoint` próprio em `shared.security`. Se a exceção for `OAuth2AuthenticationException`/`InvalidBearerTokenException` (token presente, mas inválido ou expirado), responde `{"errors":{"token":["is invalid"]}}`; caso contrário, `{"errors":{"token":["is missing"]}}`. Status `401`, JSON escrito com o `JsonMapper` (Jackson 3) do contexto.
- **Token de usuário inexistente (FR-011)**: o serviço carrega o usuário pelo `sub`; se não existir, lança `InvalidTokenException` (em `shared.error`), convertida pelo `@RestControllerAdvice` em `401 token is invalid`. Isso evita que `shared` dependa do pacote `user`.
- **Rotas com autenticação opcional**: `GET /api/profiles/{username}` é `permitAll`, mas um token presente e inválido ainda passa pelo filtro do resource server e falha com `401`, como pede FR-011.
- **Alternativas**: conversor de JWT que consulta o repositório de usuários (rejeitada: `shared` passaria a depender de `user`).

## R4. Senhas

- **Decisão**: `BCryptPasswordEncoder` (custo padrão 10) exposto como bean `PasswordEncoder`.
- **Justificativa**: `tech.md` §14 e §24. BCrypt já embute o sal.
- **Alternativas**: Argon2 (exige BouncyCastle; dependência extra sem necessidade).

## R5. Formato de erro e mapeamento para 422/409/404/401

- **Decisão**: um único `@RestControllerAdvice` em `shared.error`, que devolve `ErrorResponse(Map<String, List<String>> errors)`:
  - `MethodArgumentNotValidException` → `422`. A chave é o último segmento do caminho do campo (`user.email` → `email`). Violação no próprio objeto raiz `user` (ausente) → chave `body`, mensagem `can't be empty`.
  - `HttpMessageNotReadableException` (corpo ausente ou JSON malformado) → `422 {"errors":{"body":["can't be empty"]}}`.
  - `ValidationException` de domínio (ex.: `PUT` vazio, seguir a si mesmo, campo em branco no `PUT`) → `422`.
  - `ConflictException` (com uma ou mais chaves) → `409 has already been taken`.
  - `NotFoundException` → `404 {"errors":{"profile":["not found"]}}`.
  - `InvalidCredentialsException` → `401 {"errors":{"credentials":["invalid"]}}`.
  - `InvalidTokenException` → `401 {"errors":{"token":["is invalid"]}}`.
  - `DataIntegrityViolationException` em restrição de unicidade (corrida entre duas requisições) → `409`, com a chave deduzida do nome da restrição (`uq_users_username` → `username`, `uq_users_email` → `email`).
  - Qualquer outra exceção → `500` genérico, registrado em log uma vez, sem detalhes ao cliente.
- **Mensagens Bean Validation**: `@NotBlank(message = "can't be blank")`, `@Email(message = "is invalid")`.
- **Justificativa**: `tech.md` §7 e §21 (tratamento único na borda). O formato `ProblemDetail` não é usado porque o contrato fixa `{"errors":{...}}`, e o contrato prevalece.

## R6. `Content-Type: application/json; charset=utf-8`

- **Decisão**: `server.servlet.encoding.charset=UTF-8` e `server.servlet.encoding.force-response=true`. O `CharacterEncodingFilter` define a codificação da resposta antes de qualquer escrita, e o Tomcat acrescenta `;charset=UTF-8` ao `Content-Type` `application/json`, inclusive nas respostas do `AuthenticationEntryPoint`. Um teste de integração confere o cabeçalho (comparação sem diferenciar maiúsculas de `utf-8`).
- **Alternativas**: `produces = "application/json;charset=UTF-8"` em cada mapeamento (rejeitada: não cobre erros de segurança e espalha configuração).

## R7. `PUT /api/user`: campo ausente × `null`

- **Decisão**: o controller recebe `UpdateUserRequest(JsonNode user)` (Jackson 3, `tools.jackson.databind.JsonNode`) e o converte em um record de serviço `UserUpdate`, que guarda cada campo e se ele foi enviado (`has(...)`). `bio`/`image` enviados como `null` limpam o valor; ausentes ficam inalterados. `email`, `username` e `password` enviados como `null` ou em branco → `422 can't be blank`. Nenhum campo aceito presente → `422 {"errors":{"user":["can't be empty"]}}`. Campos desconhecidos são ignorados.
- **Justificativa**: records com `String` não distinguem ausente de `null`; `Optional` em campos/parâmetros é proibido por `tech.md` §5.
- **Alternativas**: `JsonNullable` (dependência extra); `Map<String,Object>` (perde tipagem).

## R8. Unicidade de e-mail sem diferenciar maiúsculas

- **Decisão**: o e-mail é guardado como informado (sem espaços nas pontas). Índice único `uq_users_email` sobre `lower(email)`. Consultas usam `findByEmailIgnoreCase` / `existsByEmailIgnoreCase`. Nome de usuário: restrição `uq_users_username` sensível a maiúsculas.
- **Alternativas**: tipo `citext` (exige extensão; `tech.md` §8 pede autorização para instalar extensões); normalizar para minúsculas (altera o dado do usuário).

## R9. Seguir de forma idempotente

- **Decisão**: tabela `follows` com chave primária composta `(follower_id, followed_id)` e `CHECK (follower_id <> followed_id)`. O serviço verifica a existência antes de inserir; se uma corrida violar a PK, a violação é tratada como sucesso. `DELETE` remove se existir.
- **Alternativas**: id próprio + unique (desnecessário).

## R10. Dependências novas (Spring Boot 4.1.2)

- **Decisão**: adicionar sem versão (gerenciadas pelo parent):
  - `spring-boot-starter-security`
  - `spring-boot-starter-oauth2-resource-server` (traz `spring-security-oauth2-jose` com Nimbus para codificar e decodificar)
  - `spring-boot-starter-security-test` (escopo `test`; traz `spring-security-test`)
  - plugin `maven-failsafe-plugin` (versão gerenciada pelo parent) com os goals `integration-test` e `verify`, para rodar `*IT`.
- **Verificação**: confirmar na documentação do Spring Boot 4.1 os nomes dos starters de teste modularizados e o pacote de `@WebMvcTest` (`org.springframework.boot.webmvc.test.autoconfigure`) antes de escrever os testes.

## R11. Estratégia de testes

- **Decisão**:
  - Unitários (`*Test`, Surefire, Mockito): `UserService`, `ProfileService`, mapeamento de `UserUpdate`, `TokenService`.
  - Fatia web (`*Test`, `@WebMvcTest` + `@MockitoBean` + `@Import` da configuração de segurança): formato JSON, `422`/`401` e cabeçalho `Authorization`.
  - Integração (`*IT`, `@SpringBootTest` + `MockMvc` + Testcontainers `postgres:18.6` via `TestcontainersConfiguration` existente): fluxos ponta a ponta de cada história, `Content-Type` e um teste de migrations em banco vazio com `ddl-auto: validate`.
  - Segredo JWT de teste fictício em `src/test/resources/application.yml`.
- **Justificativa**: `tech.md` §11, §26 e Banco §8.

## R12. Desempenho (SC-005)

- **Decisão**: sem cache nem ajustes especiais. As consultas usam índice único (`username`, `lower(email)`) ou a PK de `follows`. O custo dominante é o BCrypt no cadastro e no login (~50–100 ms), dentro de 500 ms.
