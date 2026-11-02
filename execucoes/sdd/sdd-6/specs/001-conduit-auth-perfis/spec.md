# Feature Specification: Conduit — Recorte A (autenticação, usuário e perfis)

**Feature Branch**: `001-conduit-auth-perfis`

**Created**: 2026-11-02

**Status**: Draft

**Input**: User description: "Use o arquivo descricao-base.md, na raiz do projeto, como única fonte de requisitos."

> Fonte única de requisitos: `descricao-base.md` (Recorte A da API Conduit/RealWorld). O contrato da API descrito ali (rotas, formatos JSON, códigos de status e formato de erro) é obrigatório e, em caso de divergência entre a prosa e a especificação OpenAPI, vale a especificação OpenAPI.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastro e login (Priority: P1)

Uma pessoa visitante cria uma conta na plataforma informando nome de usuário, e-mail e senha, e recebe de volta seus dados com um token de acesso. Depois, consegue entrar novamente informando e-mail e senha, recebendo um novo token.

**Why this priority**: Sem cadastro e login não existe usuário autenticado; todas as demais histórias dependem do token obtido aqui.

**Independent Test**: Cadastrar um usuário em `POST /api/users`, fazer login em `POST /api/users/login` e verificar que ambas as respostas trazem o objeto `user` com `email`, `token`, `username`, `bio` e `image`.

**Acceptance Scenarios**:

1. **Given** que não existe usuário com o e-mail e o nome de usuário informados, **When** a visitante envia `POST /api/users` com `username`, `email` e `password`, **Then** recebe `201` com `{"user": {"email", "token", "username", "bio": null, "image": null}}`.
2. **Given** um usuário cadastrado, **When** envia `POST /api/users/login` com e-mail e senha corretos, **Then** recebe `200` com o objeto `user` e um token válido.
3. **Given** um usuário cadastrado, **When** envia `POST /api/users/login` com senha incorreta ou e-mail inexistente, **Then** recebe `401` com corpo no formato `{"errors": {...}}`.
4. **Given** que já existe usuário com o mesmo e-mail ou nome de usuário, **When** a visitante tenta se cadastrar, **Then** recebe `409` com corpo `{"errors": {"<campo>": ["has already been taken"]}}`.
5. **Given** qualquer estado, **When** a visitante envia cadastro ou login sem um campo obrigatório ou com campo vazio, **Then** recebe `422` com corpo `{"errors": {"<campo>": ["can't be blank"]}}`.

---

### User Story 2 - Consultar e atualizar o próprio usuário (Priority: P2)

Uma pessoa autenticada consulta seus próprios dados e atualiza e-mail, nome de usuário, senha, biografia e/ou imagem.

**Why this priority**: Permite manter a conta e preencher o perfil público; depende apenas da história 1.

**Independent Test**: Com um token obtido no cadastro, chamar `GET /api/user` e `PUT /api/user` alterando `bio` e `image`, e confirmar que a resposta e uma nova consulta refletem os valores alterados.

**Acceptance Scenarios**:

1. **Given** um token válido no cabeçalho `Authorization: Token <jwt>`, **When** a pessoa envia `GET /api/user`, **Then** recebe `200` com o objeto `user` do usuário autenticado, incluindo um token.
2. **Given** um token válido, **When** envia `PUT /api/user` com `{"user": {"bio": "...", "image": "..."}}`, **Then** recebe `200` com o objeto `user` já atualizado, e os campos não enviados permanecem inalterados.
3. **Given** um token válido, **When** atualiza a senha e depois faz login com a nova senha, **Then** o login é aceito; com a senha antiga, é recusado com `401`.
4. **Given** nenhum token ou um token inválido/expirado, **When** chama `GET /api/user` ou `PUT /api/user`, **Then** recebe `401`.
5. **Given** um token válido, **When** tenta atualizar e-mail ou nome de usuário para um valor já usado por outra pessoa, **Then** recebe `409`.

---

### User Story 3 - Consultar perfil público (Priority: P3)

Qualquer pessoa, autenticada ou não, consulta o perfil público de um usuário pelo nome de usuário e vê nome, biografia, imagem e se o segue.

**Why this priority**: Dá visibilidade aos usuários da plataforma e é pré-requisito para a experiência de seguir.

**Independent Test**: Cadastrar um usuário e chamar `GET /api/profiles/{username}` sem token, verificando o objeto `profile` com `following: false`.

**Acceptance Scenarios**:

1. **Given** um usuário existente, **When** uma visitante sem token envia `GET /api/profiles/{username}`, **Then** recebe `200` com `{"profile": {"username", "bio", "image", "following": false}}`.
2. **Given** um usuário existente seguido pela pessoa autenticada, **When** ela consulta o perfil com seu token, **Then** recebe `200` com `following: true`.
3. **Given** um nome de usuário inexistente, **When** consulta o perfil, **Then** recebe `404` com corpo no formato `{"errors": {"profile": ["not found"]}}`.

---

### User Story 4 - Seguir e deixar de seguir (Priority: P4)

Uma pessoa autenticada segue outro usuário e, depois, deixa de segui-lo.

**Why this priority**: Completa o recorte social; depende das histórias 1 e 3.

**Independent Test**: Com dois usuários cadastrados, o primeiro chama `POST /api/profiles/{segundo}/follow` e recebe `following: true`; depois chama `DELETE /api/profiles/{segundo}/follow` e recebe `following: false`.

**Acceptance Scenarios**:

1. **Given** um token válido e um usuário-alvo existente, **When** envia `POST /api/profiles/{username}/follow`, **Then** recebe `200` com o objeto `profile` do alvo e `following: true`.
2. **Given** que a pessoa já segue o alvo, **When** envia `DELETE /api/profiles/{username}/follow`, **Then** recebe `200` com `following: false`, e consultas posteriores ao perfil também mostram `following: false`.
3. **Given** que a pessoa já segue o alvo, **When** envia novamente `POST .../follow`, **Then** recebe `200` com `following: true`, sem duplicar a relação.
4. **Given** nenhum token ou token inválido, **When** chama `POST` ou `DELETE` em `/api/profiles/{username}/follow`, **Then** recebe `401`.
5. **Given** um token válido e um nome de usuário inexistente, **When** chama `POST` ou `DELETE` em `.../follow`, **Then** recebe `404`.

---

### Edge Cases

- Cabeçalho `Authorization` em formato diferente de `Token <jwt>` (por exemplo, `Bearer <jwt>`), token malformado, com assinatura inválida ou expirado: tratado como token inválido (`401`).
- Consulta de perfil (autenticação opcional) com token inválido: `401`, em vez de ser tratada como anônima.
- Corpo JSON ausente, malformado ou sem o objeto raiz `user`: `422`.
- `PUT /api/user` com objeto `user` sem nenhum campo: `422` (o contrato exige ao menos um campo).
- `PUT /api/user` enviando `bio` ou `image` como `null`: o campo passa a ser `null`.
- `PUT /api/user` enviando o próprio e-mail ou nome de usuário atuais: aceito, sem conflito.
- Deixar de seguir quem não é seguido: `200` com `following: false` (operação idempotente).
- Seguir a si mesmo: sem regra específica no contrato; ver Assumptions.
- O token emitido nunca contém a senha; a senha nunca aparece em nenhuma resposta.

## Requirements *(mandatory)*

### Functional Requirements

**Geral e formato**

- **FR-001**: O sistema deve expor todos os endpoints do recorte sob o prefixo `/api`.
- **FR-002**: O sistema deve responder com `Content-Type: application/json; charset=utf-8` em todas as respostas com corpo.
- **FR-003**: O sistema deve representar o usuário autenticado no formato `{"user": {"email", "token", "username", "bio", "image"}}`, com `bio` e `image` presentes mesmo quando `null`.
- **FR-004**: O sistema deve representar um perfil no formato `{"profile": {"username", "bio", "image", "following"}}`, com `bio` e `image` presentes mesmo quando `null` e `following` booleano.
- **FR-005**: O sistema deve representar todo erro no formato `{"errors": {"<chave>": ["<mensagem>", ...]}}`.
- **FR-006**: O sistema não deve incluir a senha, nem seu hash, em nenhuma resposta.

**Autenticação**

- **FR-007**: Quando uma requisição trouxer o cabeçalho `Authorization: Token <jwt>` com um JWT válido emitido pelo sistema, o sistema deve identificar o usuário autenticado a partir desse token.
- **FR-008**: Se um endpoint de autenticação obrigatória (`GET /api/user`, `PUT /api/user`, `POST /api/profiles/{username}/follow`, `DELETE /api/profiles/{username}/follow`) for chamado sem o cabeçalho `Authorization`, então o sistema deve responder `401` com `{"errors": {"token": ["is missing"]}}`.
- **FR-009**: Se uma requisição trouxer o cabeçalho `Authorization` fora do formato `Token <jwt>`, ou com token malformado, de assinatura inválida, expirado ou referente a usuário inexistente, então o sistema deve responder `401` com corpo no formato de erro.
- **FR-010**: O sistema deve armazenar senhas apenas de forma irreversível (hash com sal), nunca em texto puro.
- **FR-011**: O sistema deve emitir tokens JWT assinados e com prazo de expiração.

**Cadastro — `POST /api/users`**

- **FR-012**: Quando receber `POST /api/users` com `user.username`, `user.email` e `user.password` válidos e únicos, o sistema deve criar o usuário e responder `201` com o objeto `user`, contendo um token válido e `bio` e `image` iguais a `null`.
- **FR-013**: Se `POST /api/users` vier sem `username`, `email` ou `password`, ou com algum deles vazio, então o sistema deve responder `422` com `{"errors": {"<campo>": ["can't be blank"]}}` para cada campo inválido.
- **FR-014**: Se `POST /api/users` informar um `email` com formato inválido, então o sistema deve responder `422` com `{"errors": {"email": ["is invalid"]}}`.
- **FR-015**: Se `POST /api/users` informar um `email` já cadastrado, então o sistema deve responder `409` com `{"errors": {"email": ["has already been taken"]}}`.
- **FR-016**: Se `POST /api/users` informar um `username` já cadastrado, então o sistema deve responder `409` com `{"errors": {"username": ["has already been taken"]}}`.

**Login — `POST /api/users/login`**

- **FR-017**: Quando receber `POST /api/users/login` com `user.email` e `user.password` que correspondam a um usuário cadastrado, o sistema deve responder `200` com o objeto `user` contendo um novo token válido.
- **FR-018**: Se `POST /api/users/login` vier sem `email` ou `password`, ou com algum deles vazio, então o sistema deve responder `422` com `{"errors": {"<campo>": ["can't be blank"]}}`.
- **FR-019**: Se o e-mail informado em `POST /api/users/login` não existir ou a senha não conferir, então o sistema deve responder `401` com `{"errors": {"credentials": ["invalid"]}}`, sem revelar qual dos dois está errado.

**Usuário atual — `GET /api/user` e `PUT /api/user`**

- **FR-020**: Quando um usuário autenticado enviar `GET /api/user`, o sistema deve responder `200` com o objeto `user` do próprio usuário, incluindo um token válido.
- **FR-021**: Quando um usuário autenticado enviar `PUT /api/user` com um ou mais dos campos `email`, `username`, `password`, `bio` e `image`, o sistema deve atualizar somente os campos enviados e responder `200` com o objeto `user` atualizado.
- **FR-022**: Quando `PUT /api/user` alterar a senha, o sistema deve passar a aceitar somente a nova senha no login.
- **FR-023**: Quando `PUT /api/user` enviar `bio` ou `image` com valor `null`, o sistema deve gravar o campo como `null`.
- **FR-024**: Se `PUT /api/user` vier com o objeto `user` sem nenhum dos campos aceitos, então o sistema deve responder `422` com corpo no formato de erro.
- **FR-025**: Se `PUT /api/user` enviar `email`, `username` ou `password` vazios, ou `email` em formato inválido, então o sistema deve responder `422` com corpo no formato de erro identificando o campo.
- **FR-026**: Se `PUT /api/user` enviar `email` ou `username` já usado por outro usuário, então o sistema deve responder `409` com `{"errors": {"<campo>": ["has already been taken"]}}`.

**Perfis — `GET /api/profiles/{username}`**

- **FR-027**: Quando receber `GET /api/profiles/{username}` para um usuário existente sem cabeçalho `Authorization`, o sistema deve responder `200` com o objeto `profile` e `following: false`.
- **FR-028**: Enquanto a requisição a `GET /api/profiles/{username}` estiver autenticada, o sistema deve informar em `following` se o usuário autenticado segue o usuário consultado.
- **FR-029**: Se `GET /api/profiles/{username}` referir um usuário inexistente, então o sistema deve responder `404` com `{"errors": {"profile": ["not found"]}}`.

**Seguir — `POST` e `DELETE /api/profiles/{username}/follow`**

- **FR-030**: Quando um usuário autenticado enviar `POST /api/profiles/{username}/follow` para um usuário existente, o sistema deve registrar que o autenticado segue o alvo e responder `200` com o objeto `profile` do alvo e `following: true`.
- **FR-031**: Quando um usuário autenticado enviar `POST /api/profiles/{username}/follow` para um alvo que já segue, o sistema deve responder `200` com `following: true` sem duplicar a relação.
- **FR-032**: Quando um usuário autenticado enviar `DELETE /api/profiles/{username}/follow` para um usuário existente, o sistema deve remover a relação de seguir, se existir, e responder `200` com o objeto `profile` do alvo e `following: false`.
- **FR-033**: Se `POST` ou `DELETE /api/profiles/{username}/follow` referir um usuário inexistente, então o sistema deve responder `404` com `{"errors": {"profile": ["not found"]}}`.

**Erros gerais**

- **FR-034**: Se o corpo de uma requisição for JSON malformado ou não trouxer o objeto raiz `user` exigido pelo endpoint, então o sistema deve responder `422` com corpo no formato de erro.
- **FR-035**: Se ocorrer um erro inesperado, então o sistema deve responder com corpo no formato de erro, sem expor detalhes internos (rastreamento de pilha, nomes de tabelas, consultas).

### Key Entities

- **Usuário**: pessoa cadastrada na plataforma. Atributos: e-mail (único), nome de usuário (único), senha (armazenada só como hash), biografia (opcional), imagem (URL opcional).
- **Relação de seguir**: vínculo direcionado entre um usuário seguidor e um usuário seguido; no máximo uma relação por par.
- **Token de acesso**: credencial emitida no cadastro, no login e na consulta/atualização do usuário atual; identifica o usuário e tem prazo de validade.
- **Perfil**: visão pública de um usuário (nome de usuário, biografia, imagem) acrescida de `following`, calculado em relação a quem consulta.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos 7 endpoints do recorte respondem com as rotas, os formatos JSON e os códigos de status definidos no contrato, verificados por uma suíte de testes de API.
- **SC-002**: Uma pessoa nova consegue se cadastrar, entrar, completar a biografia e a imagem e seguir outro usuário em menos de 5 requisições após o cadastro, sem erro.
- **SC-003**: 100% das requisições a endpoints protegidos sem token, ou com token inválido, são recusadas com `401`.
- **SC-004**: 100% das respostas de erro seguem o formato `{"errors": {...}}` e nenhuma resposta expõe senha, hash de senha ou detalhes internos.
- **SC-005**: 95% das requisições do recorte são respondidas em menos de 1 segundo com o sistema em uso normal (até 50 usuários simultâneos).

## Assumptions

- O escopo é apenas o recorte A: cadastro, login, consulta e atualização do usuário atual, perfil público e seguir/deixar de seguir. Artigos, feed, comentários, favoritos, tags e exclusão de usuário estão fora do escopo.
- Onde a prosa e a OpenAPI divergem, vale a OpenAPI: o cadastro responde `201` (não `200`) e conflitos de e-mail/nome de usuário respondem `409` (não `422`).
- O código `403` previsto no contrato não tem caso de uso neste recorte, pois cada usuário só altera os próprios dados.
- As chaves e mensagens de erro (`"can't be blank"`, `"is invalid"`, `"has already been taken"`, `"not found"`, `"is missing"`, `credentials: ["invalid"]`) seguem os exemplos do contrato e as convenções do RealWorld; testes devem verificar código de status e formato `{"errors": {...}}`.
- E-mails são comparados sem diferenciar maiúsculas de minúsculas; nomes de usuário são comparados exatamente como digitados.
- Seguir a si mesmo não tem regra específica no contrato; o sistema trata como qualquer outro alvo existente (sem bloqueio adicional).
- O token tem validade padrão de 24 horas; o valor exato é configurável e não faz parte do contrato.
- Não há recuperação de senha, confirmação de e-mail, logout no servidor nem revogação de tokens neste recorte.
- O banco de dados e a infraestrutura seguem as regras técnicas do projeto (`.claude/rules/`), que não fazem parte desta especificação funcional.
