# Feature Specification: Conduit — Recorte A (usuários, autenticação e perfis)

**Feature Branch**: `001-conduit-usuarios-perfis`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Use o arquivo descricao-base.md, na raiz do projeto, como única fonte de requisitos."

## Visão geral

Backend do recorte A da API Conduit (RealWorld), plataforma de publicação de artigos no estilo do Medium. O recorte cobre cadastro e login de usuários com token, consulta e atualização do usuário autenticado, consulta do perfil público de um usuário e seguir/deixar de seguir outros usuários. Artigos, feed, comentários, favoritos e tags estão fora do escopo, assim como a exclusão de usuários.

O contrato da API (rotas, formatos JSON, códigos de status e formato de erro) definido em `descricao-base.md` é obrigatório. Em caso de divergência entre a prosa e a especificação OpenAPI desse arquivo, vale a especificação OpenAPI.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastro de usuário (Priority: P1)

Uma pessoa visitante cria uma conta informando nome de usuário, e-mail e senha, e recebe de volta os dados da conta junto com um token de acesso, já podendo usar as funcionalidades autenticadas.

**Why this priority**: Sem contas não há usuários autenticados, perfis nem relações de seguir. É a base de todo o recorte.

**Independent Test**: Enviar `POST /api/users` com dados válidos e verificar a resposta `201` com o objeto `user` contendo `email`, `token`, `username`, `bio` e `image`.

**Acceptance Scenarios**:

1. **Given** que não existe conta com o e-mail `jake@jake.jake` nem com o nome `Jacob`, **When** a visitante envia `POST /api/users` com `{"user":{"username":"Jacob","email":"jake@jake.jake","password":"jakejake"}}`, **Then** o sistema responde `201` com `{"user":{"email":"jake@jake.jake","token":"<token>","username":"Jacob","bio":null,"image":null}}`.
2. **Given** que já existe uma conta com o nome `Jacob`, **When** outra pessoa tenta se cadastrar com o nome `Jacob`, **Then** o sistema responde `409` com `{"errors":{"username":["has already been taken"]}}`.
3. **Given** que já existe uma conta com o e-mail `jake@jake.jake`, **When** outra pessoa tenta se cadastrar com esse e-mail, **Then** o sistema responde `409` com `{"errors":{"email":["has already been taken"]}}`.
4. **Given** uma requisição de cadastro sem o campo `password`, **When** ela é enviada, **Then** o sistema responde `422` com `{"errors":{"password":["can't be blank"]}}`.

---

### User Story 2 - Login (Priority: P1)

Uma pessoa já cadastrada informa e-mail e senha e recebe os dados da conta com um token de acesso válido.

**Why this priority**: É o meio de obter um token em sessões futuras; sem ele, as funcionalidades autenticadas ficam inacessíveis após o cadastro.

**Independent Test**: Cadastrar um usuário, enviar `POST /api/users/login` com as credenciais corretas e verificar a resposta `200` com o objeto `user` e um token aceito em `GET /api/user`.

**Acceptance Scenarios**:

1. **Given** um usuário cadastrado com e-mail `jake@jake.jake` e senha `jakejake`, **When** ele envia `POST /api/users/login` com essas credenciais, **Then** o sistema responde `200` com o objeto `user` contendo um `token` válido.
2. **Given** um usuário cadastrado, **When** ele envia a senha errada, **Then** o sistema responde `401` com `{"errors":{"email or password":["is invalid"]}}`.
3. **Given** que não existe conta com o e-mail informado, **When** a pessoa tenta fazer login, **Then** o sistema responde `401` com a mesma mensagem de credenciais inválidas, sem revelar se o e-mail existe.
4. **Given** uma requisição de login sem o campo `email`, **When** ela é enviada, **Then** o sistema responde `422` com `{"errors":{"email":["can't be blank"]}}`.

---

### User Story 3 - Consultar e atualizar o usuário autenticado (Priority: P2)

O usuário autenticado consulta os próprios dados e altera e-mail, nome de usuário, senha, biografia ou imagem.

**Why this priority**: Permite manter a conta e o perfil público atualizados, mas depende do cadastro e do login.

**Independent Test**: Com um token válido, enviar `GET /api/user` e `PUT /api/user` alterando `bio` e verificar que a resposta e as consultas seguintes refletem a alteração.

**Acceptance Scenarios**:

1. **Given** um usuário autenticado, **When** ele envia `GET /api/user` com o cabeçalho `Authorization: Token <token>`, **Then** o sistema responde `200` com o objeto `user` do próprio usuário, incluindo um `token`.
2. **Given** um usuário autenticado, **When** ele envia `PUT /api/user` com `{"user":{"bio":"I like to skateboard","image":"https://i.stack.imgur.com/xHWG8.jpg"}}`, **Then** o sistema responde `200` com o objeto `user` refletindo a nova `bio` e a nova `image`, mantendo os demais campos.
3. **Given** um usuário autenticado, **When** ele altera a senha via `PUT /api/user`, **Then** o login seguinte só é aceito com a nova senha.
4. **Given** um usuário autenticado, **When** ele tenta alterar o e-mail para um e-mail já usado por outra conta, **Then** o sistema responde `422` com `{"errors":{"email":["has already been taken"]}}`.
5. **Given** uma requisição sem o cabeçalho `Authorization`, **When** ela é enviada para `GET /api/user` ou `PUT /api/user`, **Then** o sistema responde `401` com `{"errors":{"token":["is missing"]}}`.

---

### User Story 4 - Consultar perfil público (Priority: P2)

Qualquer pessoa, autenticada ou não, consulta o perfil público de um usuário pelo nome de usuário. Quando autenticada, vê também se segue esse usuário.

**Why this priority**: É a visão pública dos usuários e a base para a funcionalidade de seguir.

**Independent Test**: Cadastrar um usuário e enviar `GET /api/profiles/{username}` sem autenticação, verificando a resposta `200` com o objeto `profile` e `following` igual a `false`.

**Acceptance Scenarios**:

1. **Given** um usuário `jake` cadastrado, **When** uma visitante não autenticada envia `GET /api/profiles/jake`, **Then** o sistema responde `200` com `{"profile":{"username":"jake","bio":...,"image":...,"following":false}}`.
2. **Given** que o usuário autenticado `ana` segue `jake`, **When** `ana` envia `GET /api/profiles/jake` com seu token, **Then** o sistema responde `200` com `following` igual a `true`.
3. **Given** que não existe usuário `fulano`, **When** alguém envia `GET /api/profiles/fulano`, **Then** o sistema responde `404` com `{"errors":{"profile":["not found"]}}`.

---

### User Story 5 - Seguir e deixar de seguir usuários (Priority: P3)

O usuário autenticado passa a seguir outro usuário ou deixa de segui-lo, e recebe o perfil atualizado.

**Why this priority**: Complementa os perfis com a relação social entre usuários; depende de todas as histórias anteriores.

**Independent Test**: Com dois usuários cadastrados, o primeiro envia `POST /api/profiles/{username}/follow` e depois `DELETE /api/profiles/{username}/follow`, verificando `following` igual a `true` e depois `false`.

**Acceptance Scenarios**:

1. **Given** que `ana` (autenticada) não segue `jake`, **When** ela envia `POST /api/profiles/jake/follow`, **Then** o sistema responde `200` com o objeto `profile` de `jake` e `following` igual a `true`.
2. **Given** que `ana` já segue `jake`, **When** ela envia `POST /api/profiles/jake/follow` novamente, **Then** o sistema responde `200` com `following` igual a `true` e não cria uma relação duplicada.
3. **Given** que `ana` segue `jake`, **When** ela envia `DELETE /api/profiles/jake/follow`, **Then** o sistema responde `200` com `following` igual a `false`.
4. **Given** que `ana` não segue `jake`, **When** ela envia `DELETE /api/profiles/jake/follow`, **Then** o sistema responde `200` com `following` igual a `false`.
5. **Given** que não existe usuário `fulano`, **When** `ana` envia `POST` ou `DELETE` para `/api/profiles/fulano/follow`, **Then** o sistema responde `404` com `{"errors":{"profile":["not found"]}}`.
6. **Given** uma requisição sem o cabeçalho `Authorization`, **When** ela é enviada para `POST` ou `DELETE /api/profiles/jake/follow`, **Then** o sistema responde `401` com `{"errors":{"token":["is missing"]}}`.

---

### Edge Cases

- Cabeçalho `Authorization` com esquema diferente de `Token` (por exemplo, `Bearer <token>`), token malformado, com assinatura inválida, expirado ou de um usuário que não existe mais: tratado como token inválido, com `401`.
- Token inválido enviado para `GET /api/profiles/{username}` (autenticação opcional): `401`, conforme a especificação OpenAPI; a ausência do cabeçalho, por outro lado, é aceita.
- Corpo da requisição ausente, JSON malformado ou sem o objeto raiz `user`: `422` no formato de erro do contrato.
- Campos obrigatórios presentes mas vazios ou só com espaços (`""`, `"   "`): tratados como ausentes, com `422` e a mensagem `can't be blank`.
- E-mail com formato inválido no cadastro ou na atualização: `422` com `{"errors":{"email":["is invalid"]}}`.
- `PUT /api/user` com objeto `user` sem nenhum campo aceito: `422`.
- `PUT /api/user` com `bio` ou `image` iguais a `null`: o campo é limpo e passa a ser retornado como `null`.
- `PUT /api/user` com `email`, `username` ou `password` iguais a `null` ou vazios: `422`, pois esses campos não podem ficar vazios.
- `PUT /api/user` alterando `username` para um nome já usado por outra conta: `422` com `{"errors":{"username":["has already been taken"]}}`.
- `PUT /api/user` mantendo o próprio e-mail ou o próprio nome de usuário: aceito, sem erro de duplicidade.
- Cadastro com e-mail que difere de um já existente apenas em maiúsculas e minúsculas: tratado como duplicado (`409`).
- Usuário autenticado tentando seguir a si mesmo: `422` com `{"errors":{"profile":["cannot follow yourself"]}}`.
- Vários erros de validação na mesma requisição: todos são retornados juntos no objeto `errors`, um item por campo.

## Requirements *(mandatory)*

### Functional Requirements

Requisitos em notação EARS. Os formatos `User`, `Profile` e de erro são os definidos na seção "Formatos de resposta" abaixo.

**Cadastro (`POST /api/users`)**

- **FR-001**: Quando receber `POST /api/users` com `user.username`, `user.email` e `user.password` válidos e ainda não usados, o sistema deve criar a conta e responder `201` com o objeto `User` da nova conta, com `bio` e `image` iguais a `null` e um `token` válido.
- **FR-002**: Se o `user.username` informado em `POST /api/users` já pertencer a outra conta, então o sistema deve responder `409` com `{"errors":{"username":["has already been taken"]}}`.
- **FR-003**: Se o `user.email` informado em `POST /api/users` já pertencer a outra conta, comparado sem diferenciar maiúsculas e minúsculas, então o sistema deve responder `409` com `{"errors":{"email":["has already been taken"]}}`.
- **FR-004**: Se `user.username`, `user.email` ou `user.password` estiver ausente, nulo ou vazio em `POST /api/users`, então o sistema deve responder `422` com `{"errors":{"<campo>":["can't be blank"]}}` para cada campo nessa situação.

**Login (`POST /api/users/login`)**

- **FR-005**: Quando receber `POST /api/users/login` com `user.email` e `user.password` que correspondam a uma conta, o sistema deve responder `200` com o objeto `User` dessa conta, incluindo um `token` válido.
- **FR-006**: Se o e-mail informado em `POST /api/users/login` não pertencer a nenhuma conta ou a senha não corresponder, então o sistema deve responder `401` com `{"errors":{"email or password":["is invalid"]}}`, sem indicar qual dos dois está errado.
- **FR-007**: Se `user.email` ou `user.password` estiver ausente, nulo ou vazio em `POST /api/users/login`, então o sistema deve responder `422` com `{"errors":{"<campo>":["can't be blank"]}}` para cada campo nessa situação.

**Autenticação por token**

- **FR-008**: O sistema deve reconhecer a pessoa autenticada pelo cabeçalho `Authorization: Token <token>`, em que `<token>` é um token emitido pelo próprio sistema no cadastro, no login ou na consulta do usuário.
- **FR-009**: O sistema deve emitir tokens assinados, com identificação do usuário e prazo de validade, que não possam ser alterados sem que a alteração seja detectada.
- **FR-010**: Se uma requisição a um endpoint que exige autenticação (`GET /api/user`, `PUT /api/user`, `POST /api/profiles/{username}/follow`, `DELETE /api/profiles/{username}/follow`) não tiver o cabeçalho `Authorization`, então o sistema deve responder `401` com `{"errors":{"token":["is missing"]}}`.
- **FR-011**: Se o cabeçalho `Authorization` de qualquer requisição não seguir o esquema `Token <token>`, ou o token for malformado, tiver assinatura inválida, estiver expirado ou identificar um usuário inexistente, então o sistema deve responder `401` com `{"errors":{"token":["is invalid"]}}`.

**Usuário autenticado (`GET /api/user` e `PUT /api/user`)**

- **FR-012**: Quando um usuário autenticado enviar `GET /api/user`, o sistema deve responder `200` com o objeto `User` desse usuário, incluindo um `token` válido.
- **FR-013**: Quando um usuário autenticado enviar `PUT /api/user` com um ou mais dos campos `email`, `username`, `password`, `bio` e `image` no objeto `user`, o sistema deve alterar somente os campos informados e responder `200` com o objeto `User` atualizado, incluindo um `token` válido.
- **FR-014**: Quando `PUT /api/user` alterar a senha, o sistema deve passar a aceitar no login somente a nova senha.
- **FR-015**: Quando `PUT /api/user` informar `bio` ou `image` iguais a `null`, o sistema deve limpar o campo e retorná-lo como `null`.
- **FR-016**: Se `PUT /api/user` tiver o objeto `user` sem nenhum dos campos aceitos, então o sistema deve responder `422` com `{"errors":{"user":["can't be blank"]}}`.
- **FR-017**: Se `PUT /api/user` informar `email`, `username` ou `password` nulo ou vazio, então o sistema deve responder `422` com `{"errors":{"<campo>":["can't be blank"]}}` para cada campo nessa situação.
- **FR-018**: Se `PUT /api/user` informar `email` ou `username` já pertencente a outra conta, então o sistema deve responder `422` com `{"errors":{"<campo>":["has already been taken"]}}`.

**Perfis (`GET /api/profiles/{username}`)**

- **FR-019**: Quando receber `GET /api/profiles/{username}` sem cabeçalho `Authorization` para um usuário existente, o sistema deve responder `200` com o objeto `Profile` desse usuário e `following` igual a `false`.
- **FR-020**: Quando um usuário autenticado enviar `GET /api/profiles/{username}` para um usuário existente, o sistema deve responder `200` com o objeto `Profile`, com `following` igual a `true` se o autenticado segue esse usuário e `false` caso contrário.
- **FR-021**: Se o `{username}` de `GET /api/profiles/{username}`, `POST /api/profiles/{username}/follow` ou `DELETE /api/profiles/{username}/follow` não corresponder a nenhum usuário, então o sistema deve responder `404` com `{"errors":{"profile":["not found"]}}`.

**Seguir e deixar de seguir (`/api/profiles/{username}/follow`)**

- **FR-022**: Quando um usuário autenticado enviar `POST /api/profiles/{username}/follow` para outro usuário existente, o sistema deve registrar que o autenticado segue esse usuário e responder `200` com o objeto `Profile` dele e `following` igual a `true`.
- **FR-023**: Enquanto o usuário autenticado já seguir o usuário alvo, o sistema deve responder a `POST /api/profiles/{username}/follow` com `200` e `following` igual a `true`, sem criar relação duplicada.
- **FR-024**: Quando um usuário autenticado enviar `DELETE /api/profiles/{username}/follow` para um usuário existente, o sistema deve remover a relação de seguir, se existir, e responder `200` com o objeto `Profile` dele e `following` igual a `false`.
- **FR-025**: Se o usuário autenticado enviar `POST /api/profiles/{username}/follow` com o próprio nome de usuário, então o sistema deve responder `422` com `{"errors":{"profile":["cannot follow yourself"]}}`.

**Validação, formato e segurança**

- **FR-026**: Se o corpo de `POST /api/users`, `POST /api/users/login` ou `PUT /api/user` estiver ausente, não for JSON válido ou não tiver o objeto raiz `user`, então o sistema deve responder `422` com `{"errors":{"body":["can't be empty"]}}`.
- **FR-027**: Se `user.email` informado em `POST /api/users` ou `PUT /api/user` não tiver formato de e-mail válido, então o sistema deve responder `422` com `{"errors":{"email":["is invalid"]}}`.
- **FR-028**: Se uma requisição tiver mais de um erro de validação, então o sistema deve responder `422` com todos os erros no objeto `errors`, agrupados por campo.
- **FR-029**: O sistema deve retornar todas as respostas com corpo, de sucesso e de erro, com o cabeçalho `Content-Type: application/json; charset=utf-8`.
- **FR-030**: O sistema deve retornar todos os erros no formato `{"errors":{"<chave>":["<mensagem>", ...]}}`, sem expor detalhes internos como rastros de pilha ou nomes de tabelas.
- **FR-031**: O sistema deve armazenar as senhas somente na forma de *hash* com sal, nunca em texto puro.
- **FR-032**: O sistema não deve incluir a senha nem o seu *hash* em nenhuma resposta.

### Formatos de resposta

- **User**: `{"user":{"email":string,"token":string,"username":string,"bio":string|null,"image":string|null}}` — todos os campos sempre presentes.
- **Profile**: `{"profile":{"username":string,"bio":string|null,"image":string|null,"following":boolean}}` — todos os campos sempre presentes.
- **Erro**: `{"errors":{"<chave>":[string, ...]}}`.

### Key Entities

- **Usuário**: conta de uma pessoa na plataforma. Atributos: nome de usuário (único, sensível a maiúsculas e minúsculas), e-mail (único, sem diferenciar maiúsculas e minúsculas), senha (guardada só como *hash*), biografia (opcional) e imagem (URL opcional). Não pode ser excluído neste recorte.
- **Relação de seguir**: indica que um usuário (seguidor) segue outro (seguido). Cada par seguidor–seguido existe no máximo uma vez, e um usuário não segue a si mesmo.
- **Token de acesso**: credencial emitida pelo sistema que identifica o usuário e tem prazo de validade. Não é armazenada como dado do usuário.
- **Perfil**: visão pública de um usuário (nome de usuário, biografia, imagem), acrescida de `following` em relação a quem consulta.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos 7 endpoints do recorte respondem com as rotas, os códigos de status e os formatos JSON definidos no contrato, verificados por uma suíte de testes de API que cobre todos os cenários de aceitação desta especificação.
- **SC-002**: Uma pessoa consegue se cadastrar e, com o token recebido, consultar os próprios dados em no máximo 2 requisições, sem nenhuma etapa adicional.
- **SC-003**: 100% das requisições a endpoints autenticados sem token ou com token inválido são recusadas com `401`, e nenhuma delas lê ou altera dados.
- **SC-004**: 100% das respostas de erro seguem o formato `{"errors":{...}}` e nenhuma resposta contém senha, *hash* de senha ou detalhes internos.
- **SC-005**: 95% das requisições são respondidas em menos de 1 segundo com até 50 requisições simultâneas em ambiente local.
- **SC-006**: Seguir ou deixar de seguir o mesmo usuário repetidas vezes sempre termina no estado esperado (`following` verdadeiro após seguir, falso após deixar de seguir), sem relações duplicadas.

## Assumptions

- O contrato de `descricao-base.md` prevalece; onde a prosa e a OpenAPI divergem (por exemplo, `201` no cadastro), vale a OpenAPI.
- Conflitos de e-mail ou nome de usuário no cadastro usam `409` (previsto na OpenAPI para `POST /api/users`); na atualização usam `422`, pois a OpenAPI não prevê `409` para `PUT /api/user`.
- Credenciais inválidas no login usam `401` (previsto na OpenAPI) com a chave `email or password`, mensagem usual das implementações de referência do RealWorld.
- As mensagens de erro seguem o estilo do contrato (`can't be blank`, `has already been taken`, `is invalid`, `is missing`, `not found`), em inglês.
- A chave de erro de recurso inexistente é `profile`, conforme a OpenAPI ("a chave identifica o tipo de recurso").
- O token tem validade de 24 horas, configurável; a chave de assinatura vem da configuração externa, nunca do código.
- Um token emitido continua válido após o usuário alterar o nome de usuário ou o e-mail, pois identifica o usuário de forma estável.
- Não há regras de tamanho mínimo de senha nem de formato de nome de usuário além de não serem vazios, pois o contrato não as define.
- Seguir a si mesmo é rejeitado com `422`; o contrato não define esse caso e essa é a interpretação mais conservadora.
- O código `403` não tem cenário de uso neste recorte, pois cada usuário só altera os próprios dados por meio do token.
- Não há listagem de usuários, exclusão de conta, recuperação de senha nem confirmação de e-mail.
- Artigos, feed, comentários, favoritos e tags estão fora do escopo.
