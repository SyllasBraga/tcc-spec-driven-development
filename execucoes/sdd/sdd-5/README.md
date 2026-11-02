# Blog — API Conduit (recorte A)

API REST de blog (somente backend) em Spring Boot 4.1.2, Java 25, Maven e PostgreSQL 18.6.
Este recorte cobre cadastro, autenticação, conta do usuário e perfis com seguir/deixar de seguir.

## Pré-requisitos

- JDK 25 (o Maven vem pelo wrapper `./mvnw`);
- Docker em execução (banco local e Testcontainers nos testes de integração).

## Configuração local

Crie o `.env` na raiz a partir de `.env.example` (o `.env` não é versionado). Preencha com valores locais e
descartáveis — nunca reutilize senhas reais:

| Variável | Conteúdo |
|---|---|
| `POSTGRES_DB` | `sdd_5` (nome da pasta com `-` trocado por `_`) |
| `POSTGRES_USER` | usuário local do banco |
| `POSTGRES_PASSWORD` | senha local aleatória |
| `DB_PORT` | `5432` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/sdd_5` |
| `JWT_SECRET` | segredo aleatório com 32 ou mais caracteres, para assinar os tokens (HS256) |

## Executar

```bash
docker compose up -d db
docker compose ps              # aguardar "healthy"
set -a; . ./.env; set +a       # carrega as variáveis sem exibi-las
./mvnw -q spring-boot:run
```

A aplicação sobe em `http://localhost:8080`; o Flyway aplica as migrations de `src/main/resources/db/migration`.

## Testar

```bash
./mvnw -q verify
```

Roda os testes unitários (`*Test`, Surefire) e de integração (`*IT`, Failsafe) com PostgreSQL 18.6 via
Testcontainers. Não usa o banco local de desenvolvimento.

## Endpoints

Autenticação pelo cabeçalho `Authorization: Token <jwt>`. Erros no formato `{"errors":{"<chave>":["<mensagem>"]}}`.

| Método e rota | Autenticação | Sucesso |
|---|---|---|
| `POST /api/users` | não | `201` usuário com token |
| `POST /api/users/login` | não | `200` usuário com token |
| `GET /api/user` | obrigatória | `200` usuário atual |
| `PUT /api/user` | obrigatória | `200` usuário atualizado |
| `GET /api/profiles/{username}` | opcional | `200` perfil |
| `POST /api/profiles/{username}/follow` | obrigatória | `200` perfil (`following: true`) |
| `DELETE /api/profiles/{username}/follow` | obrigatória | `200` perfil (`following: false`) |

Contrato completo, status e mensagens de erro: [specs/001-conduit-usuarios-perfis/contracts/](specs/001-conduit-usuarios-perfis/contracts/).
