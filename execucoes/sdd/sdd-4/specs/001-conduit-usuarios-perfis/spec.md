# Feature Specification: Conduit — Recorte A (usuários, autenticação e perfis)

**Feature Branch**: `001-conduit-usuarios-perfis`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Use o arquivo descricao-base.md, na raiz do projeto, como única fonte de requisitos."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastro e login (Priority: P1)

Uma pessoa visitante cria uma conta na plataforma informando nome de usuário, e-mail e senha, e recebe de volta seus dados com um token de acesso. Depois, pode entrar novamente informando e-mail e senha e receber um novo token.

**Why this priority**: Sem conta e sem token nenhuma outra funcionalidade autenticada do recorte pode ser usada. É o MVP mínimo.

**Independent Test**: Cadastrar um usuário em `POST /api/users`, fazer login em `POST /api/users/login` com as mesmas credenciais e verificar que ambas as respostas trazem o objeto `user` com `token`.

**Acceptance Scenarios**:

1. **Given** que não existe usuário com o e-mail e o nome de usuário informados, **When** a visitante envia `POST /api/users` com `username`, `email` e `password`, **Then** recebe `201` com `{"user": {"email", "token", "username", "bio": null, "image": null}}`.
2. **Given** um usuário cadastrado, **When** envia `POST /api/users/login` com e-mail e senha corretos, **Then** recebe `200` com o objeto `user` contendo um token válido.
3. **Given** um usuário cadastrado, **When** envia login com senha errada ou e-mail inexistente, **Then** recebe `401` no formato de erro padrão.
4. **Given** que já existe um usuário com o mesmo nome de usuário (ou e-mail), **When** outra visitante tenta se cadastrar com ele, **Then** recebe `409` com `{"errors": {"username": ["has already been taken"]}}` (ou a chave `email`).
5. **Given** um corpo de cadastro sem `email`, `username` ou `password`, **When** é enviado, **Then** recebe `422` com o campo ausente listado em `errors` e a mensagem `can't be blank`.

---

### User Story 2 - Consultar e atualizar o próprio usuário (Priority: P2)

Um usuário autenticado consulta seus próprios dados e atualiza e-mail, nome de usuário, senha, biografia ou imagem.

**Why this priority**: Completa o ciclo da conta do usuário; depende apenas do P1.

**Independent Test**: Com um token obtido no cadastro, chamar `GET /api/user`, depois `PUT /api/user` alterando `bio` e `image`, e conferir que um novo `GET /api/user` reflete as alterações.

**Acceptance Scenarios**:

1. **Given** um usuário autenticado, **When** envia `GET /api/user` com `Authorization: Token <jwt>`, **Then** recebe `200` com seus dados no formato `user`, incluindo um token.
2. **Given** um usuário autenticado, **When** envia `PUT /api/user` com `bio` e `image`, **Then** recebe `200` com os novos valores e os demais campos inalterados.
3. **Given** um usuário autenticado, **When** altera a senha via `PUT /api/user`, **Then** o login passa a funcionar apenas com a nova senha.
4. **Given** uma requisição sem cabeçalho de autenticação, **When** chama `GET /api/user` ou `PUT /api/user`, **Then** recebe `401`.
5. **Given** um usuário autenticado, **When** tenta trocar o e-mail ou o nome de usuário para um já usado por outro usuário, **Then** recebe `422` com `has already been taken` na chave do campo.

---

### User Story 3 - Consultar perfil público (Priority: P3)

Qualquer pessoa, autenticada ou não, consulta o perfil público de um usuário pelo nome de usuário e vê nome, biografia, imagem e se o segue.

**Why this priority**: Expõe os dados públicos e é pré-requisito para a experiência de seguir.

**Independent Test**: Cadastrar um usuário e chamar `GET /api/profiles/{username}` sem token, verificando `following: false`.

**Acceptance Scenarios**:

1. **Given** um usuário existente, **When** uma pessoa não autenticada envia `GET /api/profiles/{username}`, **Then** recebe `200` com `{"profile": {"username", "bio", "image", "following": false}}`.
2. **Given** um usuário autenticado que segue o perfil consultado, **When** envia `GET /api/profiles/{username}` com token, **Then** recebe `following: true`.
3. **Given** um nome de usuário inexistente, **When** o perfil é consultado, **Then** recebe `404` no formato de erro padrão.

---

### User Story 4 - Seguir e deixar de seguir (Priority: P4)

Um usuário autenticado segue outro usuário e, depois, deixa de segui-lo.

**Why this priority**: Funcionalidade social do recorte; depende de P1 e P3.

**Independent Test**: Cadastrar dois usuários, com o token do primeiro chamar `POST /api/profiles/{segundo}/follow` e depois `DELETE /api/profiles/{segundo}/follow`, verificando `following` em cada resposta.

**Acceptance Scenarios**:

1. **Given** um usuário autenticado que não segue o usuário-alvo, **When** envia `POST /api/profiles/{username}/follow`, **Then** recebe `200` com o perfil do alvo e `following: true`.
2. **Given** um usuário autenticado que segue o usuário-alvo, **When** envia `DELETE /api/profiles/{username}/follow`, **Then** recebe `200` com `following: false`.
3. **Given** um usuário que já segue o alvo, **When** envia `POST .../follow` de novo, **Then** recebe `200` com `following: true` e nenhuma relação duplicada é criada.
4. **Given** uma requisição sem token, **When** chama `POST` ou `DELETE .../follow`, **Then** recebe `401`.
5. **Given** um nome de usuário inexistente, **When** chama `POST` ou `DELETE .../follow` autenticado, **Then** recebe `404`.

---

### Edge Cases

- Corpo JSON malformado, sem o objeto raiz `user` ou com campos obrigatórios vazios/só com espaços → `422`.
- `PUT /api/user` sem nenhum campo dentro de `user` → `422`.
- `PUT /api/user` enviando `email`, `username` ou `password` vazios → `422`; enviando `bio` ou `image` como `null` → o valor é apagado.
- Cabeçalho `Authorization` com esquema diferente de `Token`, token malformado, com assinatura inválida, expirado ou de usuário que não existe mais → `401`.
- `GET /api/profiles/{username}` com token inválido → `401` (a autenticação é opcional, mas, se informada, precisa ser válida).
- Usuário tentando seguir a si mesmo → `422`.
- Deixar de seguir quem não é seguido → `200` com `following: false` (operação idempotente).
- E-mail com letras maiúsculas e minúsculas diferentes de um já cadastrado é tratado como o mesmo e-mail.

## Requirements *(mandatory)*

### Functional Requirements

**Autenticação e formato geral**

- **FR-001**: O sistema deve aceitar a autenticação somente pelo cabeçalho `Authorization: Token <jwt>`.
- **FR-002**: O sistema deve devolver todas as respostas com corpo JSON com `Content-Type: application/json; charset=utf-8`.
- **FR-003**: O sistema deve representar todo erro no formato `{"errors": {"<chave>": ["<mensagem>", ...]}}`.
- **FR-004**: Se uma rota que exige autenticação for chamada sem o cabeçalho `Authorization`, então o sistema deve responder `401` com `{"errors": {"token": ["is missing"]}}`.
- **FR-005**: Se o token informado for malformado, tiver assinatura inválida, estiver expirado ou pertencer a um usuário inexistente, então o sistema deve responder `401` no formato de erro padrão, inclusive em rotas de autenticação opcional.
- **FR-006**: Se o corpo da requisição for JSON malformado ou não contiver o objeto raiz `user`, então o sistema deve responder `422` no formato de erro padrão.
- **FR-007**: O sistema deve armazenar senhas apenas na forma de *hash* com sal e nunca devolver a senha ou o *hash* em resposta alguma.
- **FR-008**: O sistema deve emitir tokens JWT assinados, que identifiquem o usuário e tenham data de expiração.

**Cadastro — `POST /api/users`**

- **FR-009**: Quando receber `POST /api/users` com `username`, `email` e `password` válidos, o sistema deve criar o usuário e responder `201` com `{"user": {"email", "token", "username", "bio": null, "image": null}}`.
- **FR-010**: Se `username`, `email` ou `password` estiver ausente, nulo ou em branco no cadastro, então o sistema deve responder `422` com `["can't be blank"]` na chave de cada campo inválido.
- **FR-011**: Se o `email` do cadastro não tiver formato de e-mail válido, então o sistema deve responder `422` com `{"errors": {"email": ["is invalid"]}}`.
- **FR-012**: Se o `username` do cadastro já pertencer a outro usuário, então o sistema deve responder `409` com `{"errors": {"username": ["has already been taken"]}}`.
- **FR-013**: Se o `email` do cadastro já pertencer a outro usuário, sem diferenciar maiúsculas de minúsculas, então o sistema deve responder `409` com `{"errors": {"email": ["has already been taken"]}}`.

**Login — `POST /api/users/login`**

- **FR-014**: Quando receber `POST /api/users/login` com `email` e `password` que correspondam a um usuário, o sistema deve responder `200` com o objeto `user`, incluindo um novo token.
- **FR-015**: Se `email` ou `password` estiver ausente, nulo ou em branco no login, então o sistema deve responder `422` com `["can't be blank"]` na chave de cada campo inválido.
- **FR-016**: Se o e-mail não existir ou a senha não conferir no login, então o sistema deve responder `401` com `{"errors": {"credentials": ["invalid"]}}`, sem revelar qual dos dois falhou.

**Usuário atual — `GET /api/user` e `PUT /api/user`**

- **FR-017**: Quando um usuário autenticado enviar `GET /api/user`, o sistema deve responder `200` com seus dados no formato `user`, incluindo um token válido.
- **FR-018**: Quando um usuário autenticado enviar `PUT /api/user` com um ou mais dos campos `email`, `username`, `password`, `bio` e `image`, o sistema deve atualizar somente os campos informados e responder `200` com o objeto `user` atualizado, incluindo um token válido.
- **FR-019**: Se o objeto `user` do `PUT /api/user` não contiver nenhum dos campos aceitos, então o sistema deve responder `422` no formato de erro padrão.
- **FR-020**: Se `email`, `username` ou `password` for enviado nulo ou em branco no `PUT /api/user`, então o sistema deve responder `422` com `["can't be blank"]` na chave do campo.
- **FR-021**: Se o novo `email` do `PUT /api/user` não tiver formato de e-mail válido, então o sistema deve responder `422` com `{"errors": {"email": ["is invalid"]}}`.
- **FR-022**: Se o novo `username` ou `email` do `PUT /api/user` já pertencer a outro usuário, então o sistema deve responder `422` com `["has already been taken"]` na chave do campo.
- **FR-023**: Quando `bio` ou `image` for enviado como `null` no `PUT /api/user`, o sistema deve apagar o valor do campo e devolvê-lo como `null`.
- **FR-024**: Quando a senha for alterada pelo `PUT /api/user`, o sistema deve aceitar no login somente a nova senha.

**Perfil — `GET /api/profiles/{username}`**

- **FR-025**: Quando receber `GET /api/profiles/{username}` de um usuário existente, o sistema deve responder `200` com `{"profile": {"username", "bio", "image", "following"}}`.
- **FR-026**: Enquanto a requisição de perfil não estiver autenticada, o sistema deve devolver `following: false`.
- **FR-027**: Enquanto a requisição de perfil estiver autenticada, o sistema deve devolver `following: true` se o usuário autenticado seguir o perfil consultado e `false` caso contrário.
- **FR-028**: Se o `username` consultado em `/api/profiles/{username}` ou em `/api/profiles/{username}/follow` não existir, então o sistema deve responder `404` com `{"errors": {"profile": ["not found"]}}`.

**Seguir — `POST` e `DELETE /api/profiles/{username}/follow`**

- **FR-029**: Quando um usuário autenticado enviar `POST /api/profiles/{username}/follow`, o sistema deve registrar que ele segue o usuário-alvo e responder `200` com o perfil do alvo e `following: true`.
- **FR-030**: Quando um usuário autenticado enviar `POST /api/profiles/{username}/follow` para um alvo que já segue, o sistema deve responder `200` com `following: true` sem criar relação duplicada.
- **FR-031**: Quando um usuário autenticado enviar `DELETE /api/profiles/{username}/follow`, o sistema deve remover a relação, se existir, e responder `200` com o perfil do alvo e `following: false`.
- **FR-032**: Se um usuário autenticado tentar seguir a si mesmo, então o sistema deve responder `422` com `{"errors": {"profile": ["cannot follow yourself"]}}`.

### Key Entities

- **Usuário**: pessoa cadastrada na plataforma. Atributos: nome de usuário (único), e-mail (único, sem diferenciar maiúsculas), senha (guardada só como *hash*), biografia (opcional) e imagem (URL opcional).
- **Relação de seguir**: vínculo direcionado entre um usuário seguidor e um usuário seguido. Cada par seguidor–seguido existe no máximo uma vez.
- **Perfil**: visão pública de um usuário (nome, biografia, imagem) acrescida do indicador `following`, calculado em relação a quem consulta. Não é armazenado separadamente.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das rotas do recorte (7 operações) respondem com os códigos de status e os formatos JSON definidos no contrato, verificados por uma suíte automatizada de testes de API.
- **SC-002**: Uma pessoa consegue se cadastrar, entrar e consultar os próprios dados em no máximo 3 requisições, cada uma respondida em menos de 1 segundo em ambiente local.
- **SC-003**: 100% das respostas de erro seguem o formato `{"errors": {...}}`; nenhuma resposta expõe senha, *hash*, rastreamento de pilha ou nomes internos.
- **SC-004**: 100% das tentativas de acesso a rotas protegidas sem token ou com token inválido são recusadas com `401`.
- **SC-005**: Seguir ou deixar de seguir o mesmo usuário repetidas vezes nunca gera relação duplicada nem erro.

## Assumptions

- O escopo é apenas o recorte A: cadastro, login, usuário atual, perfil e seguir/deixar de seguir. Artigos, feed, comentários, favoritos, tags e exclusão de usuário estão fora do escopo.
- Quando a prosa e a OpenAPI divergem, vale a OpenAPI. Por isso a mensagem de campo obrigatório é `can't be blank` e duplicidade no `PUT /api/user` responde `422` (a OpenAPI dessa rota não prevê `409`), enquanto no cadastro responde `409`.
- As chaves de erro não fixadas pelo contrato (`credentials` no login inválido, `profile` no 404 e ao seguir a si mesmo) foram escolhidas seguindo o padrão do contrato.
- Seguir a si mesmo é recusado com `422`, por não fazer sentido de negócio; o contrato não define o caso.
- O token expira em 24 horas; não há renovação nem revogação de token neste recorte.
- O nome de usuário é comparado de forma exata (diferencia maiúsculas); o e-mail é comparado sem diferenciar maiúsculas.
- Não há regra mínima de tamanho de senha além de não estar em branco, pois o contrato não define uma.
- O status `403` não se aplica a nenhuma operação deste recorte, pois cada usuário só altera os próprios dados.
- O formato das rotas, códigos de status e JSON é citado nos requisitos porque o contrato da API é o próprio produto (backend sem interface).
