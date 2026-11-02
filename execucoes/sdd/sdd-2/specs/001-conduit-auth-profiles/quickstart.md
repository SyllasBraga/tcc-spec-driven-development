# Quickstart: validar o Recorte A da API Conduit

Guia de validação ponta a ponta. Contrato: [contracts/openapi.yaml](./contracts/openapi.yaml). Modelo: [data-model.md](./data-model.md).

## Pré-requisitos

- Java 25, Docker em execução, `curl`.
- `.env` na raiz criado a partir de `.env.example` (ver `tech.md`, seção 2.2), incluindo a nova variável `JWT_SECRET` com pelo menos 32 caracteres aleatórios. O `.env` não é versionado.

## 1. Testes automatizados

```bash
./mvnw -q verify
```

Esperado: Surefire (`*Test`) e Failsafe (`*IT`) passam. Os testes de integração sobem um PostgreSQL 18.6 descartável via Testcontainers, aplicam as migrations `V1` e `V2` em banco vazio e validam o mapeamento JPA. Eles não usam o banco de desenvolvimento.

## 2. Subir banco e aplicação

```bash
docker compose up -d db
docker compose ps            # db deve estar "healthy"
```

Exporte as variáveis do `.env` no shell (sem imprimi-las) e rode `./mvnw spring-boot:run`. Ao subir, o Flyway aplica `V1__criar_tabela_users` e `V2__criar_tabela_follows`, e o Hibernate valida o esquema.

## 3. Cenários manuais (base `http://localhost:8080/api`)

Use dados fictícios. Em cada resposta, confira `Content-Type: application/json;charset=UTF-8` (`curl -i`).

| # | Requisição | Esperado | História/FR |
|---|---|---|---|
| 1 | `POST /users` `{"user":{"username":"jake","email":"jake@jake.jake","password":"jakejake"}}` | `201`, `user` com `token`, `bio: null`, `image: null` | US1, FR-001 |
| 2 | Repetir 1 | `409`, `errors.email` e `errors.username` = `has already been taken` | FR-004 |
| 3 | `POST /users` sem `email` | `422`, `errors.email` = `can't be blank` | FR-002 |
| 4 | `POST /users/login` com `JAKE@jake.jake` / `jakejake` | `200` com novo `token` | US2, FR-005 |
| 5 | `POST /users/login` com senha errada | `401`, `errors.credentials` = `invalid` | FR-007 |
| 6 | `GET /user` sem cabeçalho | `401`, `errors.token` = `is missing` | FR-010 |
| 7 | `GET /user` com `Authorization: Bearer <token>` | `401`, `is missing` | R2 |
| 8 | `GET /user` com `Authorization: Token abc.def.ghi` | `401`, `errors.token` = `is invalid` | FR-011 |
| 9 | `GET /user` com `Authorization: Token <token>` | `200` com o `user` | US3, FR-012 |
| 10 | `PUT /user` `{"user":{"bio":"I like to skateboard"}}` | `200`, `bio` alterado, demais iguais | FR-013 |
| 11 | `PUT /user` `{"user":{"bio":null}}` | `200`, `bio: null` | FR-018 |
| 12 | `PUT /user` `{"user":{}}` | `422`, `errors.user` = `can't be empty` | FR-014 |
| 13 | `POST /users` cria `ana`; `GET /profiles/ana` sem token | `200`, `following: false` | US4, FR-020 |
| 14 | `GET /profiles/ninguem` | `404`, `errors.profile` = `not found` | FR-022 |
| 15 | `POST /profiles/ana/follow` com token de `jake` (2 vezes) | `200`, `following: true` nas duas | US5, FR-023/024 |
| 16 | `GET /profiles/ana` com token de `jake` | `200`, `following: true` | FR-021 |
| 17 | `DELETE /profiles/ana/follow` com token de `jake` (2 vezes) | `200`, `following: false` nas duas | FR-025 |
| 18 | `POST /profiles/jake/follow` com token de `jake` | `422`, `errors.profile` = `cannot follow yourself` | FR-026 |
| 19 | `POST /profiles/ana/follow` sem token | `401`, `is missing` | FR-010 |
| 20 | `POST /users` com corpo `{` | `422`, `errors.body` = `can't be empty` | FR-028 |

Exemplo:

```bash
curl -i -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"user":{"username":"jake","email":"jake@jake.jake","password":"jakejake"}}'
```

## 4. Conferir o esquema (opcional, somente leitura)

Siga `tech.md` (`\conninfo` primeiro, `psql` dentro do contêiner):

```bash
docker compose exec -T db sh -c 'psql -X -v ON_ERROR_STOP=1 -P pager=off -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "\\conninfo" -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;" -c "\\d users" -c "\\d follows"'
```

Esperado: `V1` e `V2` com `success = t`; restrições `pk_users`, `uq_users_username`, `uq_users_email`, `pk_follows`, `fk_follows_follower`, `fk_follows_followed`, `ck_follows_not_self`.
