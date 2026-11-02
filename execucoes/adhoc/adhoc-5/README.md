# blog — API Conduit (recorte A)

Backend REST em Spring Boot 4.1 + PostgreSQL 18.6 com o recorte A da API Conduit (RealWorld):
cadastro e login com JWT, consulta e atualização do usuário autenticado, perfis públicos e seguir/deixar de seguir.

## Endpoints

| Método | Rota | Autenticação |
|---|---|---|
| `POST` | `/api/users` | não (responde 201) |
| `POST` | `/api/users/login` | não |
| `GET` | `/api/user` | obrigatória |
| `PUT` | `/api/user` | obrigatória |
| `GET` | `/api/profiles/{username}` | opcional |
| `POST` | `/api/profiles/{username}/follow` | obrigatória |
| `DELETE` | `/api/profiles/{username}/follow` | obrigatória |

O token vai no cabeçalho `Authorization: Token <jwt>`. Os erros seguem `{"errors": {"<campo>": ["<mensagem>"]}}`:
422 (validação), 401 (token ausente/inválido ou credenciais erradas), 404 (perfil inexistente) e 409 (username ou e-mail já usado).

Decisões que o contrato não fixa:

- e-mail é guardado em minúsculas, então login e unicidade não diferenciam maiúsculas;
- username não aceita espaços nem `/` (ele faz parte da URL do perfil) e tem até 50 caracteres;
- no `PUT /api/user`, campo ausente ou `null` não muda nada; para limpar `bio` ou `image`, envie `""`;
- seguir e deixar de seguir são idempotentes; seguir a si mesmo devolve 422.

## Executar

1. Copie `.env.example` para `.env` e preencha os valores (inclusive `JWT_SECRET`, com pelo menos 32 bytes aleatórios).
2. Suba o banco: `docker compose up -d db`.
3. Carregue as variáveis do `.env` no ambiente e rode `./mvnw spring-boot:run`. As migrations do Flyway são aplicadas na subida.

## Testar

`./mvnw verify` roda os testes unitários (`*Test`, Surefire) e os de integração (`*IT`, Failsafe). Os de integração
usam PostgreSQL 18.6 descartável via Testcontainers, então o Docker precisa estar disponível.
