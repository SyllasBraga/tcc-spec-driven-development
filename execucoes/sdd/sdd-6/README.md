# Blog — API Conduit (Recorte A)

API REST de blog, somente backend, em Spring Boot 4.1.2 (Java 25) com PostgreSQL 18.6. Este recorte cobre
cadastro, login com JWT, consulta e atualização do usuário atual, perfis públicos e seguir/deixar de seguir.

## Pré-requisitos

- JDK 25
- Docker (banco local e Testcontainers)
- [Hurl](https://hurl.dev) (opcional, para a suíte ponta a ponta)

## Configuração local

Crie o `.env` na raiz a partir de `.env.example` (o `.env` não é versionado):

| Variável | Valor |
|---|---|
| `POSTGRES_DB` | nome da pasta raiz com `-` trocado por `_` (por exemplo, `sdd_6`) |
| `POSTGRES_USER` | usuário local, por exemplo `agente` |
| `POSTGRES_PASSWORD` | senha aleatória local, nunca reutilizada de outro ambiente |
| `DB_PORT` | `5432` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/<POSTGRES_DB>` |
| `JWT_SECRET` | 32 bytes aleatórios em Base64 |

Para gerar valores aleatórios sem exibi-los no terminal, grave-os direto no arquivo, por exemplo com o módulo
`secrets` do Python (`secrets.token_urlsafe(24)` para a senha e
`base64.b64encode(secrets.token_bytes(32))` para o `JWT_SECRET`).

## Comandos

```bash
# subir o banco local e aguardar "healthy"
docker compose up -d db
docker compose ps

# compilar e rodar testes unitários (*Test) e de integração (*IT, com Testcontainers)
./mvnw -q verify

# subir a aplicação (porta 8080), com as variáveis do .env exportadas no ambiente
set -a; . ./.env; set +a
./mvnw -q spring-boot:run
```

O Flyway aplica as migrations de `src/main/resources/db/migration` ao subir a aplicação, e o Hibernate apenas
valida o esquema (`ddl-auto: validate`).

## Suíte Hurl

Com a aplicação em execução:

```bash
hurl --test --variable host=http://localhost:8080 --variable run_id=$(date +%s) src/test/hurl/recorte-a.hurl
```

## Endpoints

Autenticação pelo cabeçalho `Authorization: Token <jwt>`. Todas as respostas usam
`Content-Type: application/json;charset=UTF-8` e os erros seguem o formato `{"errors": {"<chave>": ["<mensagem>"]}}`.

| Método e rota | Autenticação | Sucesso |
|---|---|---|
| `POST /api/users` | não | `201` `{"user"}` |
| `POST /api/users/login` | não | `200` `{"user"}` |
| `GET /api/user` | obrigatória | `200` `{"user"}` |
| `PUT /api/user` | obrigatória | `200` `{"user"}` |
| `GET /api/profiles/{username}` | opcional | `200` `{"profile"}` |
| `POST /api/profiles/{username}/follow` | obrigatória | `200` `{"profile"}` |
| `DELETE /api/profiles/{username}/follow` | obrigatória | `200` `{"profile"}` |

O contrato completo (OpenAPI) e o catálogo de erros estão em
[`specs/001-conduit-auth-perfis/contracts/`](specs/001-conduit-auth-perfis/contracts/).
