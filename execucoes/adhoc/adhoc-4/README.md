# blog — API Conduit (recorte A)

Backend REST no estilo da API [RealWorld/Conduit](https://realworld-docs.netlify.app/), em Spring Boot 4.1.2, Java 25 e PostgreSQL 18.6.

Recorte implementado: cadastro e login com JWT, consulta e atualização do usuário autenticado, perfil público e seguir/deixar de seguir.

## Endpoints

| Método | Rota | Autenticação | Sucesso |
|---|---|---|---|
| `POST` | `/api/users` | não | `201` + `{"user": ...}` |
| `POST` | `/api/users/login` | não | `200` + `{"user": ...}` |
| `GET` | `/api/user` | obrigatória | `200` + `{"user": ...}` |
| `PUT` | `/api/user` | obrigatória | `200` + `{"user": ...}` |
| `GET` | `/api/profiles/{username}` | opcional | `200` + `{"profile": ...}` |
| `POST` | `/api/profiles/{username}/follow` | obrigatória | `200` + `{"profile": ...}` |
| `DELETE` | `/api/profiles/{username}/follow` | obrigatória | `200` + `{"profile": ...}` |

Autenticação: cabeçalho `Authorization: Token <jwt>`. Respostas em `application/json;charset=UTF-8`.

Erros seguem o formato `{"errors": {"<campo>": ["<mensagem>"]}}`:

- `422`: validação (`can't be blank`, `is invalid`), JSON malformado, e-mail ou username já usados **na atualização**;
- `409`: e-mail ou username já usados **no cadastro**;
- `401`: token ausente (`token: is missing`), token inválido (`token: is invalid`) ou login incorreto (`email or password: is invalid`);
- `404`: perfil inexistente (`profile: not found`).

Decisões de comportamento não fixadas pelo contrato:

- O e-mail é normalizado (sem espaços nas pontas, em minúsculas); o username diferencia maiúsculas de minúsculas.
- Em `PUT /api/user`, campos ausentes ou `null` não mudam. Em `bio` e `image`, texto vazio (`""`) remove o valor. É preciso informar pelo menos um campo.
- `GET /api/user` e `PUT /api/user` devolvem o mesmo token recebido; cadastro e login emitem um token novo.
- Seguir e deixar de seguir são idempotentes. Seguir ou deixar de seguir a si mesmo devolve `422`.
- A senha aceita até 72 bytes (limite do BCrypt).

## Executar localmente

Pré-requisitos: Java 25 e Docker.

1. Crie o `.env` a partir de `.env.example` (não versionado). `JWT_SECRET` deve ser uma chave aleatória em Base64 com pelo menos 32 bytes decodificados.
2. Suba o banco: `docker compose up -d db`
3. Suba a aplicação: `./mvnw spring-boot:run` (lê o `.env` da raiz; as migrations Flyway são aplicadas na subida).

## Testes

```bash
./mvnw verify
```

- Testes unitários (`*Test`, Surefire): regras dos serviços, sem Spring nem banco.
- Testes de integração (`*IT`, Failsafe): API completa com MockMvc e PostgreSQL 18.6 descartável via Testcontainers (exige Docker). Não usam o banco local de desenvolvimento.

## Estrutura

- `user` — cadastro, login e conta do usuário autenticado (`web`, `service`, `persistence`).
- `profile` — perfis públicos e relação de seguir.
- `shared` — segurança (JWT), tratamento de erros e configuração.
- `src/main/resources/db/migration` — migrations Flyway (`V1__criar_tabela_users`, `V2__criar_tabela_follows`).
