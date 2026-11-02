# Contratos da API

- [openapi.yaml](openapi.yaml): cópia literal da especificação OpenAPI 3.1 de `descricao-base.md`. É o contrato obrigatório; não editar.
- Base: `http://localhost:8080/api`. Autenticação: cabeçalho `Authorization: Token <jwt>`.
- `Content-Type` das respostas: `application/json` com `charset=utf-8`.

## Resumo dos endpoints

| Método | Rota | Auth | Sucesso | Erros |
|---|---|---|---|---|
| POST | `/api/users` | não | 201 User | 409, 422 |
| POST | `/api/users/login` | não | 200 User | 401, 422 |
| GET | `/api/user` | sim | 200 User | 401 |
| PUT | `/api/user` | sim | 200 User | 401, 422 |
| GET | `/api/profiles/{username}` | opcional | 200 Profile | 401, 404 |
| POST | `/api/profiles/{username}/follow` | sim | 200 Profile | 401, 404, 422 |
| DELETE | `/api/profiles/{username}/follow` | sim | 200 Profile | 401, 404 |

## Chaves de erro adotadas

O formato é sempre `{"errors": {"<chave>": ["<mensagem>"]}}` (`GenericErrorModel`). O contrato não fixa as chaves; este projeto usa:

| Situação | Status | Corpo |
|---|---|---|
| Campo obrigatório em branco | 422 | `{"errors":{"<campo>":["can't be blank"]}}` |
| E-mail com formato inválido | 422 | `{"errors":{"email":["is invalid"]}}` |
| JSON malformado ou sem `user` | 422 | `{"errors":{"body":["<mensagem genérica>"]}}` |
| `PUT /api/user` sem campos aceitos | 422 | `{"errors":{"user":["at least one field is required"]}}` |
| Duplicidade no cadastro | 409 | `{"errors":{"<email\|username>":["has already been taken"]}}` |
| Duplicidade no `PUT /api/user` | 422 | `{"errors":{"<email\|username>":["has already been taken"]}}` |
| Seguir a si mesmo | 422 | `{"errors":{"profile":["cannot follow yourself"]}}` |
| Login com credenciais erradas | 401 | `{"errors":{"credentials":["invalid"]}}` |
| Token ausente em rota protegida | 401 | `{"errors":{"token":["is missing"]}}` |
| Token inválido ou expirado | 401 | `{"errors":{"token":["is invalid"]}}` |
| Perfil inexistente | 404 | `{"errors":{"profile":["not found"]}}` |
| Erro inesperado | 500 | `{"errors":{"server":["unexpected error"]}}` |
