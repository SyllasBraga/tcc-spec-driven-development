# Research: Conduit — Recorte A

Decisões técnicas da Fase 0. Cada item resolve uma incerteza do Technical Context do [plan.md](plan.md).

## R1. Emissão e validação do JWT

- **Decision**: Usar o suporte oficial do Spring Security como *resource server* (`spring-boot-starter-security-oauth2-resource-server`, versão gerenciada pelo Boot 4.1.2). Tokens HS256 assinados com `NimbusJwtEncoder`/`NimbusJwtDecoder` a partir de uma chave simétrica lida de variável de ambiente (`JWT_SECRET`, mínimo de 32 bytes). Claims: `sub` = id do usuário, `iss` = `conduit`, `iat`, `exp` (24 h). O decodificador valida assinatura, expiração e emissor.
- **Rationale**: `tech.md` §24 manda preferir o suporte oficial a implementações manuais e validar assinatura, expiração e emissor. HS256 com segredo único é o mínimo suficiente para um único serviço.
- **Alternatives considered**: biblioteca JJWT com filtro próprio (rejeitada: implementação manual); RSA com par de chaves (rejeitada: complexidade sem benefício em serviço único).
- **Verificar na implementação**: o nome exato do starter no Boot 4 (os starters de segurança foram renomeados com o prefixo `spring-boot-starter-security-`). Se o nome divergir, usar o que a documentação 4.1.x indicar.

## R2. Cabeçalho `Authorization: Token <jwt>`

- **Decision**: Registrar um `BearerTokenResolver` próprio que aceita somente o prefixo `Token ` e devolve `null` quando o cabeçalho está ausente. Cabeçalho presente com outro esquema ou vazio → token inválido (`401`).
- **Rationale**: O `DefaultBearerTokenResolver` só entende `Bearer`. O contrato exige `Token`.
- **Alternatives considered**: filtro que reescreve o cabeçalho para `Bearer` (rejeitada: menos explícito).

## R3. Autenticação opcional em `GET /api/profiles/{username}`

- **Decision**: Rota `permitAll`; o filtro de bearer token continua ativo, então token inválido gera `401` (comportamento padrão do resource server) e token ausente segue como anônimo.
- **Rationale**: Atende FR-005, FR-026 e FR-027 sem código extra.

## R4. Respostas 401 no formato Conduit

- **Decision**: `AuthenticationEntryPoint` próprio no `SecurityFilterChain` que escreve `{"errors":{"token":["is missing"]}}` quando não há token e `{"errors":{"token":["is invalid"]}}` quando a autenticação falha. O mesmo vale para o `AuthenticationEntryPoint` do resource server.
- **Rationale**: O formato de erro do contrato é fixo (FR-003, FR-004); `ProblemDetail` não se aplica porque o contrato define o formato.

## R5. Tratamento de erros e validação

- **Decision**: Um único `@RestControllerAdvice` em `shared.erro` converte:
  - `MethodArgumentNotValidException` → `422`, agrupando mensagens por campo (`can't be blank`, `is invalid`);
  - `HttpMessageNotReadableException` (JSON malformado) → `422` com `{"errors":{"body":["is invalid"]}}`;
  - exceções de domínio: `RecursoNaoEncontradoException` → `404`, `ConflitoException` → `409`, `ValidacaoException` → `422`, `CredenciaisInvalidasException` → `401`.
  - qualquer outra → `500` com mensagem genérica, sem detalhes internos.
- Mensagens de Bean Validation definidas nas anotações (`@NotBlank(message = "can't be blank")`, `@Email(message = "is invalid")`).
- **Rationale**: `tech.md` §7 e §21: conversão em um único ponto, sem stack traces.

## R6. `PUT /api/user`: distinguir campo ausente de campo `null`

- **Decision**: O controller recebe o corpo como `JsonNode` (Jackson 3, `tools.jackson.databind`) e um mapeador da camada `web` constrói um record `AlteracaoUsuario` em que cada campo indica presença e valor. Regras de FR-019 a FR-023 aplicadas no mapeador (formato) e no serviço (unicidade).
- **Rationale**: Records comuns não distinguem ausência de `null`; FR-023 exige que `null` apague `bio`/`image` enquanto ausência mantém o valor.
- **Alternatives considered**: `Optional<String>` em componentes do record (rejeitada: `tech.md` §5 proíbe `Optional` em campos); `JsonNullable` (rejeitada: dependência extra).

## R7. `Content-Type: application/json; charset=utf-8`

- **Decision**: `server.servlet.encoding.charset: UTF-8` e `server.servlet.encoding.force-response: true`, que fazem o contêiner acrescentar `;charset=UTF-8` ao `Content-Type`. Coberto por teste de integração que verifica o cabeçalho em sucesso e em erro (incluindo 401 do filtro de segurança).
- **Rationale**: Uma única configuração global, sem `produces` em cada rota.
- **Alternatives considered**: `produces = "application/json;charset=UTF-8"` em cada mapeamento (rejeitada: não cobre respostas do filtro de segurança).

## R8. Senhas

- **Decision**: `PasswordEncoder` BCrypt (`BCryptPasswordEncoder`) declarado como bean em `shared.seguranca`.
- **Rationale**: `tech.md` §14 e §24.

## R9. Unicidade de e-mail sem diferenciar maiúsculas

- **Decision**: Guardar o e-mail como informado e criar índice único em `lower(email)`; buscas por e-mail usam `lower(...)`. Nome de usuário com restrição `UNIQUE` simples (diferencia maiúsculas). A aplicação verifica antes de salvar (para devolver a mensagem certa) e o banco garante a integridade em corrida; violação de restrição capturada no serviço vira `409`/`422` conforme a rota.
- **Rationale**: Integridade no banco (`tech.md` §22) e resposta com o e-mail exatamente como enviado.
- **Alternatives considered**: tipo `citext` (rejeitada: exige extensão); normalizar para minúsculas (rejeitada: altera o valor devolvido ao cliente).

## R10. Token devolvido em `GET`/`PUT /api/user`

- **Decision**: Devolver o mesmo token recebido na requisição. Como o `sub` é o id do usuário, ele continua válido após troca de e-mail ou nome.
- **Rationale**: Mais simples; evita emitir tokens a cada leitura.

## R11. Testes

- **Decision**:
  - Unitários (`*Test`, Surefire): serviços com Mockito e um `Clock` fixo; mapeador de `PUT`.
  - Fatia web (`*Test`, `@WebMvcTest` + `@MockitoBean`): status, formato JSON e erros de cada controller.
  - Integração (`*IT`, Failsafe): `@SpringBootTest(webEnvironment = RANDOM_PORT)` com o `TestcontainersConfiguration` existente (`postgres:18.6`, `@ServiceConnection`), aplicando todas as migrations e validando o mapeamento JPA, e fluxos ponta a ponta via HTTP real (inclui `Content-Type` e 401 do filtro).
  - Adicionar `maven-failsafe-plugin` (versão gerenciada pelo parent) ao `pom.xml`.
- **Rationale**: `tech.md` §11, §26 e seção de migrations §8.
- **Segredo nos testes**: `JWT_SECRET` dos testes é gerado aleatoriamente em tempo de execução por `@DynamicPropertySource`/propriedade de teste, sem valor fixo versionado.

## R12. Configuração própria

- **Decision**: `@ConfigurationProperties("conduit.jwt")` com record validado (`segredo`, `validade` = `PT24H`, `emissor` = `conduit`). `segredo` vem de `${JWT_SECRET}`; `JWT_SECRET` entra no `.env.example` com valor fictício.
- **Rationale**: `tech.md` §20.
