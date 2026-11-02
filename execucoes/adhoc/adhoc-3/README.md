# blog — API Conduit (recorte A)

Backend REST no estilo Conduit/RealWorld: cadastro e login com JWT, usuário autenticado,
perfis públicos e seguir/deixar de seguir. Java 25, Spring Boot 4.1.2, Maven e PostgreSQL 18.6.

## Endpoints

| Método e rota | Autenticação | Descrição |
|---|---|---|
| `POST /api/users` | não | Cadastro (201) |
| `POST /api/users/login` | não | Login |
| `GET /api/user` | obrigatória | Usuário atual |
| `PUT /api/user` | obrigatória | Atualiza `email`, `username`, `password`, `bio`, `image` |
| `GET /api/profiles/{username}` | opcional | Perfil público |
| `POST /api/profiles/{username}/follow` | obrigatória | Seguir |
| `DELETE /api/profiles/{username}/follow` | obrigatória | Deixar de seguir |

O token vai no cabeçalho `Authorization: Token <jwt>`. Erros seguem `{"errors": {"<campo>": ["<mensagem>"]}}`:
422 (validação), 401 (token ausente/inválido ou credenciais erradas), 404 (perfil inexistente),
409 (e-mail ou username já usados).

Decisões de comportamento não fixadas pelo contrato:

- E-mail é único sem diferenciar maiúsculas; username diferencia.
- No `PUT /api/user`, campo omitido não muda; `"bio": null` ou `"image": null` apaga o valor.
- Seguir é idempotente; seguir a si mesmo retorna 422.

## Executar localmente

1. Copie `.env.example` para `.env` e preencha os valores (inclusive `JWT_SECRET`, com pelo menos 32 caracteres aleatórios).
   A aplicação lê o `.env` da raiz automaticamente; variáveis de ambiente têm precedência.
2. Suba o banco: `docker compose up -d db`
3. Rode a aplicação: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`

As migrations Flyway (`src/main/resources/db/migration`) são aplicadas na subida.

## Testes

`./mvnw verify` roda os testes unitários (`*Test`) e os de integração (`*IT`, com PostgreSQL via Testcontainers;
exige Docker).
