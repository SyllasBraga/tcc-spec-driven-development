# Research: Conduit — Recorte A

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

Cada decisão segue o formato Decisão / Justificativa / Alternativas. Não restam itens `NEEDS CLARIFICATION`.

## R1. Emissão e validação de JWT

- **Decisão**: Usar o suporte oficial do Spring Security como *resource server* JWT (starter `spring-boot-starter-security-oauth2-resource-server` no Spring Boot 4). Os tokens são emitidos com `NimbusJwtEncoder` e validados com `NimbusJwtDecoder`, ambos com chave simétrica HMAC-SHA256. A chave vem da variável de ambiente `JWT_SECRET`, com no mínimo 256 bits, codificada em Base64. O decoder valida assinatura, expiração (`exp`) e emissor (`iss`). As claims são `sub` (id do usuário), `iss`, `iat` e `exp`.
- **Justificativa**: `tech.md` §24 pede o suporte oficial (*resource server*) em vez de implementação manual, com validação de assinatura, expiração e emissor e com a chave fora do código. A chave simétrica basta porque o mesmo serviço emite e valida o token.
- **Alternativas**: biblioteca `jjwt` com filtro manual (rejeitada: implementação manual); par RSA (rejeitado: complexidade sem benefício quando há um único serviço).
- **A conferir na implementação**: o nome exato do starter e os pacotes no Spring Boot 4.1.2. No Boot 4, os starters de OAuth2 ganharam o prefixo `security-`.

## R2. Esquema `Authorization: Token <jwt>`

- **Decisão**: Implementar um `BearerTokenResolver` próprio (`ConduitTokenResolver`) que aceita somente o prefixo `Token `. Se o cabeçalho existir em outro formato (por exemplo, `Bearer x`) ou vier vazio, o resolver lança `InvalidBearerTokenException`, o que gera `401`. Se o cabeçalho estiver ausente, o resolver retorna `null`: a requisição segue anônima, e as rotas protegidas respondem `401` com `token: ["is missing"]`.
- **Justificativa**: O contrato fixa o prefixo `Token`, e o resolver padrão só aceita `Bearer`.
- **Alternativas**: reescrever o cabeçalho em um filtro anterior (rejeitada: menos explícita).

## R3. Autenticação opcional em `GET /api/profiles/{username}`

- **Decisão**: A rota fica como `permitAll()`. Sem cabeçalho, a requisição é anônima e responde `following: false`. Com token inválido, o `BearerTokenAuthenticationFilter` recusa a requisição antes do controller e responde `401`, como prevê a OpenAPI (a rota lista `401`).
- **Justificativa**: É o comportamento padrão do filtro e combina com o caso de borda da especificação.

## R4. Respostas de erro (401, 403, 404, 409, 422)

- **Decisão**: Todos os erros saem no formato `{"errors": {"<chave>": ["<mensagem>"]}}`, gerado em dois pontos de borda:
  - **Erros de segurança**: um `AuthenticationEntryPoint` próprio responde `401` com `token: ["is missing"]` quando não há credencial e com `token: ["is invalid"]` quando o token é inválido. Um `AccessDeniedHandler` responde `403` com `{"errors": {"authorization": ["forbidden"]}}`, que não deve ocorrer neste recorte.
  - **Demais erros**: um `@RestControllerAdvice` único em `shared` trata:
    - `MethodArgumentNotValidException`, que vira `422` com uma chave por campo;
    - `HttpMessageNotReadableException` (JSON malformado ou sem raiz), que vira `422` com `body: ["is invalid"]`;
    - exceções de domínio de `shared.erro`: `RecursoNaoEncontradoException` (`404`; ex.: `PerfilNaoEncontradoException` → `profile: ["not found"]`), `CampoJaUtilizadoException` (`409`, `<campo>: ["has already been taken"]`), `NaoAutenticadoException` (`401`, `credentials: ["invalid"]` ou `token: ["is invalid"]`) e `ValidacaoException` (`422`);
    - `DataIntegrityViolationException` nas restrições únicas, como salvaguarda para corrida entre requisições, que vira `409`;
    - `Exception` genérica, que vira `500` com `{"errors": {"server": ["unexpected error"]}}`, sem detalhes internos.
- **Justificativa**: O contrato define o formato de erro, que prevalece sobre o `ProblemDetail` de `tech.md` §21. O tratamento fica centralizado, como exige `tech.md` §7.
- **Alternativas**: `ProblemDetail` (rejeitada: viola o contrato).

## R5. Validação de entrada

- **Decisão**: Os DTOs de entrada são records com Bean Validation. A raiz `user` leva `@NotNull @Valid`, e os campos obrigatórios levam `@NotBlank(message = "can't be blank")`. O e-mail leva `@Email(message = "is invalid")`. Foram adotados limites de 255 caracteres para e-mail e de 50 para nome de usuário (`@Size`, mensagem `"is too long"`), com os mesmos limites no banco. Antes de validar, e-mail e nome de usuário passam por `trim`.
- **Justificativa**: Atende FR-013, FR-014, FR-018 e FR-025 e respeita o código `422` do contrato. O Spring retornaria `400` por padrão, e o *advice* converte para `422`.
- **Nota**: Os limites de tamanho não constam do contrato e são proteções de borda (`tech.md` §14). A especificação não lista a mensagem de excesso de tamanho porque não há teste do contrato para isso.

## R6. Atualização parcial em `PUT /api/user` (ausente × `null`)

- **Decisão**: O controller recebe a raiz `user` como `ObjectNode` do Jackson 3 (`tools.jackson.databind.node`). Um mapeador da camada `web` converte esse nó em `AtualizacaoUsuario`, um record cujos campos são do tipo `Alteracao<T>`, uma interface `sealed` com duas implementações:
  - `Manter`, quando o campo está ausente;
  - `Definir(T valor)`, quando o campo está presente, inclusive com valor `null`.

  As regras ficam assim:
  - `bio` e `image` aceitam `null`;
  - `email`, `username` e `password` com `null` ou vazios geram `422`;
  - objeto sem nenhum campo aceito gera `422` com `body: ["at least one field is required"]`.
- **Justificativa**: Um record comum não distingue um campo ausente de um campo `null`, e FR-021 e FR-023 exigem essa distinção. `tech.md` proíbe `Optional` em campos. A interface `sealed` com `switch` exaustivo é o padrão recomendado em `tech.md` §5.
- **Alternativas**: `JsonNullable` (rejeitada: dependência extra); `Map<String,Object>` (rejeitada: sem tipos).

## R7. Hash de senha

- **Decisão**: Usar o `PasswordEncoder` do Spring Security, criado por `PasswordEncoderFactories.createDelegatingPasswordEncoder()`. O padrão atual é BCrypt, e o hash fica com o prefixo `{bcrypt}`. A coluna é `varchar(255)`.
- **Justificativa**: `tech.md` §14 e §24. O encoder delegante permite trocar o algoritmo no futuro sem migrar os hashes existentes.

## R8. Unicidade de e-mail e nome de usuário

- **Decisão**: O e-mail é normalizado para minúsculas na aplicação e fica sob a restrição `uq_usuario_email`. O nome de usuário é comparado exatamente e fica sob `uq_usuario_username`. O serviço verifica a existência antes de gravar, o que permite indicar o campo no `409`. A violação da restrição no banco é a segunda barreira contra requisições concorrentes.
- **Justificativa**: Segue a premissa da especificação (e-mail sem diferenciar maiúsculas) e mantém a integridade no banco (`tech.md` §22).
- **Alternativas**: índice único em `lower(email)` (rejeitada: normalizar na escrita é mais simples e torna consultas por igualdade diretas).

## R9. Relação de seguir

- **Decisão**: A tabela `seguimento` usa chave primária composta `(seguidor_id, seguido_id)` e índice `ix_seguimento_seguido_id`. O seguir é idempotente: se a relação já existe, não é inserida de novo. O deixar de seguir apaga pela chave, se ela existir. Seguir a si mesmo é permitido, conforme a premissa da especificação.
- **Justificativa**: Atende FR-031 e FR-032. A chave composta impede relações duplicadas.

## R10. `Content-Type: application/json; charset=utf-8`

- **Decisão**: Configurar `server.servlet.encoding.charset=UTF-8` e `server.servlet.encoding.force-response=true`, para que o cabeçalho inclua `charset=UTF-8`. As respostas de erro geradas pelos handlers de segurança definem `application/json;charset=UTF-8` explicitamente.
- **Justificativa**: O contrato exige o *charset* e o Spring o omite por padrão em `application/json`. A comparação de *charset* não diferencia maiúsculas de minúsculas.
- **A conferir na implementação**: a propriedade no Boot 4.1.2. Se ela não tiver efeito sobre `application/json`, a alternativa é registrar um conversor ou filtro que defina o *charset*.

## R11. Configuração própria

- **Decisão**: Criar `@ConfigurationProperties("conduit.jwt")`, um record validado com os campos `secret` (vem de `JWT_SECRET`), `issuer` (padrão `conduit`) e `ttl` (`Duration`, padrão `24h`). Adicionar `JWT_SECRET` ao `.env.example` com um valor fictício. Nos testes, o segredo é gerado aleatoriamente em tempo de execução (`@DynamicPropertySource` ou propriedade na configuração de teste) e nunca é versionado.
- **Justificativa**: `tech.md` §20 e as regras de segredos.

## R12. Estratégia de testes

- **Decisão**:
  - **Unitários (`*Test`, Surefire)**: `UsuarioService` e `PerfilService` com Mockito; mapeador de `AtualizacaoUsuario`; `ConduitTokenResolver`.
  - **Fatia web (`*Test`)**: `@WebMvcTest` dos controllers com `@MockitoBean`, cobrindo status, JSON e erros `422`.
  - **Integração (`*IT`, Failsafe)**: `@SpringBootTest` com Testcontainers `postgres:18.6` e `@ServiceConnection`. Aplica todas as migrations (`ddl-auto: validate`) e percorre os 7 endpoints com `MockMvc`/`RestTestClient`, incluindo os casos de `401`, `404`, `409` e `422`.
  - **Suíte Hurl ponta a ponta**: `src/test/hurl/recorte-a.hurl`, executada contra a aplicação em execução, com nomes de usuário únicos por execução (variável `run_id`).
- **Justificativa**: Segue `tech.md` §11, §26 e §8. O `pom.xml` atual não tem o Failsafe e precisa recebê-lo.

## R13. Dependências a adicionar ao `pom.xml`

- `spring-boot-starter-security`
- `spring-boot-starter-security-oauth2-resource-server` (inclui `spring-security-oauth2-jose` / Nimbus)
- `spring-boot-starter-security-test` (escopo `test`)
- `maven-failsafe-plugin`, sem versão, pois é gerenciado pelo parent

Nenhuma versão é declarada manualmente: o BOM do Spring Boot 4.1.2 gerencia todas.
