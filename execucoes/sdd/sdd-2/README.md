# Blog — API Conduit (Recorte A)

API REST de blog (somente backend) em Spring Boot 4.1.2, Java 25, Maven e PostgreSQL 18.6.
Este recorte cobre cadastro, login com JWT, conta do usuário, perfis públicos e seguir/deixar de seguir.

Contrato completo: [`specs/001-conduit-auth-profiles/contracts/openapi.yaml`](specs/001-conduit-auth-profiles/contracts/openapi.yaml).

## Pré-requisitos

- Java 25
- Docker (banco local e Testcontainers)

## Configuração (`.env`)

Crie o `.env` na raiz a partir do `.env.example`. Ele **não** é versionado.

| Variável | Valor |
|---|---|
| `POSTGRES_DB` | nome da pasta raiz com `-` trocado por `_` |
| `POSTGRES_USER` | usuário local |
| `POSTGRES_PASSWORD` | senha local aleatória |
| `DB_PORT` | `5432` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/<POSTGRES_DB>` |
| `JWT_SECRET` | segredo aleatório com pelo menos 32 caracteres |

Exemplo para gerar valores aleatórios sem exibi-los: `python -c "import secrets; print(secrets.token_urlsafe(36))"`
redirecionado direto para o arquivo.

## Executar

```bash
docker compose up -d db          # sobe o PostgreSQL local
docker compose ps                # aguarde o estado "healthy"
# exporte as variáveis do .env no shell e então:
./mvnw spring-boot:run
```

Ao subir, o Flyway aplica as migrations de `src/main/resources/db/migration` e o Hibernate valida o esquema
(`ddl-auto: validate`). A API responde em `http://localhost:8080`.

## Testar

```bash
./mvnw -q verify
```

Roda os testes unitários (`*Test`, Surefire) e de integração (`*IT`, Failsafe). Os de integração usam um
PostgreSQL 18.6 descartável via Testcontainers, nunca o banco de desenvolvimento.

## Rotas

Autenticação pelo cabeçalho `Authorization: Token <jwt>`.

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| `POST` | `/api/users` | não | Cadastro (`201`) |
| `POST` | `/api/users/login` | não | Login (`200`) |
| `GET` | `/api/user` | sim | Usuário autenticado |
| `PUT` | `/api/user` | sim | Atualiza o usuário autenticado |
| `GET` | `/api/profiles/{username}` | opcional | Perfil público |
| `POST` | `/api/profiles/{username}/follow` | sim | Seguir |
| `DELETE` | `/api/profiles/{username}/follow` | sim | Deixar de seguir |

Erros seguem o formato `{"errors":{"<chave>":["<mensagem>"]}}`.
