# Quickstart: validar o Recorte A da API Conduit

**Feature**: [spec.md](./spec.md) | **Contrato**: [contracts/openapi.yaml](./contracts/openapi.yaml) | **Modelo**: [data-model.md](./data-model.md)

Guia para provar, ponta a ponta, que a funcionalidade funciona. Comandos executados na raiz do repositório (Git Bash).

## Pré-requisitos

- JDK 25 e Docker em execução.
- `.env` criado a partir de `.env.example` (ver `.claude/rules/tech.md`, "Criação obrigatória do `.env`"), incluindo `JWT_SECRET` (Base64 de 32 bytes aleatórios, gerado com o módulo `secrets` do Python). Nunca cole o valor no chat nem em arquivos versionados.
- `curl`; opcionalmente `jq` e `hurl`.

## 1. Testes automatizados

```bash
./mvnw -q verify
```

Esperado: build verde. Roda os testes unitários e de fatia (`*Test`, Surefire) e os de integração (`*IT`, Failsafe) em um PostgreSQL 18.6 descartável via Testcontainers — **não** usa o banco local. Inclui `MigrationsIT` (migrations aplicadas em banco vazio, `ddl-auto: validate`).

## 2. Subir o banco local e a aplicação

```bash
docker compose up -d db
docker compose ps            # aguarde "healthy"
./mvnw -q spring-boot:run -Dspring-boot.run.profiles=dev
```

Esperado: a aplicação sobe em `http://localhost:8080`, o Flyway aplica `V1` e `V2`, e `GET http://localhost:8080/actuator/health` retorna `{"status":"UP"}`.

## 3. Cenários manuais (resumo dos cenários de aceitação)

Variável de apoio: `API=http://localhost:8080/api`. Use dados fictícios.

| # | Requisição | Esperado |
|---|---|---|
| 1 | `POST $API/users` com `{"user":{"username":"jake","email":"jake@jake.jake","password":"jakejake"}}` | `201`, `user` com `token`, `bio: null`, `image: null`; `Content-Type` com `charset=UTF-8` |
| 2 | Repetir o passo 1 | `409`, `errors.username` e `errors.email` = `has already been taken` |
| 3 | `POST $API/users` com `{"user":{"username":"x","password":"y"}}` | `422`, `errors.email` = `can't be blank` |
| 4 | `POST $API/users/login` com e-mail `JAKE@jake.jake` e senha correta | `200`, `user` com novo `token` (e-mail sem diferenciar maiúsculas) |
| 5 | Login com senha errada | `401`, `errors.credentials` |
| 6 | `GET $API/user` sem cabeçalho | `401`, `errors.token` = `is missing` |
| 7 | `GET $API/user` com `Authorization: Bearer <token>` | `401`, `errors.token` = `is invalid` |
| 8 | `GET $API/user` com `Authorization: Token <token>` | `200`, `user` de jake |
| 9 | `PUT $API/user` com `{"user":{"bio":"I like to skateboard"}}` | `200`, `bio` atualizada, demais campos mantidos |
| 10 | `PUT $API/user` com `{"user":{"bio":null}}` | `200`, `bio: null` |
| 11 | `PUT $API/user` com `{"user":{}}` | `422` |
| 12 | Cadastrar `celeb`; `GET $API/profiles/celeb` sem token | `200`, `following: false` |
| 13 | `POST $API/profiles/celeb/follow` com token de jake (2×) | `200`, `following: true` nas duas |
| 14 | `GET $API/profiles/celeb` com token de jake | `following: true` |
| 15 | `DELETE $API/profiles/celeb/follow` com token de jake (2×) | `200`, `following: false` nas duas |
| 16 | `POST $API/profiles/jake/follow` com token de jake | `422`, `errors.profile` = `cannot follow yourself` |
| 17 | `GET $API/profiles/ninguem` | `404`, `errors.profile` = `not found` |
| 18 | `GET $API/profiles/celeb` com `Authorization: Token abc` | `401`, `errors.token` = `is invalid` |

Exemplo do passo 1:

```bash
curl -si -X POST "$API/users" \
  -H 'Content-Type: application/json' \
  -d '{"user":{"username":"jake","email":"jake@jake.jake","password":"jakejake"}}'
```

## 4. Suíte Hurl (opcional)

Se houver uma suíte Hurl no repositório, rode-a contra um banco limpo e mostre somente as requisições reprovadas. Recriar o banco local (`docker compose down -v`) apaga os dados e **exige confirmação explícita** (`tech.md`, "Reinicialização do banco local").

## 5. Encerrar

```bash
docker compose stop db
```
