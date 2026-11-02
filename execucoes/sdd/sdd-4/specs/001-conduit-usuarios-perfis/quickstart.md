# Quickstart: validar o Recorte A da Conduit

Guia de execução e validação ponta a ponta. Contrato em [contracts/openapi.yaml](contracts/openapi.yaml); esquema em [data-model.md](data-model.md).

## Pré-requisitos

- Java 25, Docker em execução.
- `.env` criado a partir de `.env.example` (regras em `.claude/rules/tech.md` §2.2), com `JWT_SECRET` preenchido por um valor aleatório de pelo menos 32 bytes gerado localmente.

## 1. Testes automatizados

```bash
./mvnw -q verify
```

Esperado: unitários (`*Test`) e de integração (`*IT`, Testcontainers `postgres:18.6`) passam; o IT de migrations aplica `V1` e `V2` em banco vazio com `ddl-auto: validate`.

## 2. Subir banco e aplicação

```bash
docker compose up -d db
docker compose ps          # aguardar "healthy"
./mvnw spring-boot:run     # com as variáveis do .env exportadas no ambiente
```

Esperado: Flyway aplica `V1__criar_tabela_usuario` e `V2__criar_tabela_seguimento`; a aplicação sobe na porta 8080.

## 3. Cenários de validação manual

Use dados fictícios. `$T1`/`$T2` = tokens devolvidos nos cadastros.

| # | Requisição | Esperado |
|---|---|---|
| 1 | `POST /api/users` `{"user":{"username":"ana","email":"ana@exemplo.test","password":"segredo1"}}` | `201`, `user.token` presente, `bio`/`image` `null` |
| 2 | Repetir o passo 1 | `409`, `errors.username` = `has already been taken` |
| 3 | `POST /api/users` com `email` vazio | `422`, `errors.email` = `can't be blank` |
| 4 | `POST /api/users/login` com e-mail e senha do passo 1 | `200`, novo token |
| 5 | Login com senha errada | `401`, `errors.credentials` |
| 6 | `GET /api/user` sem cabeçalho | `401`, `errors.token` = `is missing` |
| 7 | `GET /api/user` com `Authorization: Token $T1` | `200`, dados de `ana` |
| 8 | `PUT /api/user` `{"user":{"bio":"Olá","image":null}}` com `$T1` | `200`, `bio` = `Olá`, `image` `null` |
| 9 | `PUT /api/user` `{"user":{}}` com `$T1` | `422` |
| 10 | Cadastrar `beto`; `GET /api/profiles/ana` sem token | `200`, `following: false` |
| 11 | `POST /api/profiles/ana/follow` com `$T2` (2×) | `200`, `following: true` nas duas |
| 12 | `GET /api/profiles/ana` com `$T2` | `following: true` |
| 13 | `DELETE /api/profiles/ana/follow` com `$T2` (2×) | `200`, `following: false` nas duas |
| 14 | `POST /api/profiles/ana/follow` com `$T1` | `422`, `errors.profile` = `cannot follow yourself` |
| 15 | `GET /api/profiles/naoexiste` | `404`, `errors.profile` = `not found` |
| 16 | `GET /api/profiles/ana` com `Authorization: Token invalido` | `401` |

Em todas as respostas, conferir `Content-Type: application/json;charset=UTF-8`.

## 4. Encerrar

```bash
docker compose stop db
```
