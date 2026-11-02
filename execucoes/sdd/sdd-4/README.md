# blog — API Conduit (Recorte A)

API REST de blog, somente backend, em Spring Boot 4.1.2 + Java 25 + PostgreSQL 18.6.
Este recorte cobre usuários, autenticação JWT e perfis.

## Pré-requisitos

- Java 25
- Docker (banco local e Testcontainers)
- Python 3, apenas para gerar segredos locais

## Configuração local

Crie o `.env` (não versionado) a partir do `.env.example` e preencha com valores locais:

| Variável | Valor |
|---|---|
| `POSTGRES_DB` | nome da pasta do projeto com `-` trocado por `_` |
| `POSTGRES_USER` | `agente` |
| `POSTGRES_PASSWORD` | senha aleatória: `python -c "import secrets; print(secrets.token_urlsafe(24))"` |
| `DB_PORT` | `5432` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/<POSTGRES_DB>` |
| `JWT_SECRET` | segredo aleatório de pelo menos 32 bytes: `python -c "import secrets; print(secrets.token_urlsafe(48))"` |

Nunca versione o `.env` nem reutilize senhas reais.

## Banco de dados

```bash
docker compose up -d db
docker compose ps    # aguarde o estado "healthy"
```

O esquema é criado pelas migrations Flyway em `src/main/resources/db/migration` ao subir a aplicação.

## Testes

```bash
./mvnw -q verify
```

Executa os testes unitários (`*Test`, Surefire) e de integração (`*IT`, Failsafe). Os testes de integração usam
Testcontainers com `postgres:18.6` e não dependem do banco local.

## Executar a aplicação

Com as variáveis do `.env` exportadas no ambiente:

```bash
set -a; . ./.env; set +a
./mvnw spring-boot:run
```

A API responde em `http://localhost:8080`.

## Endpoints

Autenticação pelo cabeçalho `Authorization: Token <jwt>`. Todas as respostas usam
`Content-Type: application/json;charset=UTF-8` e os erros seguem o formato `{"errors": {"<campo>": ["<mensagem>"]}}`.

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| `POST` | `/api/users` | não | Cadastro (`201`) |
| `POST` | `/api/users/login` | não | Login |
| `GET` | `/api/user` | sim | Usuário atual |
| `PUT` | `/api/user` | sim | Atualiza o usuário atual |
| `GET` | `/api/profiles/{username}` | opcional | Perfil público |
| `POST` | `/api/profiles/{username}/follow` | sim | Seguir |
| `DELETE` | `/api/profiles/{username}/follow` | sim | Deixar de seguir |

Contrato completo: [`specs/001-conduit-usuarios-perfis/contracts/openapi.yaml`](specs/001-conduit-usuarios-perfis/contracts/openapi.yaml).
