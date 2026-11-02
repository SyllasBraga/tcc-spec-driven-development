# Contratos — Conduit Recorte A

- [`openapi.yaml`](openapi.yaml): especificação OpenAPI **obrigatória**, copiada sem alteração de `descricao-base.md`. Ela prevalece sobre a prosa.

## Resumo dos endpoints

| Método e rota | Autenticação | Sucesso | Erros |
|---|---|---|---|
| `POST /api/users` | não | `201` `{"user"}` | `409`, `422` |
| `POST /api/users/login` | não | `200` `{"user"}` | `401`, `422` |
| `GET /api/user` | obrigatória | `200` `{"user"}` | `401` |
| `PUT /api/user` | obrigatória | `200` `{"user"}` | `401`, `409`, `422` |
| `GET /api/profiles/{username}` | opcional | `200` `{"profile"}` | `401` (token inválido), `404` |
| `POST /api/profiles/{username}/follow` | obrigatória | `200` `{"profile"}` | `401`, `404` |
| `DELETE /api/profiles/{username}/follow` | obrigatória | `200` `{"profile"}` | `401`, `404` |

- **Cabeçalho de autenticação**: `Authorization: Token <jwt>`.
- **Content-Type das respostas**: `application/json; charset=utf-8`.

## Catálogo de erros (formato `{"errors": {"<chave>": ["<mensagem>"]}}`)

| Situação | Status | Corpo |
|---|---|---|
| Campo obrigatório ausente ou vazio | `422` | `{"errors":{"<campo>":["can't be blank"]}}` |
| E-mail em formato inválido | `422` | `{"errors":{"email":["is invalid"]}}` |
| Campo acima do limite | `422` | `{"errors":{"<campo>":["is too long"]}}` |
| JSON malformado ou sem raiz `user` | `422` | `{"errors":{"body":["is invalid"]}}` |
| `PUT /api/user` sem nenhum campo | `422` | `{"errors":{"body":["at least one field is required"]}}` |
| E-mail ou nome de usuário já em uso | `409` | `{"errors":{"<email\|username>":["has already been taken"]}}` |
| Login com credenciais inválidas | `401` | `{"errors":{"credentials":["invalid"]}}` |
| Token ausente em rota protegida | `401` | `{"errors":{"token":["is missing"]}}` |
| Token malformado, expirado, com assinatura inválida ou com prefixo diferente de `Token` | `401` | `{"errors":{"token":["is invalid"]}}` |
| Perfil inexistente | `404` | `{"errors":{"profile":["not found"]}}` |
| Acesso negado (sem caso de uso neste recorte) | `403` | `{"errors":{"authorization":["forbidden"]}}` |
| Erro inesperado | `500` | `{"errors":{"server":["unexpected error"]}}` |
