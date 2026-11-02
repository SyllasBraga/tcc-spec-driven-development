# blog — API Conduit (recorte A)

API REST de blog (somente backend) em Java 25, Spring Boot 4.1.2, Maven e PostgreSQL 18.6.
Este recorte entrega cadastro e login com JWT, consulta e atualização do usuário autenticado,
perfis públicos e seguir/deixar de seguir.

Contrato da API: [specs/001-conduit-usuarios-perfis/contracts/](specs/001-conduit-usuarios-perfis/contracts/README.md)
(OpenAPI em `openapi.yaml`).

## Pré-requisitos

- JDK 25
- Docker com Compose

## Configuração local

Crie o `.env` na raiz a partir do `.env.example`. O `.env` não é versionado.

| Variável | Valor |
|---|---|
| `POSTGRES_DB` | `sdd_1` |
| `POSTGRES_USER` | usuário local (por exemplo, `agente`) |
| `POSTGRES_PASSWORD` | senha aleatória local, nunca uma senha real |
| `DB_PORT` | `5432` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/sdd_1` |
| `JWT_SECRET` | segredo aleatório com pelo menos 32 bytes |

Exemplo para gerar valores aleatórios: `python -c "import secrets; print(secrets.token_urlsafe(48))"`.

## Executar

```bash
docker compose up -d db
docker compose ps                    # aguardar "healthy"
./mvnw -q spring-boot:run -Dspring-boot.run.profiles=dev
```

O perfil `dev` importa as variáveis do `.env`. O Flyway aplica as migrations de
`src/main/resources/db/migration` e a API sobe em `http://localhost:8080`.

## Testar

```bash
./mvnw -q verify
```

- `*Test` (Surefire): testes unitários e de fatia (`@WebMvcTest`).
- `*IT` (Failsafe): testes de integração com PostgreSQL 18.6 descartável via Testcontainers
  (exige Docker). Eles não usam o banco do Docker Compose.

## Endpoints

Autenticação pelo cabeçalho `Authorization: Token <jwt>`.

| Método | Rota | Autenticação | Sucesso |
|---|---|---|---|
| POST | `/api/users` | não | 201 User |
| POST | `/api/users/login` | não | 200 User |
| GET | `/api/user` | sim | 200 User |
| PUT | `/api/user` | sim | 200 User |
| GET | `/api/profiles/{username}` | opcional | 200 Profile |
| POST | `/api/profiles/{username}/follow` | sim | 200 Profile |
| DELETE | `/api/profiles/{username}/follow` | sim | 200 Profile |

Erros sempre no formato `{"errors": {"<campo>": ["<mensagem>"]}}`.

## Encerrar

```bash
docker compose stop db
```

`docker compose down -v` apaga os dados locais do banco.
