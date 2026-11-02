# Contrato de erros e decisões sobre a OpenAPI

O contrato normativo é [openapi.yaml](openapi.yaml), extraído sem alterações de `descricao-base.md`. Este arquivo fixa o que a OpenAPI deixa em aberto: chaves e mensagens de erro de cada situação. Todas as respostas usam `Content-Type: application/json; charset=utf-8` e o corpo `{"errors":{"<chave>":["<mensagem>", ...]}}`.

Base da API: `http://localhost:8080/api`. Autenticação: cabeçalho `Authorization: Token <jwt>`.

## Matriz de status por endpoint

| Endpoint | Auth | Sucesso | Erros possíveis |
|---|---|---|---|
| `POST /api/users/login` | não | `200` User | `401`, `422` |
| `POST /api/users` | não | `201` User | `409`, `422` |
| `GET /api/user` | obrigatória | `200` User | `401` |
| `PUT /api/user` | obrigatória | `200` User | `401`, `422` |
| `GET /api/profiles/{username}` | opcional | `200` Profile | `401` (token inválido), `404` |
| `POST /api/profiles/{username}/follow` | obrigatória | `200` Profile | `401`, `404`, `422` |
| `DELETE /api/profiles/{username}/follow` | obrigatória | `200` Profile | `401`, `404` |

`POST /api/users` (cadastro) não devolve cabeçalho `Location`: o recurso criado não tem rota de leitura própria fora de `/api/user`, e o contrato não o exige.

## Catálogo de erros

| Situação | Status | Corpo | Requisitos |
|---|---|---|---|
| Corpo ausente, JSON inválido ou sem objeto `user` | 422 | `{"errors":{"body":["can't be empty"]}}` | FR-026 |
| Campo obrigatório ausente, `null`, vazio ou só espaços | 422 | `{"errors":{"<campo>":["can't be blank"]}}` | FR-004, FR-007, FR-017 |
| E-mail com formato inválido | 422 | `{"errors":{"email":["is invalid"]}}` | FR-027 |
| `bio`/`image` com tipo não textual no PUT | 422 | `{"errors":{"<campo>":["is invalid"]}}` | FR-013 |
| PUT sem nenhum campo aceito | 422 | `{"errors":{"user":["can't be blank"]}}` | FR-016 |
| PUT com e-mail/username de outra conta | 422 | `{"errors":{"<campo>":["has already been taken"]}}` | FR-018 |
| Cadastro com e-mail/username existente | 409 | `{"errors":{"<campo>":["has already been taken"]}}` | FR-002, FR-003 |
| Seguir a si mesmo | 422 | `{"errors":{"profile":["cannot follow yourself"]}}` | FR-025 |
| Credenciais de login inválidas | 401 | `{"errors":{"email or password":["is invalid"]}}` | FR-006 |
| Cabeçalho `Authorization` ausente em rota autenticada | 401 | `{"errors":{"token":["is missing"]}}` | FR-010 |
| Esquema ≠ `Token`, token malformado, assinatura inválida, expirado ou de usuário inexistente | 401 | `{"errors":{"token":["is invalid"]}}` | FR-011 |
| Usuário do `{username}` inexistente | 404 | `{"errors":{"profile":["not found"]}}` | FR-021 |
| Acesso negado (sem cenário no recorte) | 403 | `{"errors":{"authorization":["forbidden"]}}` | — |
| Erro inesperado | 500 | `{"errors":{"server":["internal error"]}}` | FR-030 |

Regras gerais:

- Vários erros na mesma requisição são devolvidos juntos, agrupados por campo, sem mensagens repetidas (FR-028).
- Chaves de campo usam o nome do JSON (`email`, `username`, `password`, `bio`, `image`), sem o prefixo `user.`.
- Precedência: corpo ilegível (422 `body`) → validação de campos (422) → unicidade (409/422) → regras de seguir.
- Nenhuma resposta contém senha, *hash*, rastro de pilha, SQL ou nomes de tabelas (FR-030, FR-032).
