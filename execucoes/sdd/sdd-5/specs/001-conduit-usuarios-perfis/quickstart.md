# Quickstart: validar o Recorte A da API Conduit

Guia para provar, de ponta a ponta, que a funcionalidade atende à [spec](spec.md). Contratos: [openapi.yaml](contracts/openapi.yaml) e [erros.md](contracts/erros.md). Modelo: [data-model.md](data-model.md).

## Pré-requisitos

- Docker em execução (banco local e Testcontainers).
- JDK 25 (o Maven vem pelo wrapper `./mvnw`).
- `curl` (Git Bash no Windows).
- `.env` na raiz criado a partir de `.env.example`, conforme `.claude/rules/tech.md` §2.2, incluindo `JWT_SECRET` (32 ou mais caracteres aleatórios, gerados localmente). Nunca imprima nem cole o conteúdo do `.env`.

## 1. Validação automatizada (principal)

```bash
./mvnw -q verify
```

Esperado: compilação sem erros, testes unitários (`*Test`, Surefire) e de integração (`*IT`, Failsafe, PostgreSQL 18.6 via Testcontainers) aprovados, incluindo:

- `MigracoesIT`: aplica `V1` e `V2` em banco vazio e valida o mapeamento JPA;
- ITs dos 7 endpoints cobrindo todos os cenários de aceitação da spec (US1 a US5), os casos de borda e o cabeçalho `Content-Type`.

Não usa o banco local de desenvolvimento.

## 2. Subir a aplicação contra o banco local

```bash
docker compose up -d db
docker compose ps              # aguardar "healthy"
set -a; . ./.env; set +a       # carrega as variáveis sem exibi-las
./mvnw -q spring-boot:run
```

Esperado: aplicação em `http://localhost:8080`, Flyway aplica `V1__criar_tabela_usuario` e `V2__criar_tabela_seguimento`, `ddl-auto: validate` passa. `GET /actuator/health` → `{"status":"UP"}`.

## 3. Roteiro manual (curl)

Use nomes fictícios. `-i` mostra status e cabeçalhos.

```bash
API=http://localhost:8080/api

# US1 — cadastro: 201 + User com token
curl -si -X POST $API/users -H 'Content-Type: application/json' \
  -d '{"user":{"username":"ana","email":"ana@exemplo.test","password":"segredo123"}}'

# US1 — duplicado: 409 {"errors":{"username":["has already been taken"], ...}}
curl -si -X POST $API/users -H 'Content-Type: application/json' \
  -d '{"user":{"username":"ana","email":"ANA@exemplo.test","password":"x"}}'

# US2 — login: 200 + User; guardar o token
TOKEN_ANA=$(curl -s -X POST $API/users/login -H 'Content-Type: application/json' \
  -d '{"user":{"email":"ana@exemplo.test","password":"segredo123"}}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# US2 — senha errada: 401 {"errors":{"email or password":["is invalid"]}}
curl -si -X POST $API/users/login -H 'Content-Type: application/json' \
  -d '{"user":{"email":"ana@exemplo.test","password":"errada"}}'

# US3 — usuário atual: 200; sem token: 401 token is missing
curl -si $API/user -H "Authorization: Token $TOKEN_ANA"
curl -si $API/user

# US3 — atualização parcial: 200 com bio/image novos
curl -si -X PUT $API/user -H "Authorization: Token $TOKEN_ANA" -H 'Content-Type: application/json' \
  -d '{"user":{"bio":"Gosto de skate","image":"https://exemplo.test/ana.png"}}'

# Segundo usuário para seguir
curl -s -X POST $API/users -H 'Content-Type: application/json' \
  -d '{"user":{"username":"bruno","email":"bruno@exemplo.test","password":"segredo456"}}' > /dev/null

# US4 — perfil anônimo: 200 following=false; inexistente: 404 profile not found
curl -si $API/profiles/bruno
curl -si $API/profiles/ninguem

# US5 — seguir (2x, idempotente), consultar, deixar de seguir
curl -si -X POST   $API/profiles/bruno/follow -H "Authorization: Token $TOKEN_ANA"   # following=true
curl -si -X POST   $API/profiles/bruno/follow -H "Authorization: Token $TOKEN_ANA"   # following=true
curl -si           $API/profiles/bruno        -H "Authorization: Token $TOKEN_ANA"   # following=true
curl -si -X DELETE $API/profiles/bruno/follow -H "Authorization: Token $TOKEN_ANA"   # following=false

# Bordas de autenticação: 401 token is invalid
curl -si $API/user -H "Authorization: Bearer $TOKEN_ANA"
curl -si $API/profiles/bruno -H 'Authorization: Token abc.def.ghi'

# Seguir a si mesmo: 422 profile cannot follow yourself
curl -si -X POST $API/profiles/ana/follow -H "Authorization: Token $TOKEN_ANA"
```

Em todas as respostas, conferir `Content-Type: application/json;charset=UTF-8` (equivalente a `application/json; charset=utf-8`) e a ausência de senha ou *hash* no corpo.

## 4. Conferência do esquema (opcional, somente leitura)

```bash
docker compose exec -T db sh -c \
  'psql -X -v ON_ERROR_STOP=1 -P pager=off -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "\\conninfo" -c "\\d usuario" -c "\\d seguimento"'
```

Esperado: restrições `pk_usuario`, `uq_usuario_username`, `uq_usuario_email`, `pk_seguimento`, `fk_seguimento_seguidor`, `fk_seguimento_seguido`, `ck_seguimento_nao_segue_a_si_mesmo` e `ix_seguimento_seguido_id`.

## 5. Recriar o banco local

Para repetir o roteiro do zero é preciso `docker compose down -v`, que apaga os dados locais e **exige confirmação explícita** (ver `tech.md`, seção "Reinicialização do banco local").
