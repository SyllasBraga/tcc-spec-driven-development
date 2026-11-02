# Quickstart: validar o Recorte A da API Conduit

Guia de validação ponta a ponta. Contrato em [contracts/](contracts/README.md); modelo em [data-model.md](data-model.md).

## Pré-requisitos

- JDK 25, Docker com Compose, `curl`.
- `.env` na raiz criado a partir de `.env.example` (regra de `tech.md` §2.2), com `POSTGRES_DB=sdd_1`, `POSTGRES_USER=agente`, senha aleatória de 32 caracteres, `DB_PORT=5432`, `DB_URL=jdbc:postgresql://localhost:5432/sdd_1` e `JWT_SECRET` aleatório com pelo menos 32 bytes.
- O `.env.example` passa a listar `JWT_SECRET` (valor fictício).

## 1. Testes automatizados

```bash
./mvnw -q verify
```

Esperado: Surefire (`*Test`) e Failsafe (`*IT`) passam. Os `*IT` sobem `postgres:18.6` via Testcontainers, aplicam `V1` e `V2` em banco vazio e validam o mapeamento JPA. Não usam o banco do Docker Compose.

## 2. Banco local e aplicação

```bash
docker compose up -d db
docker compose ps            # aguardar "healthy"
./mvnw -q spring-boot:run -Dspring-boot.run.profiles=dev
```

O perfil `dev` importa as variáveis do `.env`. Esperado no log: Flyway aplica `1 - criar tabela usuario` e `2 - criar tabela seguidor`; aplicação na porta `8080`.

Conferência opcional do histórico (leitura, dentro do contêiner):

```bash
docker compose exec -T db sh -c 'psql -X -v ON_ERROR_STOP=1 -P pager=off -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"'
```

## 3. Cenários manuais

Dados fictícios. Guarde os tokens retornados em `TOKEN_A` e `TOKEN_B`.

| # | Requisição | Esperado |
|---|---|---|
| 1 | `POST /api/users` `{"user":{"username":"ana","email":"ana@exemplo.test","password":"senha-ficticia"}}` | `201`, `user.token` preenchido, `bio` e `image` `null` |
| 2 | Repetir o 1 | `409`, `errors.username` ou `errors.email` = `has already been taken` |
| 3 | `POST /api/users` sem `email` | `422`, `errors.email` = `can't be blank` |
| 4 | `POST /api/users` para `bia` / `bia@exemplo.test` | `201` |
| 5 | `POST /api/users/login` com e-mail `ANA@exemplo.test` e senha certa | `200`, novo token |
| 6 | `POST /api/users/login` com senha errada | `401`, `errors.credentials` |
| 7 | `GET /api/user` sem `Authorization` | `401`, `errors.token` = `is missing` |
| 8 | `GET /api/user` com `Authorization: Token $TOKEN_A` | `200`, `user.username` = `ana` |
| 9 | `GET /api/user` com `Authorization: Token abc` | `401`, `errors.token` = `is invalid` |
| 10 | `PUT /api/user` `{"user":{"bio":"Olá"}}` | `200`, `bio` = `Olá`, demais campos iguais |
| 11 | `PUT /api/user` `{"user":{"bio":null}}` | `200`, `bio` = `null` |
| 12 | `PUT /api/user` `{"user":{}}` | `422`, `errors.user` |
| 13 | `PUT /api/user` `{"user":{"username":"bia"}}` | `422`, `errors.username` = `has already been taken` |
| 14 | `GET /api/profiles/bia` sem token | `200`, `following: false` |
| 15 | `POST /api/profiles/bia/follow` com `TOKEN_A` | `200`, `following: true` |
| 16 | Repetir o 15 | `200`, `following: true` |
| 17 | `GET /api/profiles/bia` com `TOKEN_A` | `200`, `following: true` |
| 18 | `DELETE /api/profiles/bia/follow` com `TOKEN_A` | `200`, `following: false` |
| 19 | `POST /api/profiles/ana/follow` com `TOKEN_A` | `422`, `errors.profile` |
| 20 | `GET /api/profiles/ninguem` | `404`, `errors.profile` = `not found` |
| 21 | Qualquer resposta acima | `Content-Type` = `application/json` com `charset=utf-8` (caixa indiferente) |

Exemplo:

```bash
curl -si -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"user":{"username":"ana","email":"ana@exemplo.test","password":"senha-ficticia"}}'
```

## 4. Encerrar

```bash
docker compose stop db
```

Não use `docker compose down -v` sem confirmação: apaga os dados locais.
