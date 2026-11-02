# Quickstart: validar o Recorte A

Guia de execução e validação. Os formatos e códigos esperados estão em [contracts/README.md](contracts/README.md) e o esquema está em [data-model.md](data-model.md).

## Pré-requisitos

- Docker em execução, para o banco local e o Testcontainers.
- JDK 25, porque o Maven Wrapper baixa o Maven 3.10.0.
- [Hurl](https://hurl.dev), opcional, para a suíte ponta a ponta.
- Arquivo `.env` na raiz, criado a partir de `.env.example` conforme `.claude/rules/tech.md` §2.2. Também precisa da variável `JWT_SECRET`: 32 bytes aleatórios em Base64, gerados localmente e nunca versionados.

## 1. Validação automatizada (não usa o banco local)

```bash
./mvnw -q verify
```

Esse comando roda:

- os testes unitários e de fatia web (`*Test`);
- os testes de integração (`*IT`), que sobem `postgres:18.6` via Testcontainers, aplicam `V1` e `V2` em banco vazio, validam o mapeamento JPA e percorrem os 7 endpoints.

**Esperado**: build verde.

## 2. Subir a aplicação contra o banco local

```bash
docker compose up -d db
docker compose ps          # aguardar "healthy"
# exportar as variáveis do .env para o processo (sem imprimi-las) e então:
./mvnw -q spring-boot:run
```

**Esperado**: a aplicação sobe na porta `8080`, o Flyway registra `V1` e `V2` com `success = true` e `GET /actuator/health` retorna `UP`.

## 3. Cenários de verificação manual

| # | Requisição | Esperado |
|---|---|---|
| 1 | `POST /api/users` com `username`, `email`, `password` novos | `201`, `user.token` presente, `bio`/`image` `null` |
| 2 | Repetir o passo 1 | `409`, `errors.email` ou `errors.username` |
| 3 | `POST /api/users` com `{"user":{}}` | `422`, chaves `email`, `username`, `password` |
| 4 | `POST /api/users/login` com senha errada | `401` |
| 5 | `POST /api/users/login` correto | `200`, novo token |
| 6 | `GET /api/user` sem cabeçalho | `401`, `errors.token = ["is missing"]` |
| 7 | `GET /api/user` com `Authorization: Token <jwt>` | `200`, mesmo usuário |
| 8 | `PUT /api/user` com `{"user":{"bio":"x","image":"y"}}` | `200`, campos alterados e demais mantidos |
| 9 | `GET /api/profiles/<outro>` sem token | `200`, `following: false` |
| 10 | `POST /api/profiles/<outro>/follow` | `200`, `following: true` |
| 11 | `GET /api/profiles/<outro>` com token | `following: true` |
| 12 | `DELETE /api/profiles/<outro>/follow` | `200`, `following: false` |
| 13 | `GET /api/profiles/inexistente` | `404`, `errors.profile = ["not found"]` |
| 14 | Qualquer rota com `Authorization: Bearer <jwt>` | `401` |

Em todas as respostas, verifique também o cabeçalho `Content-Type: application/json;charset=UTF-8`.

## 4. Suíte Hurl (ponta a ponta)

Com a aplicação em execução:

```bash
hurl --test --variable host=http://localhost:8080 --variable run_id=$(date +%s) src/test/hurl/recorte-a.hurl
```

**Esperado**: todas as requisições aprovadas. Pelas regras do projeto, relate apenas as requisições reprovadas.
