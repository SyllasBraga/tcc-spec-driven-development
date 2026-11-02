# Feature Specification: Recorte A da API Conduit — autenticação, usuário e perfis

**Feature Branch**: `001-conduit-auth-profiles`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Use o arquivo descricao-base.md, na raiz do projeto, como única fonte de requisitos."

## Contexto

Plataforma de publicação de artigos no estilo do Medium (API Conduit / RealWorld). Este recorte cobre apenas: cadastro e login com token JWT, consulta e atualização do usuário autenticado, consulta de perfil público e seguir/deixar de seguir usuários. Artigos, feed, comentários, favoritos e tags estão fora do escopo. O contrato da API (rotas, formatos JSON, códigos de status e formato de erro) definido em `descricao-base.md` é obrigatório; quando a prosa divergir da especificação OpenAPI, vale a OpenAPI.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastro de novo usuário (Priority: P1)

Uma pessoa visitante informa nome de usuário, e-mail e senha e passa a ter uma conta na plataforma, recebendo imediatamente um token que a identifica nas próximas requisições.

**Why this priority**: Sem cadastro não existem usuários; todas as demais funcionalidades dependem disso.

**Independent Test**: Enviar `POST /api/users` com dados válidos e verificar a resposta `201` no formato User com token; repetir com dados duplicados e inválidos e verificar `409` e `422`.

**Acceptance Scenarios**:

1. **Given** que não existe usuário com o e-mail `jake@jake.jake` nem com o nome `Jacob`, **When** a visitante envia `POST /api/users` com `{"user":{"username":"Jacob","email":"jake@jake.jake","password":"jakejake"}}`, **Then** o sistema responde `201` com `{"user":{"email":"jake@jake.jake","token":"<jwt>","username":"Jacob","bio":null,"image":null}}`.
2. **Given** que já existe usuário com o nome `Jacob`, **When** outra visitante tenta se cadastrar com o mesmo nome, **Then** o sistema responde `409` com `{"errors":{"username":["has already been taken"]}}`.
3. **Given** qualquer estado, **When** a visitante envia o cadastro sem `email`, **Then** o sistema responde `422` com `{"errors":{"email":["can't be blank"]}}`.

---

### User Story 2 - Login de usuário existente (Priority: P1)

Uma pessoa já cadastrada informa e-mail e senha e recebe um token para acessar as funcionalidades autenticadas.

**Why this priority**: É o único meio de obter um token para uma conta existente; junto com o cadastro forma o MVP de autenticação.

**Independent Test**: Cadastrar um usuário, enviar `POST /api/users/login` com as credenciais corretas e verificar `200` com o User e token; repetir com senha errada e verificar `401`.

**Acceptance Scenarios**:

1. **Given** um usuário cadastrado com `jake@jake.jake` / `jakejake`, **When** ele envia `POST /api/users/login` com essas credenciais, **Then** o sistema responde `200` com o objeto `user` contendo `email`, `token`, `username`, `bio` e `image`.
2. **Given** um usuário cadastrado, **When** ele envia a senha incorreta, **Then** o sistema responde `401` com `{"errors":{"credentials":["invalid"]}}`.
3. **Given** qualquer estado, **When** o login é enviado sem `password`, **Then** o sistema responde `422` com `{"errors":{"password":["can't be blank"]}}`.

---

### User Story 3 - Consulta e atualização do usuário autenticado (Priority: P2)

Um usuário autenticado consulta os próprios dados e altera e-mail, nome de usuário, senha, biografia ou imagem.

**Why this priority**: Permite manter a conta, mas depende de cadastro e login já existirem.

**Independent Test**: Com um token válido, enviar `GET /api/user` e `PUT /api/user` alterando `bio` e `image`, e verificar que as mudanças aparecem na resposta e em uma nova consulta.

**Acceptance Scenarios**:

1. **Given** um usuário autenticado, **When** envia `GET /api/user` com `Authorization: Token <jwt>`, **Then** o sistema responde `200` com o seu objeto `user`, incluindo um token válido.
2. **Given** um usuário autenticado, **When** envia `PUT /api/user` com `{"user":{"bio":"I like to skateboard","image":"https://i.stack.imgur.com/xHWG8.jpg"}}`, **Then** o sistema responde `200` com `bio` e `image` atualizados e os demais campos inalterados.
3. **Given** um usuário autenticado que alterou a senha, **When** tenta logar com a senha antiga, **Then** o sistema responde `401`; com a nova senha, responde `200`.
4. **Given** nenhuma credencial, **When** a requisição `GET /api/user` é enviada sem o cabeçalho `Authorization`, **Then** o sistema responde `401` com `{"errors":{"token":["is missing"]}}`.
5. **Given** dois usuários, **When** um deles tenta mudar o próprio e-mail para o e-mail do outro, **Then** o sistema responde `409` com `{"errors":{"email":["has already been taken"]}}`.

---

### User Story 4 - Consulta de perfil público (Priority: P2)

Qualquer pessoa, autenticada ou não, consulta o perfil público de um usuário pelo nome de usuário. Se estiver autenticada, o perfil informa se ela segue esse usuário.

**Why this priority**: Base para a funcionalidade social; útil mesmo sem o recurso de seguir.

**Independent Test**: Cadastrar um usuário e enviar `GET /api/profiles/{username}` sem token; verificar `200` com `following: false`. Consultar nome inexistente e verificar `404`.

**Acceptance Scenarios**:

1. **Given** o usuário `jake` cadastrado, **When** uma visitante sem token envia `GET /api/profiles/jake`, **Then** o sistema responde `200` com `{"profile":{"username":"jake","bio":<bio>,"image":<image>,"following":false}}`.
2. **Given** um usuário autenticado que segue `jake`, **When** envia `GET /api/profiles/jake` com seu token, **Then** o sistema responde `200` com `following: true`.
3. **Given** que não existe o usuário `ninguem`, **When** é enviado `GET /api/profiles/ninguem`, **Then** o sistema responde `404` com `{"errors":{"profile":["not found"]}}`.

---

### User Story 5 - Seguir e deixar de seguir usuários (Priority: P3)

Um usuário autenticado passa a seguir outro usuário e pode deixar de segui-lo depois.

**Why this priority**: Completa o recorte social, mas depende de todas as histórias anteriores.

**Independent Test**: Com dois usuários, A envia `POST /api/profiles/{B}/follow` e verifica `following: true`; depois `DELETE /api/profiles/{B}/follow` e verifica `following: false`.

**Acceptance Scenarios**:

1. **Given** os usuários A (autenticado) e B, com A não seguindo B, **When** A envia `POST /api/profiles/B/follow`, **Then** o sistema responde `200` com o perfil de B e `following: true`.
2. **Given** A já segue B, **When** A envia `POST /api/profiles/B/follow` de novo, **Then** o sistema responde `200` com `following: true` e não cria uma segunda relação.
3. **Given** A segue B, **When** A envia `DELETE /api/profiles/B/follow`, **Then** o sistema responde `200` com o perfil de B e `following: false`.
4. **Given** A não segue B, **When** A envia `DELETE /api/profiles/B/follow`, **Then** o sistema responde `200` com `following: false`.
5. **Given** nenhuma credencial, **When** é enviado `POST /api/profiles/B/follow` sem token, **Then** o sistema responde `401`.
6. **Given** A autenticado, **When** envia `POST /api/profiles/ninguem/follow` para um usuário inexistente, **Then** o sistema responde `404` com `{"errors":{"profile":["not found"]}}`.

---

### Edge Cases

- Token com assinatura inválida, malformado ou expirado: tratado como não autenticado (`401`), inclusive no `GET /api/profiles/{username}`, onde a autenticação é opcional mas o token, se enviado, precisa ser válido.
- Cabeçalho `Authorization` com esquema diferente de `Token` (por exemplo, `Bearer`): tratado como token ausente (`401` nas rotas autenticadas; anônimo na consulta de perfil).
- Token válido de um usuário que não existe mais na base: `401`.
- Corpo da requisição ausente, JSON malformado ou sem o objeto raiz `user`: `422`.
- Campos obrigatórios presentes mas vazios ou só com espaços: `422` com `can't be blank`.
- E-mail com formato inválido no cadastro ou na atualização: `422` com `{"errors":{"email":["is invalid"]}}`.
- `PUT /api/user` sem nenhum campo dentro de `user`: `422`.
- `PUT /api/user` com `bio` ou `image` iguais a `null`: o campo é limpo.
- `PUT /api/user` alterando o próprio nome ou e-mail para o valor atual: aceito, sem conflito.
- Usuário tentando seguir a si mesmo: `422` com `{"errors":{"profile":["cannot follow yourself"]}}`.
- Cadastro com e-mail e nome de usuário ambos já usados: `409` listando as duas chaves (`email` e `username`).

## Requirements *(mandatory)*

### Functional Requirements

Requisitos em notação EARS.

**Cadastro (`POST /api/users`)**

- **FR-001**: Quando uma requisição `POST /api/users` com `username`, `email` e `password` válidos e únicos for recebida, o sistema deve criar o usuário e responder `201` com o corpo `{"user":{"email","token","username","bio":null,"image":null}}`.
- **FR-002**: Se `username`, `email` ou `password` estiverem ausentes, nulos ou em branco no cadastro, então o sistema deve responder `422` com `{"errors":{"<campo>":["can't be blank"]}}` para cada campo inválido.
- **FR-003**: Se o `email` do cadastro não tiver formato de e-mail válido, então o sistema deve responder `422` com `{"errors":{"email":["is invalid"]}}`.
- **FR-004**: Se o `username` ou o `email` do cadastro já pertencer a outro usuário, então o sistema deve responder `409` com `{"errors":{"<campo>":["has already been taken"]}}` para cada campo em conflito.

**Login (`POST /api/users/login`)**

- **FR-005**: Quando uma requisição `POST /api/users/login` com `email` e `password` correspondentes a um usuário cadastrado for recebida, o sistema deve responder `200` com o objeto User contendo um novo token.
- **FR-006**: Se `email` ou `password` estiverem ausentes, nulos ou em branco no login, então o sistema deve responder `422` com `{"errors":{"<campo>":["can't be blank"]}}`.
- **FR-007**: Se o `email` não pertencer a nenhum usuário ou a `password` não conferir, então o sistema deve responder `401` com `{"errors":{"credentials":["invalid"]}}`, sem indicar qual dos dois falhou.

**Autenticação**

- **FR-008**: O sistema deve identificar o usuário autenticado pelo cabeçalho `Authorization: Token <jwt>`.
- **FR-009**: O sistema deve emitir tokens JWT assinados que identificam o usuário e têm prazo de validade.
- **FR-010**: Se uma rota que exige autenticação (`GET /api/user`, `PUT /api/user`, `POST /api/profiles/{username}/follow`, `DELETE /api/profiles/{username}/follow`) for chamada sem o cabeçalho `Authorization` ou com esquema diferente de `Token`, então o sistema deve responder `401` com `{"errors":{"token":["is missing"]}}`.
- **FR-011**: Se o token enviado for malformado, tiver assinatura inválida, estiver expirado ou referenciar usuário inexistente, então o sistema deve responder `401` com `{"errors":{"token":["is invalid"]}}`, inclusive em `GET /api/profiles/{username}`.

**Usuário autenticado (`GET /api/user`, `PUT /api/user`)**

- **FR-012**: Quando uma requisição autenticada `GET /api/user` for recebida, o sistema deve responder `200` com o objeto User do usuário autenticado, incluindo um token válido.
- **FR-013**: Quando uma requisição autenticada `PUT /api/user` com um ou mais dos campos `email`, `username`, `password`, `bio` e `image` for recebida, o sistema deve atualizar somente os campos enviados e responder `200` com o objeto User atualizado.
- **FR-014**: Se o `PUT /api/user` não contiver nenhum dos campos aceitos, então o sistema deve responder `422` com `{"errors":{"user":["can't be empty"]}}`.
- **FR-015**: Se `email`, `username` ou `password` forem enviados no `PUT /api/user` com valor nulo ou em branco, então o sistema deve responder `422` com `{"errors":{"<campo>":["can't be blank"]}}`.
- **FR-016**: Se o `email` enviado no `PUT /api/user` não tiver formato de e-mail válido, então o sistema deve responder `422` com `{"errors":{"email":["is invalid"]}}`.
- **FR-017**: Se o `email` ou o `username` enviado no `PUT /api/user` já pertencer a outro usuário, então o sistema deve responder `409` com `{"errors":{"<campo>":["has already been taken"]}}`.
- **FR-018**: Quando `bio` ou `image` forem enviados com valor `null` no `PUT /api/user`, o sistema deve limpar o campo e devolvê-lo como `null`.
- **FR-019**: Quando a `password` for alterada pelo `PUT /api/user`, o sistema deve passar a aceitar no login somente a nova senha.

**Perfis (`GET /api/profiles/{username}`)**

- **FR-020**: Quando uma requisição `GET /api/profiles/{username}` sem token for recebida para um usuário existente, o sistema deve responder `200` com `{"profile":{"username","bio","image","following":false}}`.
- **FR-021**: Quando uma requisição autenticada `GET /api/profiles/{username}` for recebida para um usuário existente, o sistema deve responder `200` com `following` igual a `true` se o usuário autenticado seguir o perfil e `false` caso contrário.
- **FR-022**: Se o `{username}` de qualquer rota `/api/profiles/{username}` não corresponder a um usuário cadastrado, então o sistema deve responder `404` com `{"errors":{"profile":["not found"]}}`.

**Seguir (`POST` e `DELETE /api/profiles/{username}/follow`)**

- **FR-023**: Quando uma requisição autenticada `POST /api/profiles/{username}/follow` for recebida para outro usuário existente, o sistema deve registrar que o usuário autenticado segue esse perfil e responder `200` com o Profile e `following: true`.
- **FR-024**: Enquanto o usuário autenticado já seguir o perfil, o sistema deve tratar um novo `POST /api/profiles/{username}/follow` sem criar relação duplicada e responder `200` com `following: true`.
- **FR-025**: Quando uma requisição autenticada `DELETE /api/profiles/{username}/follow` for recebida para um usuário existente, o sistema deve remover a relação de seguir, se existir, e responder `200` com o Profile e `following: false`.
- **FR-026**: Se o `{username}` de `POST /api/profiles/{username}/follow` for o do próprio usuário autenticado, então o sistema deve responder `422` com `{"errors":{"profile":["cannot follow yourself"]}}`.

**Formato e erros gerais**

- **FR-027**: O sistema deve responder com `Content-Type: application/json; charset=utf-8` em todas as respostas com corpo, inclusive as de erro.
- **FR-028**: Se o corpo de uma requisição `POST` ou `PUT` estiver ausente, for JSON malformado ou não tiver o objeto raiz `user`, então o sistema deve responder `422` com `{"errors":{"body":["can't be empty"]}}`.
- **FR-029**: O sistema deve devolver todos os erros no formato `{"errors":{"<chave>":["<mensagem>", ...]}}`, sem stack traces nem detalhes internos.
- **FR-030**: O sistema deve armazenar senhas somente na forma de hash com sal e nunca incluí-las em respostas ou logs.
- **FR-031**: O sistema deve sempre incluir as chaves `bio` e `image` nas respostas User e Profile, com valor `null` quando não preenchidas.

### Key Entities

- **Usuário**: pessoa cadastrada. Atributos: nome de usuário (único, público), e-mail (único, privado ao próprio usuário), senha (armazenada só como hash), biografia (opcional) e imagem (URL opcional).
- **Perfil**: visão pública de um usuário (nome de usuário, biografia, imagem), acrescida do indicador `following` calculado em relação a quem consulta.
- **Relação de seguir**: vínculo direcionado entre um usuário seguidor e um usuário seguido; no máximo uma relação por par e nunca de um usuário para si mesmo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das rotas do recorte (7 operações) respondem com os códigos de status e o formato JSON definidos no contrato, verificados por uma suíte de testes de API.
- **SC-002**: Uma pessoa nova consegue se cadastrar, obter o token e consultar os próprios dados em no máximo 3 requisições.
- **SC-003**: 100% das requisições a rotas protegidas sem credencial válida são recusadas com `401`.
- **SC-004**: Nenhuma resposta da API contém a senha, o hash da senha ou detalhes internos de erro.
- **SC-005**: 95% das requisições do recorte são respondidas em menos de 500 ms em ambiente local com até 50 usuários simultâneos.
- **SC-006**: Repetir a mesma operação de seguir ou deixar de seguir N vezes produz o mesmo estado final que executá-la uma vez.

## Assumptions

- A especificação OpenAPI prevalece sobre a prosa: cadastro bem-sucedido responde `201` e conflitos de unicidade (cadastro e atualização) respondem `409`, enquanto falhas de validação respondem `422`.
- As chaves e mensagens de erro não fixadas pelo contrato seguem o padrão dos exemplos (`can't be blank`, `has already been taken`, `is invalid`, `not found`, `is missing`) e usam como chave o nome do campo ou do recurso (`profile`, `token`, `credentials`, `body`, `user`).
- Login com credenciais inválidas responde `401`, conforme a OpenAPI.
- Seguir a si mesmo não é permitido (`422`); deixar de seguir a si mesmo apenas devolve `following: false`.
- Seguir e deixar de seguir são idempotentes.
- A comparação de e-mail para unicidade e login ignora maiúsculas e minúsculas; o nome de usuário diferencia maiúsculas e minúsculas, inclusive na rota `/api/profiles/{username}`.
- Não há regras de tamanho mínimo de senha nem de formato de nome de usuário além de não ser vazio, pois o contrato não as define.
- O token tem validade de 24 horas e não existe logout nem revogação de token neste recorte.
- O token devolvido por `GET /api/user` e `PUT /api/user` pode ser o mesmo enviado na requisição ou um novo, desde que seja válido.
- A imagem é aceita como texto livre (URL), sem validação de existência.
- Não há exclusão de usuário, recuperação de senha, confirmação de e-mail nem papéis de administração; o código `403` não é usado por nenhuma rota deste recorte.
- Artigos, feed, comentários, favoritos e tags estão fora do escopo.
