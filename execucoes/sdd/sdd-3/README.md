# blog — Recorte A da API Conduit

API REST (somente backend) do Recorte A da API Conduit/RealWorld: cadastro, login com JWT, usuário autenticado,
perfil público e seguir/deixar de seguir. Spring Boot 4.1.2, Java 25, PostgreSQL 18.6 e Flyway.

Contrato: [`specs/001-conduit-auth-profiles/contracts/openapi.yaml`](specs/001-conduit-auth-profiles/contracts/openapi.yaml).

## Pré-requisitos

- JDK 25
- Docker (banco local e Testcontainers)

## Configuração

Crie o `.env` na raiz a partir do `.env.example` (o `.env` não é versionado):

| Variável | Valor |
|---|---|
| `POSTGRES_DB` | `sdd_3` |
| `POSTGRES_USER` | usuário local |
| `POSTGRES_PASSWORD` | senha local aleatória (nunca reutilize senhas reais) |
| `DB_PORT` | `5432` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/sdd_3` |
| `JWT_SECRET` | Base64 de 32 bytes aleatórios, gravado direto no `.env` sem exibir o valor (ver abaixo) |

```bash
python -c "import base64,secrets;open('.env','a').write('JWT_SECRET='+base64.b64encode(secrets.token_bytes(32)).decode()+'\n')"
```

O perfil `dev` importa o `.env` automaticamente (`spring.config.import`).

## Comandos

```bash
docker compose up -d db                                     # sobe o PostgreSQL local
./mvnw -q verify                                            # testes unitários e de integração (Testcontainers)
./mvnw -q spring-boot:run -Dspring-boot.run.profiles=dev    # sobe a API em http://localhost:8080
```

Os testes usam um PostgreSQL 18.6 descartável via Testcontainers, não o banco local.

## Endpoints

O token vai no cabeçalho `Authorization: Token <jwt>`.

| Método | Rota | Autenticação |
|---|---|---|
| `POST` | `/api/users` | não |
| `POST` | `/api/users/login` | não |
| `GET` | `/api/user` | obrigatória |
| `PUT` | `/api/user` | obrigatória |
| `GET` | `/api/profiles/{username}` | opcional |
| `POST` | `/api/profiles/{username}/follow` | obrigatória |
| `DELETE` | `/api/profiles/{username}/follow` | obrigatória |

Erros seguem o formato `{"errors":{"<chave>":["<mensagem>"]}}`.
