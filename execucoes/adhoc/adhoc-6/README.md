# blog — API Conduit (recorte A)

Backend REST em Spring Boot 4.1 / Java 25 / PostgreSQL 18.6 que implementa o recorte A da API
[Conduit (RealWorld)](https://realworld-docs.netlify.app/): cadastro e login com JWT, usuário autenticado,
perfis públicos e seguir/deixar de seguir.

| Método | Rota | Autenticação |
|---|---|---|
| `POST` | `/api/users` | não (retorna `201`) |
| `POST` | `/api/users/login` | não |
| `GET` | `/api/user` | obrigatória |
| `PUT` | `/api/user` | obrigatória |
| `GET` | `/api/profiles/{username}` | opcional |
| `POST` | `/api/profiles/{username}/follow` | obrigatória |
| `DELETE` | `/api/profiles/{username}/follow` | obrigatória |

O token vai no cabeçalho `Authorization: Token <jwt>`. Erros seguem o formato `{"errors": {"campo": ["mensagem"]}}`
com `401`, `404`, `409` (username/email já usados) e `422` (validação).

## Executar localmente

1. Copie `.env.example` para `.env` e preencha os valores (o `.env` não é versionado).
   `JWT_SECRET` precisa ter pelo menos 32 bytes.
2. Suba o banco: `docker compose up -d db`
3. Rode a aplicação: `./mvnw spring-boot:run` (lê o `.env` da raiz; as migrations do Flyway são aplicadas na subida).
   Com dados de desenvolvimento: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.

## Testes

`./mvnw verify` roda os testes unitários (`*Test`, Surefire) e os de integração (`*IT`, Failsafe).
Os testes de integração usam PostgreSQL 18.6 descartável via Testcontainers, então exigem Docker.
