# Feature Specification: Recorte A da API Conduit — Autenticação, Usuário e Perfis

**Feature Branch**: `001-conduit-auth-profiles`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Use o arquivo descricao-base.md, na raiz do projeto, como única fonte de requisitos."

> Fonte única de requisitos: `descricao-base.md` (Recorte A da API Conduit/RealWorld). O contrato da API (rotas, formatos JSON, códigos de status e formato de erro) é obrigatório; em divergência entre a prosa e a especificação OpenAPI, vale a OpenAPI. Por isso, os requisitos citam endpoints, formatos e códigos de status — eles são o próprio requisito de negócio deste produto, que é somente uma API.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastro e login (Priority: P1)

Uma pessoa visitante cria uma conta informando nome de usuário, e-mail e senha e recebe imediatamente seus dados de usuário com um token de acesso. Depois, uma pessoa já cadastrada entra informando e-mail e senha e recebe os mesmos dados com um novo token, que usa para acessar os recursos protegidos.

**Why this priority**: Sem cadastro e login não há token, e sem token nenhuma outra funcionalidade protegida do recorte pode ser usada. É o MVP.

**Independent Test**: Cadastrar um usuário por `POST /api/users`, fazer login por `POST /api/users/login` e verificar que ambos devolvem o objeto `user` com `token` utilizável.

**Acceptance Scenarios**:

1. **Given** que não existe usuário com o nome "jake" nem com o e-mail "jake@jake.jake", **When** a pessoa envia `POST /api/users` com `{"user":{"username":"jake","email":"jake@jake.jake","password":"jakejake"}}`, **Then** o sistema responde `201` com `{"user":{"email":"jake@jake.jake","token":"<token>","username":"jake","bio":null,"image":null}}`.
2. **Given** que já existe um usuário com o nome "jake", **When** outra pessoa tenta se cadastrar com o nome "jake", **Then** o sistema responde `409` com `{"errors":{"username":["has already been taken"]}}`.
3. **Given** um cadastro sem o campo `email`, **When** é enviado a `POST /api/users`, **Then** o sistema responde `422` com `{"errors":{"email":["can't be blank"]}}`.
4. **Given** um usuário cadastrado com e-mail "jake@jake.jake" e senha "jakejake", **When** a pessoa envia `POST /api/users/login` com essas credenciais, **Then** o sistema responde `200` com o objeto `user` contendo um `token` válido.
5. **Given** um usuário cadastrado, **When** a pessoa tenta login com senha errada ou e-mail inexistente, **Then** o sistema responde `401` com corpo no formato de erro padrão, sem revelar qual dos dois campos está incorreto.

---

### User Story 2 - Consultar e atualizar o usuário autenticado (Priority: P2)

Uma pessoa autenticada consulta os próprios dados e atualiza qualquer combinação de e-mail, nome de usuário, senha, biografia e imagem.

**Why this priority**: Permite à pessoa manter a própria conta e é a base do perfil público (biografia e imagem). Depende apenas do P1.

**Independent Test**: Com um token obtido no P1, chamar `GET /api/user` e `PUT /api/user` e verificar os dados devolvidos; repetir sem token e verificar `401`.

**Acceptance Scenarios**:

1. **Given** uma pessoa autenticada, **When** envia `GET /api/user` com o cabeçalho `Authorization: Token <token>`, **Then** o sistema responde `200` com o objeto `user` dela, incluindo um `token`.
2. **Given** uma requisição sem cabeçalho `Authorization`, **When** é enviada a `GET /api/user`, **Then** o sistema responde `401` com `{"errors":{"token":["is missing"]}}`.
3. **Given** uma pessoa autenticada, **When** envia `PUT /api/user` com `{"user":{"bio":"I like to skateboard","image":"https://i.stack.imgur.com/xHWG8.jpg"}}`, **Then** o sistema responde `200` com o objeto `user` refletindo os novos valores e mantendo os demais.
4. **Given** uma pessoa autenticada que alterou a senha por `PUT /api/user`, **When** ela faz login com a nova senha, **Then** o login é aceito; **And** o login com a senha antiga é recusado com `401`.
5. **Given** que outro usuário já usa o e-mail "outro@x.com", **When** a pessoa autenticada tenta mudar o próprio e-mail para "outro@x.com", **Then** o sistema responde `422` com `{"errors":{"email":["has already been taken"]}}`.

---

### User Story 3 - Consultar perfil público (Priority: P3)

Qualquer pessoa, autenticada ou não, consulta o perfil público de um usuário pelo nome de usuário. Se estiver autenticada, o perfil indica se ela segue esse usuário.

**Why this priority**: Expõe a identidade pública dos usuários; é pré-requisito para a experiência de seguir.

**Independent Test**: Cadastrar um usuário e chamar `GET /api/profiles/{username}` com e sem token.

**Acceptance Scenarios**:

1. **Given** um usuário "jake" com biografia e imagem, **When** uma pessoa não autenticada envia `GET /api/profiles/jake`, **Then** o sistema responde `200` com `{"profile":{"username":"jake","bio":"...","image":"...","following":false}}`.
2. **Given** que não existe usuário "ninguem", **When** é enviado `GET /api/profiles/ninguem`, **Then** o sistema responde `404` com `{"errors":{"profile":["not found"]}}`.
3. **Given** uma pessoa autenticada que segue "jake", **When** envia `GET /api/profiles/jake` com seu token, **Then** o campo `following` é `true`.
4. **Given** uma requisição a `GET /api/profiles/jake` com um token inválido, **When** é enviada, **Then** o sistema responde `401`.

---

### User Story 4 - Seguir e deixar de seguir (Priority: P4)

Uma pessoa autenticada passa a seguir outro usuário e pode deixar de segui-lo a qualquer momento.

**Why this priority**: É a única interação social do recorte; depende de P1 e P3.

**Independent Test**: Com dois usuários cadastrados, A segue B por `POST /api/profiles/B/follow`, confere `following: true`, deixa de seguir por `DELETE /api/profiles/B/follow` e confere `following: false`.

**Acceptance Scenarios**:

1. **Given** A autenticado e B existente não seguido por A, **When** A envia `POST /api/profiles/B/follow`, **Then** o sistema responde `200` com o perfil de B e `following: true`.
2. **Given** A já segue B, **When** A envia `POST /api/profiles/B/follow` novamente, **Then** o sistema responde `200` com `following: true` e não duplica a relação.
3. **Given** A segue B, **When** A envia `DELETE /api/profiles/B/follow`, **Then** o sistema responde `200` com o perfil de B e `following: false`.
4. **Given** A não segue B, **When** A envia `DELETE /api/profiles/B/follow`, **Then** o sistema responde `200` com `following: false`.
5. **Given** uma requisição sem token, **When** é enviada a `POST` ou `DELETE /api/profiles/B/follow`, **Then** o sistema responde `401`.
6. **Given** que não existe usuário "ninguem", **When** A envia `POST /api/profiles/ninguem/follow`, **Then** o sistema responde `404`.

---

### Edge Cases

- Campos obrigatórios ausentes, nulos, vazios ou só com espaços no cadastro ou no login → `422`, com uma entrada em `errors` por campo inválido.
- Corpo ausente, JSON malformado ou sem o objeto raiz `user` → `422` no formato de erro padrão.
- E-mail com formato inválido no cadastro ou na atualização → `422`.
- Cadastro com e-mail já usado → `409` com `{"errors":{"email":["has already been taken"]}}`; com nome e e-mail já usados, ambos aparecem em `errors`.
- `PUT /api/user` com objeto `user` sem nenhum campo aceito → `422`.
- `PUT /api/user` com `bio` ou `image` iguais a `null` → o valor é limpo e devolvido como `null`.
- `PUT /api/user` trocando o nome de usuário para um já usado por outra pessoa → `422` com `{"errors":{"username":["has already been taken"]}}`; trocar para o próprio valor atual não é conflito.
- Cabeçalho `Authorization` com esquema diferente de `Token`, token malformado, com assinatura inválida ou expirado → `401` com `{"errors":{"token":["is invalid"]}}`.
- Token válido de um usuário que não existe mais na base → `401`.
- Usuário tentando seguir a si mesmo → `422` com `{"errors":{"profile":["cannot follow yourself"]}}`.
- Rota inexistente ou método não suportado não deve expor detalhes internos (stack trace, nomes de tabelas).

## Requirements *(mandatory)*

### Functional Requirements

**Cadastro (`POST /api/users`)**

- **FR-001**: Quando receber `POST /api/users` com `user.username`, `user.email` e `user.password` válidos e ainda não usados, o sistema deve criar o usuário e responder `201` com o objeto `user` (`email`, `token`, `username`, `bio`, `image`), com `bio` e `image` iguais a `null`.
- **FR-002**: Se `POST /api/users` vier sem `username`, `email` ou `password`, ou com algum deles vazio, então o sistema deve responder `422` com `{"errors":{"<campo>":["can't be blank"]}}` para cada campo inválido.
- **FR-003**: Se `POST /api/users` vier com `email` em formato inválido, então o sistema deve responder `422` com `{"errors":{"email":["is invalid"]}}`.
- **FR-004**: Se `POST /api/users` vier com `username` já cadastrado, então o sistema deve responder `409` com `{"errors":{"username":["has already been taken"]}}`.
- **FR-005**: Se `POST /api/users` vier com `email` já cadastrado, então o sistema deve responder `409` com `{"errors":{"email":["has already been taken"]}}`.

**Login (`POST /api/users/login`)**

- **FR-006**: Quando receber `POST /api/users/login` com `user.email` e `user.password` que correspondem a um usuário cadastrado, o sistema deve responder `200` com o objeto `user` contendo um novo `token`.
- **FR-007**: Se `POST /api/users/login` vier sem `email` ou `password`, ou com algum deles vazio, então o sistema deve responder `422` com `{"errors":{"<campo>":["can't be blank"]}}`.
- **FR-008**: Se as credenciais de `POST /api/users/login` não corresponderem a um usuário cadastrado, então o sistema deve responder `401` com `{"errors":{"credentials":["invalid"]}}`, sem indicar se o erro está no e-mail ou na senha.

**Autenticação por token**

- **FR-009**: O sistema deve aceitar o token de acesso somente no cabeçalho `Authorization` no formato `Token <jwt>`.
- **FR-010**: O sistema deve emitir tokens JWT assinados, com identificação do usuário e data de expiração.
- **FR-011**: Se uma rota que exige autenticação (`GET /api/user`, `PUT /api/user`, `POST` e `DELETE /api/profiles/{username}/follow`) receber requisição sem o cabeçalho `Authorization`, então o sistema deve responder `401` com `{"errors":{"token":["is missing"]}}`.
- **FR-012**: Se qualquer rota receber cabeçalho `Authorization` com esquema diferente de `Token`, token malformado, com assinatura inválida, expirado ou de usuário inexistente, então o sistema deve responder `401` com `{"errors":{"token":["is invalid"]}}`.

**Usuário autenticado (`GET /api/user` e `PUT /api/user`)**

- **FR-013**: Quando receber `GET /api/user` com token válido, o sistema deve responder `200` com o objeto `user` do usuário autenticado, incluindo um `token` válido.
- **FR-014**: Quando receber `PUT /api/user` com token válido e ao menos um dos campos `email`, `username`, `password`, `bio` ou `image`, o sistema deve atualizar somente os campos enviados e responder `200` com o objeto `user` atualizado, incluindo um `token` válido.
- **FR-015**: Quando `PUT /api/user` trouxer `password`, o sistema deve passar a aceitar somente a nova senha no login.
- **FR-016**: Quando `PUT /api/user` trouxer `bio` ou `image` com valor `null`, o sistema deve gravar o campo como vazio e devolvê-lo como `null`.
- **FR-017**: Se `PUT /api/user` vier sem nenhum campo aceito no objeto `user`, então o sistema deve responder `422` no formato de erro padrão.
- **FR-018**: Se `PUT /api/user` trouxer `email`, `username` ou `password` vazios, ou `email` em formato inválido, então o sistema deve responder `422` com uma entrada em `errors` para cada campo inválido.
- **FR-019**: Se `PUT /api/user` trouxer `email` ou `username` já usado por outro usuário, então o sistema deve responder `422` com `{"errors":{"<campo>":["has already been taken"]}}`.

**Perfil (`GET /api/profiles/{username}`)**

- **FR-020**: Quando receber `GET /api/profiles/{username}` de um usuário existente, o sistema deve responder `200` com `{"profile":{"username","bio","image","following"}}`.
- **FR-021**: Enquanto a requisição a `GET /api/profiles/{username}` não tiver cabeçalho `Authorization`, o sistema deve devolver `following: false`.
- **FR-022**: Enquanto a requisição a `GET /api/profiles/{username}` tiver token válido, o sistema deve devolver `following: true` se o usuário autenticado segue o perfil consultado, e `false` caso contrário.
- **FR-023**: Se `{username}` não corresponder a um usuário cadastrado em `GET /api/profiles/{username}`, `POST /api/profiles/{username}/follow` ou `DELETE /api/profiles/{username}/follow`, então o sistema deve responder `404` com `{"errors":{"profile":["not found"]}}`.

**Seguir (`POST` e `DELETE /api/profiles/{username}/follow`)**

- **FR-024**: Quando receber `POST /api/profiles/{username}/follow` com token válido, o sistema deve registrar que o usuário autenticado segue o perfil e responder `200` com o `profile` e `following: true`.
- **FR-025**: Enquanto o usuário autenticado já seguir o perfil, o sistema deve tratar `POST /api/profiles/{username}/follow` sem criar relação duplicada e responder `200` com `following: true`.
- **FR-026**: Quando receber `DELETE /api/profiles/{username}/follow` com token válido, o sistema deve remover a relação, se existir, e responder `200` com o `profile` e `following: false`.
- **FR-027**: Se o usuário autenticado tentar seguir o próprio perfil, então o sistema deve responder `422` com `{"errors":{"profile":["cannot follow yourself"]}}`.

**Formato de resposta e erros**

- **FR-028**: O sistema deve responder com `Content-Type: application/json; charset=utf-8` em todas as respostas com corpo.
- **FR-029**: O sistema deve devolver todos os erros no formato `{"errors":{"<chave>":["<mensagem>"]}}`.
- **FR-030**: Se o corpo de uma requisição estiver ausente, for JSON malformado ou não tiver o objeto raiz `user`, então o sistema deve responder `422` com `{"errors":{"body":["can't be empty"]}}` ou mensagem equivalente no mesmo formato.
- **FR-031**: Se ocorrer um erro interno, então o sistema deve responder sem expor stack traces, nomes de tabelas ou outros detalhes internos.
- **FR-032**: O sistema deve sempre incluir as chaves `bio` e `image` no objeto `user` e no objeto `profile`, com `null` quando não houver valor.
- **FR-033**: O sistema não deve incluir a senha nem seu hash em nenhuma resposta.

**Segurança dos dados**

- **FR-034**: O sistema deve armazenar senhas somente na forma de hash adequado para senhas, nunca em texto puro.
- **FR-035**: O sistema deve tratar a comparação de e-mails sem diferenciar maiúsculas de minúsculas, tanto na unicidade quanto no login.

### Key Entities

- **Usuário**: pessoa cadastrada. Atributos: nome de usuário (único, identifica o perfil público), e-mail (único), senha (guardada somente como hash), biografia (opcional) e imagem (URL opcional).
- **Relação de seguir**: vínculo direcional entre um usuário seguidor e um usuário seguido. Cada par é único; não há relação de um usuário com ele mesmo.
- **Perfil**: visão pública de um usuário (nome de usuário, biografia, imagem) mais o indicador `following`, calculado em relação a quem consulta. Não é armazenado separadamente.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos cenários de aceitação deste documento passam em uma suíte automatizada de testes de API executada contra um banco limpo.
- **SC-002**: Uma pessoa nova consegue se cadastrar e usar o token recebido para consultar os próprios dados em no máximo 2 requisições.
- **SC-003**: 100% das respostas de erro seguem o formato `{"errors":{...}}` com o código de status definido no contrato; nenhuma resposta expõe senha, hash ou detalhes internos.
- **SC-004**: 95% das requisições do recorte são respondidas em menos de 500 ms com até 50 requisições simultâneas em ambiente local.
- **SC-005**: Requisições repetidas de seguir ou deixar de seguir o mesmo perfil nunca geram relações duplicadas nem erros.

## Assumptions

- Escopo restrito ao Recorte A: cadastro, login, usuário autenticado, perfil e seguir/deixar de seguir. Artigos, feed, comentários, favoritos, tags e exclusão de usuário estão fora do escopo.
- Validade do token: 24 horas, configurável fora do código. Não há renovação de token nem logout; o token só deixa de valer ao expirar.
- O token devolvido por `GET /api/user` e `PUT /api/user` pode ser um novo token ou o mesmo enviado; basta ser válido.
- Validação mínima: campos obrigatórios não podem ser vazios nem só espaços; o e-mail deve ter formato válido. Não há regra de tamanho mínimo de senha nem de formato de nome de usuário além de não ser vazio, pois a fonte não as define.
- O nome de usuário diferencia maiúsculas de minúsculas na busca do perfil e na unicidade; o e-mail não diferencia (FR-035).
- A OpenAPI não prevê `409` em `PUT /api/user`; por isso conflitos na atualização respondem `422` (FR-019), enquanto conflitos no cadastro respondem `409` (FR-004, FR-005), como prevê a OpenAPI.
- As mensagens de erro seguem o estilo do contrato ("can't be blank", "has already been taken", "is missing", "is invalid", "not found"); a suíte de testes valida as chaves e os códigos de status.
- A recusa de seguir a si mesmo (FR-027) é uma decisão de consistência dos dados, não uma funcionalidade nova.
- A API é consumida apenas por clientes HTTP; não há interface web.
