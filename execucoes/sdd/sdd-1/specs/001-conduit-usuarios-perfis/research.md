# Research: Recorte A da API Conduit

Fonte dos requisitos: [spec.md](spec.md). Regras do projeto: `CLAUDE.md` e `.claude/rules/`.
A constituição (`.specify/memory/constitution.md`) ainda é o modelo vazio. As regras de `.claude/rules/` fazem o papel dela.

## R1. Autenticação JWT

- **Decision**: Spring Security com OAuth2 Resource Server (JWT) e `NimbusJwtEncoder`/`NimbusJwtDecoder` com chave simétrica HMAC-SHA256. Starters: `spring-boot-starter-security` e `spring-boot-starter-security-oauth2-resource-server`; testes com `spring-boot-starter-security-test`. Sem versão no `pom.xml` (gerenciadas pelo parent 4.1.2). Confirmar os nomes dos starters na documentação do Spring Boot 4.1 ao editar o `pom.xml`.
- **Rationale**: `tech.md` §24 pede o suporte oficial (resource server) em vez de validação manual. Ele já valida assinatura e expiração. HS256 dispensa infraestrutura de chaves para uma API única.
- **Alternatives considered**: biblioteca `jjwt` com filtro próprio (rejeitado: implementação manual de segurança); RSA (rejeitado: emissor e validador são o mesmo serviço, sem ganho).

## R2. Esquema `Token` no cabeçalho

- **Decision**: `BearerTokenResolver` próprio que aceita somente `Authorization: Token <jwt>`. Outro esquema ou cabeçalho ausente resulta em "sem token".
- **Rationale**: o contrato usa `Token`, não `Bearer`. O resolvedor padrão só reconhece `Bearer`.
- **Alternatives considered**: filtro que reescreve o cabeçalho para `Bearer` (rejeitado: indireto e frágil).

## R3. Claims, emissor e expiração

- **Decision**: `sub` = id do usuário; `iss` = valor configurado; `iat` e `exp`, com validade padrão de 24 h. Propriedades em `@ConfigurationProperties` record validado (`conduit.jwt.secret`, `conduit.jwt.issuer`, `conduit.jwt.validade`). O segredo vem da variável de ambiente `JWT_SECRET`, com no mínimo 32 bytes. O decoder valida assinatura, expiração e emissor. O relógio vem de um bean `Clock`.
- **Rationale**: `tech.md` §24 exige validar assinatura, expiração e emissor, com a chave fora do código. O id não muda quando o username muda, então o token continua válido após `PUT /api/user`.
- **Alternatives considered**: `sub` = username (rejeitado: quebra o token ao trocar username).

## R4. Rotas públicas, opcionais e protegidas

- **Decision**: `SecurityFilterChain` sem estado (`STATELESS`), CSRF desabilitado, sem CORS (não há cliente web no escopo). Públicas: `POST /api/users`, `POST /api/users/login`, `GET /api/profiles/*`, `GET /actuator/health`, `GET /actuator/info`. Demais rotas: autenticadas.
- Em `GET /api/profiles/{username}`, sem token a requisição segue anônima. Com token inválido, o filtro do resource server já responde `401`, o que atende o edge case da spec.
- **Rationale**: `tech.md` §24 (rotas explícitas, autenticação por padrão). O contrato lista `401` também para o GET de perfil.

## R5. Respostas 401 no formato do contrato

- **Decision**: `AuthenticationEntryPoint` próprio no `oauth2ResourceServer` e no `exceptionHandling`. Sem token: `{"errors":{"token":["is missing"]}}`. Token inválido (`InvalidBearerTokenException` ou usuário do `sub` inexistente): `{"errors":{"token":["is invalid"]}}`. `AccessDeniedHandler` análogo para `403`.
- **Rationale**: o entry point padrão do Spring devolve corpo vazio e cabeçalho `WWW-Authenticate`. O contrato exige `GenericErrorModel`.

## R6. Formato de erro e tratamento centralizado

- **Decision**: um `@RestControllerAdvice` em `shared/web` converte as exceções no corpo `{"errors": {campo: [mensagens]}}` (record `ErroResponse(Map<String, List<String>> errors)`):
  - `MethodArgumentNotValidException` → `422`, um item por campo.
  - `HttpMessageNotReadableException` → `422`, chave `body`.
  - `ValidacaoException` (domínio, com campo e mensagem) → `422`.
  - `ConflitoException` → `409`.
  - `CredenciaisInvalidasException` → `401`, chave `credentials`.
  - `RecursoNaoEncontradoException` → `404`, chave do recurso (`profile`).
  - `Exception` → `500`, chave `server`, mensagem genérica, com log `ERROR`.
- **Rationale**: o contrato fixa o formato de erro, então ele prevalece sobre o `ProblemDetail` sugerido em `tech.md` §21. Ponto único de conversão (§7).
- **Alternatives considered**: `ProblemDetail` (rejeitado: viola o contrato).

## R7. Mensagens de validação

- **Decision**: mensagens fixas no estilo RealWorld nas anotações: `@NotBlank(message = "can't be blank")`, `@Email(message = "is invalid")`. Duplicidade: `"has already been taken"`.
- **Rationale**: os exemplos do contrato usam esse texto; mensagens estáveis facilitam testes de contrato.

## R8. `PUT /api/user`: campo ausente versus `null`

- **Decision**: o DTO de entrada recebe `user` como `tools.jackson.databind.node.ObjectNode` (Jackson 3). Um mapeador na camada `web` converte para o record de comando `AtualizacaoUsuario`, que guarda, para cada campo, se ele foi informado e o valor. Validação: nenhum campo aceito → `422` (`user`); `email`/`username`/`password` informados em branco ou nulos → `422`; `email` inválido → `422`. Campos desconhecidos são ignorados.
- **Rationale**: records com `String` não distinguem ausente de `null`, e a spec exige que `bio: null` limpe o campo enquanto a ausência mantém o valor. `tech.md` §5 proíbe `Optional` em campos e parâmetros.
- **Alternatives considered**: `Optional<String>` nos componentes do record (rejeitado por §5); `JsonNullable` do openapi-tools (rejeitado: dependência extra).

## R9. Duplicidade e concorrência

- **Decision**: o serviço verifica duplicidade antes de gravar (mensagem amigável). As restrições `UNIQUE` do banco garantem a integridade; `DataIntegrityViolationException` na gravação é convertida no mesmo erro de duplicidade (`409` no cadastro, `422` no update), identificando o campo pelo nome da restrição (`uq_usuario_email`, `uq_usuario_username`).
- **Rationale**: `tech.md` exige integridade no banco. A checagem prévia sozinha falha sob concorrência.

## R10. Normalização de e-mail e username

- **Decision**: e-mail com `trim` e minúsculas (`Locale.ROOT`) antes de validar e gravar; a coluna guarda o valor normalizado com `UNIQUE`. Username só com `trim`, mantendo a caixa.
- **Rationale**: FR-006 e premissas da spec. Guardar o valor normalizado evita índice funcional.

## R11. Hash de senha

- **Decision**: `PasswordEncoder` via `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (BCrypt por padrão, com prefixo `{bcrypt}`).
- **Rationale**: `tech.md` §14 e §24. O formato delegado permite trocar o algoritmo depois.

## R12. `Content-Type: application/json; charset=utf-8`

- **Decision**: `spring.servlet.encoding.charset: UTF-8` e `spring.servlet.encoding.force-response: true` (prefixo do Boot 4); as respostas de erro do `TratadorGlobalDeErros` declaram o charset explicitamente. O Tomcat passa a enviar `application/json;charset=UTF-8`. O teste de integração confere o tipo de mídia e o charset sem diferenciar caixa nem espaço (parâmetros MIME são equivalentes).
- **Rationale**: FR-028 e a descrição-base. Se a forma exata `; charset=utf-8` for exigida literalmente por algum cliente, um filtro pode reescrever o cabeçalho; não é necessário agora.

## R13. Serialização JSON

- **Decision**: Jackson 3 padrão do Spring Boot 4. `bio` e `image` nulos são serializados como `null` (não omitidos), conforme `required` no schema. Envelopes `{"user": ...}` e `{"profile": ...}` por records de resposta (`UsuarioResponse(UsuarioDados user)`, `PerfilResponse(PerfilDados profile)`).
- **Rationale**: o schema OpenAPI marca `bio` e `image` como obrigatórios e anuláveis.

## R14. Seguir: idempotência e auto-seguimento

- **Decision**: `POST follow` grava só se a relação não existir; `DELETE follow` remove se existir. Ambos respondem `200`. Seguir a si mesmo → `422`; o banco também impede (`ck_seguidor_distintos`).
- **Rationale**: spec (edge cases, FR-024 a FR-027). `422` está entre os códigos listados no contrato para essas rotas.

## R15. Organização de pacotes

- **Decision**: pacote base `com.example.blog` (do `pom.xml`).
  - `usuario` (`web`, `service`, `persistence`): cadastro, login, usuário atual.
  - `perfil` (`web`, `service`, `persistence`): perfil público e seguimento. `perfil.service` usa `usuario.service` para localizar usuários; a entidade de seguimento guarda só os ids, sem relação JPA com a entidade de outro pacote.
  - `shared` (`security`, `web`, `config`): segurança/JWT, tratamento de erros, `Clock`. Não depende de pacotes de funcionalidade. A verificação "usuário do `sub` existe" é feita no serviço de usuário, que lança a exceção traduzida pelo tratamento comum.
  - A classe existente `BlogApplication` permanece no pacote raiz.
- **Rationale**: `structure.md` (um pacote por funcionalidade, sentido `web` → `service` → `persistence`, `shared` independente).

## R16. Estratégia de testes

- **Decision**:
  - Unitários (`*Test`, Surefire): serviços de usuário e perfil com Mockito; mapeador do `PUT` (ausente vs `null`); serviço de token com `Clock` fixo.
  - Fatia (`*Test`): `@WebMvcTest` dos controllers com `@MockitoBean` dos serviços e a configuração de segurança importada, validando status e JSON.
  - Integração (`*IT`, Failsafe): `@SpringBootTest` + Testcontainers `postgres:18.6` com `@ServiceConnection` (configuração existente `TestcontainersConfiguration`), cobrindo os cenários ponta a ponta da spec e um teste que aplica todas as migrations em banco vazio com `ddl-auto: validate`.
  - Adicionar `maven-failsafe-plugin` ao `pom.xml` (versão gerenciada pelo parent).
- **Rationale**: `tech.md` §11, §26 e seção 8 de migrations.

## R17. Migrations

- **Decision**: `V1__criar_tabela_usuario.sql` e `V2__criar_tabela_seguidor.sql` em `src/main/resources/db/migration`. Sem seed de desenvolvimento neste recorte.
- **Rationale**: uma mudança lógica por migration (`tech.md` §6.3). Pasta de migrations ainda vazia, então começa em `V1`.

## R18. Escopo de desempenho

- **Decision**: sem metas numéricas; a spec não define. Índices nas colunas de busca (`email`, `username` via `UNIQUE`; `seguido_id` em `seguidor`).
- **Rationale**: evitar requisitos inventados (`descricao-base.md`: não acrescentar escopo).
