# blog — API Conduit (recorte A)

Backend REST do recorte A da API Conduit (RealWorld): cadastro e login com JWT, consulta e atualização do
usuário autenticado, consulta de perfis e seguir/deixar de seguir usuários.

Stack: Java 25, Spring Boot 4.1.2, Maven (wrapper), PostgreSQL 18.6 em Docker, Flyway.

## Endpoints

| Método | Rota | Autenticação |
|---|---|---|
| `POST` | `/api/users` | não |
| `POST` | `/api/users/login` | não |
| `GET` | `/api/user` | sim |
| `PUT` | `/api/user` | sim |
| `GET` | `/api/profiles/{username}` | opcional |
| `POST` | `/api/profiles/{username}/follow` | sim |
| `DELETE` | `/api/profiles/{username}/follow` | sim |

Autenticação: cabeçalho `Authorization: Token <jwt>`. Erros seguem o formato `{"errors": {"campo": ["mensagem"]}}`.

## Configuração

Copie `.env.example` para `.env` e preencha com valores locais. A aplicação lê o `.env` da raiz automaticamente.

| Variável | Uso |
|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DB_PORT` | banco local em Docker |
| `DB_URL` | URL JDBC do banco |
| `JWT_SECRET` | segredo HS256 dos tokens, com pelo menos 32 caracteres |

## Executar

```bash
docker compose up -d db
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

As migrations Flyway (`src/main/resources/db/migration`) são aplicadas na subida.

## Testar

```bash
./mvnw verify
```

Testes unitários (`*Test`) rodam no Surefire. Testes de integração (`*IT`) rodam no Failsafe, contra um
PostgreSQL 18.6 descartável via Testcontainers (exige Docker).
