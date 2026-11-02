# Feature Specification: Recorte A da API Conduit — usuários, autenticação e perfis

**Feature Branch**: `001-conduit-usuarios-perfis`

**Created**: 2026-10-31

**Status**: Draft

**Input**: `descricao-base.md` (Recorte A da API Conduit/RealWorld). O contrato da API (rotas, JSON, status e formato de erro) dessa descrição é obrigatório; em divergência entre prosa e OpenAPI, vale a OpenAPI.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastro e login com JWT (Priority: P1)

Um visitante cria uma conta com `username`, `email` e `password` e recebe um token JWT. Depois, entra com `email` e `password` e recebe um novo token.

**Why this priority**: Sem conta e token, nenhum outro recurso autenticado funciona.

**Independent Test**: `POST /api/users` seguido de `POST /api/users/login` com as mesmas credenciais; ambos devolvem `{"user": {...}}` com `token`.

**Acceptance Scenarios**:

1. **Given** e-mail e username livres, **When** `POST /api/users` com os três campos, **Then** status `201` e corpo User com `bio` e `image` nulos e `token` preenchido.
2. **Given** usuário cadastrado, **When** `POST /api/users/login` com credenciais corretas, **Then** status `200` e corpo User.
3. **Given** usuário cadastrado, **When** login com senha errada, **Then** status `401` e corpo de erro `{"errors": {...}}`.
4. **Given** username já usado, **When** novo cadastro com esse username, **Then** status `409` e `{"errors": {"username": ["has already been taken"]}}`.
5. **Given** corpo sem `email`, **When** cadastro, **Then** status `422` e `{"errors": {"email": ["can't be blank"]}}`.

---

### User Story 2 - Consultar e atualizar o usuário autenticado (Priority: P2)

O usuário autenticado consulta seus dados e altera `email`, `username`, `password`, `bio` ou `image`.

**Why this priority**: Completa o ciclo de conta; depende só da US1.

**Independent Test**: com token da US1, `GET /api/user` e `PUT /api/user` alterando `bio`; nova consulta reflete a mudança.

**Acceptance Scenarios**:

1. **Given** token válido, **When** `GET /api/user`, **Then** status `200` e corpo User do dono do token.
2. **Given** token válido, **When** `PUT /api/user` com `{"user": {"bio": "..."}}`, **Then** status `200` e User com `bio` atualizada e demais campos preservados.
3. **Given** troca de senha via `PUT /api/user`, **When** login com a nova senha, **Then** status `200`; com a senha antiga, `401`.
4. **Given** requisição sem cabeçalho `Authorization`, **When** `GET /api/user`, **Then** status `401`.

---

### User Story 3 - Perfis públicos e seguir/deixar de seguir (Priority: P3)

Qualquer pessoa consulta o perfil público de um usuário. Um usuário autenticado segue e deixa de seguir outros usuários, e o campo `following` reflete essa relação.

**Why this priority**: Funcionalidade social; depende das US1 e US2 apenas para ter usuários e tokens.

**Independent Test**: com dois usuários, A segue B (`POST /api/profiles/B/follow`), `GET /api/profiles/B` com token de A mostra `following: true`; após `DELETE`, `following: false`.

**Acceptance Scenarios**:

1. **Given** usuário B existe, **When** `GET /api/profiles/B` sem token, **Then** status `200` e Profile com `following: false`.
2. **Given** A autenticado, **When** `POST /api/profiles/B/follow`, **Then** status `200` e Profile de B com `following: true`.
3. **Given** A segue B, **When** `DELETE /api/profiles/B/follow`, **Then** status `200` e `following: false`.
4. **Given** username inexistente, **When** `GET /api/profiles/{username}`, **Then** status `404`.
5. **Given** sem token, **When** `POST /api/profiles/B/follow`, **Then** status `401`.

---

### Edge Cases

- Token malformado, com assinatura inválida ou expirado em rota que exige autenticação: `401`.
- Token inválido enviado em `GET /api/profiles/{username}` (autenticação opcional): `401`; ausência de token: consulta anônima.
- Cabeçalho `Authorization` com esquema diferente de `Token` (por exemplo, `Bearer`): tratado como token ausente.
- Seguir quem já é seguido ou deixar de seguir quem não é seguido: operação idempotente, `200`.
- Usuário tenta seguir a si mesmo: `422`.
- E-mail duplicado com caixa diferente (`Jake@x.com` e `jake@x.com`): considerado o mesmo e-mail.
- `PUT /api/user` sem nenhum campo aceito: `422`.
- `PUT /api/user` com `password` de menos de 8 caracteres: `422`; 8 e 64 caracteres: `200`.
- `PUT /api/user` com `bio` ou `image` `null` ou texto em branco (`""`): limpa o campo (resposta `null`); campo ausente: mantém o valor.
- Corpo JSON malformado ou sem o objeto `user`: `422`.

## Requirements *(mandatory)*

### Functional Requirements

**Cadastro (`POST /api/users`)**

- **FR-001**: Quando receber `POST /api/users` com `user.username`, `user.email` e `user.password` válidos, o sistema deve criar o usuário e responder `201` com o corpo User (`email`, `token`, `username`, `bio`, `image`).
- **FR-002**: Se `user.username`, `user.email` ou `user.password` estiver ausente ou em branco no cadastro, então o sistema deve responder `422` com `{"errors": {"<campo>": ["can't be blank"]}}` para cada campo inválido.
- **FR-003**: Se `user.email` não tiver formato de e-mail válido, então o sistema deve responder `422` com `{"errors": {"email": ["is invalid"]}}`.
- **FR-004**: Se o `username` ou o `email` informado no cadastro já pertencer a outro usuário, então o sistema deve responder `409` com `{"errors": {"<campo>": ["has already been taken"]}}`.
- **FR-005**: O sistema deve armazenar a senha somente como *hash* adaptativo com sal, nunca em texto puro.
- **FR-006**: O sistema deve comparar e-mails sem diferenciar maiúsculas de minúsculas e sem espaços nas extremidades.

**Login (`POST /api/users/login`)**

- **FR-007**: Quando receber `POST /api/users/login` com `user.email` e `user.password` corretos, o sistema deve responder `200` com o corpo User contendo um novo token JWT.
- **FR-008**: Se `user.email` ou `user.password` estiver ausente ou em branco no login, então o sistema deve responder `422` no formato de erro do contrato.
- **FR-009**: Se o e-mail não existir ou a senha não conferir, então o sistema deve responder `401` com `{"errors": {"credentials": ["invalid"]}}`, sem indicar qual dos dois falhou.

**Autenticação**

- **FR-010**: O sistema deve ler o token do cabeçalho `Authorization: Token <jwt>`.
- **FR-011**: O sistema deve emitir tokens JWT assinados, com identificador do usuário e data de expiração.
- **FR-012**: Se uma rota que exige autenticação for chamada sem token, então o sistema deve responder `401` com `{"errors": {"token": ["is missing"]}}`.
- **FR-013**: Se o token for malformado, tiver assinatura inválida, estiver expirado ou apontar para usuário inexistente, então o sistema deve responder `401` com `{"errors": {"token": ["is invalid"]}}`.

**Usuário atual (`GET`/`PUT /api/user`)**

- **FR-014**: Quando receber `GET /api/user` com token válido, o sistema deve responder `200` com o corpo User do usuário autenticado.
- **FR-015**: Quando receber `PUT /api/user` com token válido, o sistema deve atualizar somente os campos aceitos presentes no corpo (`email`, `username`, `password`, `image`, `bio`) e responder `200` com o corpo User atualizado.
- **FR-016**: Se `PUT /api/user` não trouxer nenhum campo aceito, então o sistema deve responder `422` com `{"errors": {"user": ["at least one field is required"]}}`.
- **FR-017**: Se `PUT /api/user` trouxer `email`, `username` ou `password` em branco ou nulo, então o sistema deve responder `422` com `{"errors": {"<campo>": ["can't be blank"]}}`.
- **FR-018**: Se `PUT /api/user` trouxer `email` ou `username` já usado por outro usuário, então o sistema deve responder `422` com `{"errors": {"<campo>": ["has already been taken"]}}`.
- **FR-019**: Quando `PUT /api/user` trouxer `bio` ou `image` com valor `null` ou texto em branco, o sistema deve gravar o campo como nulo.
- **FR-019a**: Se `PUT /api/user` trouxer `password` com menos de 8 caracteres, então o sistema deve responder `422` com `{"errors": {"password": ["is too short (minimum is 8 characters)"]}}`; senhas de 8 a 64 caracteres devem ser aceitas (política NIST SP 800-63B do contrato).

**Perfis (`GET /api/profiles/{username}`)**

- **FR-020**: Quando receber `GET /api/profiles/{username}` de um usuário existente, o sistema deve responder `200` com `{"profile": {"username", "bio", "image", "following"}}`.
- **FR-021**: Enquanto a requisição não tiver token, o sistema deve responder o perfil com `following: false`.
- **FR-022**: Enquanto a requisição tiver token válido, o sistema deve informar em `following` se o usuário autenticado segue o perfil consultado.
- **FR-023**: Se o `username` do caminho não existir, então o sistema deve responder `404` com `{"errors": {"profile": ["not found"]}}`.

**Seguir (`POST`/`DELETE /api/profiles/{username}/follow`)**

- **FR-024**: Quando receber `POST /api/profiles/{username}/follow` com token válido, o sistema deve registrar que o usuário autenticado segue o perfil e responder `200` com Profile e `following: true`.
- **FR-025**: Quando receber `DELETE /api/profiles/{username}/follow` com token válido, o sistema deve remover a relação, se existir, e responder `200` com Profile e `following: false`.
- **FR-026**: Se o usuário autenticado tentar seguir a si mesmo, então o sistema deve responder `422` com `{"errors": {"profile": ["cannot follow yourself"]}}`.
- **FR-027**: Se o `username` alvo de seguir ou deixar de seguir não existir, então o sistema deve responder `404` com `{"errors": {"profile": ["not found"]}}`.

**Formato geral**

- **FR-028**: O sistema deve responder com `Content-Type: application/json; charset=utf-8` em todas as respostas com corpo.
- **FR-029**: Se o corpo da requisição for JSON malformado ou não tiver o objeto `user`, então o sistema deve responder `422` com `{"errors": {"body": ["..."]}}`.
- **FR-030**: Se ocorrer erro inesperado, então o sistema deve responder `500` no formato `{"errors": {...}}`, sem stack trace nem detalhes internos.
- **FR-031**: O sistema não deve oferecer exclusão de usuário nem recursos de artigos, feed, comentários, favoritos e tags.

### Key Entities

- **Usuário**: conta da plataforma. Atributos: e-mail (único, sem diferenciar caixa), username (único), *hash* da senha, bio (opcional), imagem (URL opcional).
- **Seguimento**: relação dirigida "usuário A segue usuário B". Única por par; um usuário não segue a si mesmo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos endpoints do recorte respondem com as rotas, campos JSON e códigos de status do contrato OpenAPI da descrição-base.
- **SC-002**: Todo cenário de aceitação e caso de borda acima é coberto por teste automatizado que passa em `./mvnw verify`.
- **SC-003**: Nenhuma senha em texto puro aparece no banco, em logs ou em respostas.
- **SC-004**: Um cliente consegue cadastrar, entrar, atualizar o perfil e seguir outro usuário usando apenas o contrato, sem conhecer detalhes internos.

## Assumptions

- O token JWT expira em 24 horas por padrão (configurável). Não há *refresh token* nem logout no servidor.
- `GET /api/user` e `PUT /api/user` devolvem no campo `token` o mesmo token recebido na requisição.
- O username diferencia maiúsculas de minúsculas; o e-mail não.
- Não há regra de tamanho mínimo de senha nem de formato de username além de não estar em branco, pois a descrição-base não define.
- Duplicidade em `PUT /api/user` usa `422`, porque o contrato OpenAPI desse endpoint não lista `409`.
