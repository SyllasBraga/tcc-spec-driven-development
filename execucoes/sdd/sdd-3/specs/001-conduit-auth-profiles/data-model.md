# Data Model: Recorte A da API Conduit

**Feature**: [spec.md](./spec.md) | **Research**: [research.md](./research.md) | **Date**: 2026-11-01

Esquema `public`. Criado somente por migrations Flyway em `src/main/resources/db/migration`; o Hibernate apenas valida (`ddl-auto: validate`).

## Entidade `User` — tabela `users`

Pessoa cadastrada. Mapeada pela entidade JPA `User` em `com.example.blog.user.persistence`.

| Coluna | Tipo | Nulo | Regra |
|---|---|---|---|
| `id` | `bigint generated always as identity` | não | `pk_users`; vai no `sub` do token |
| `username` | `text` | não | `uq_users_username`; `ck_users_username_not_blank` (`btrim(username) <> ''`); diferencia maiúsculas |
| `email` | `text` | não | `uq_users_email`; `ck_users_email_lowercase` (`email = lower(email)`); `ck_users_email_not_blank` |
| `password_hash` | `text` | não | hash com prefixo do algoritmo (`{bcrypt}...`); nunca sai na API |
| `bio` | `text` | sim | `NULL` = sem biografia |
| `image` | `text` | sim | URL; `NULL` = sem imagem |
| `created_at` | `timestamptz` | não | `DEFAULT now()` |
| `updated_at` | `timestamptz` | não | `DEFAULT now()`; atualizado pela aplicação (com `Clock`) a cada `PUT /api/user` |

`bio` e `image` são as únicas colunas anuláveis: são opcionais pelo contrato (`type: [string, 'null']`).

**Regras de validação (aplicação)** — ver [research.md R9](./research.md#r9-validação-de-entrada):

- `username`, `email`, `password`: obrigatórios no cadastro; não vazios e não só espaços.
- `email`: formato válido; gravado com `trim` e em minúsculas (FR-035).
- `username`: gravado com `trim`.
- `bio`/`image`: `""` ou `null` explícito em `PUT /api/user` → `NULL` (FR-016).

**Comportamento na entidade** (sem setters genéricos, `tech.md` §6):

- `User(String username, String email, String passwordHash, Instant agora)` — criação.
- `alterarEmail(...)`, `alterarUsername(...)`, `alterarSenha(String novoHash)`, `alterarBio(...)`, `alterarImagem(...)`, `marcarAtualizado(Instant)`.
- `equals`/`hashCode` por `id`, tratando entidade não persistida (`id == null`) como diferente de qualquer outra.

## Entidade `Follow` — tabela `follows`

Relação direcional "seguidor → seguido". Mapeada pela entidade JPA `Follow` em `com.example.blog.profile.persistence`, com chave composta `FollowId(followerId, followedId)` (`@EmbeddedId`). As colunas são mapeadas como ids (`Long`), sem `@ManyToOne`, porque o recorte nunca navega da relação para o usuário.

| Coluna | Tipo | Nulo | Regra |
|---|---|---|---|
| `follower_id` | `bigint` | não | `fk_follows_follower` → `users(id)` `ON DELETE CASCADE` |
| `followed_id` | `bigint` | não | `fk_follows_followed` → `users(id)` `ON DELETE CASCADE`; `ix_follows_followed_id` |
| `created_at` | `timestamptz` | não | `DEFAULT now()` |

- `pk_follows` = `(follower_id, followed_id)` — garante unicidade do par e serve de índice para `follower_id`.
- `ck_follows_not_self`: `follower_id <> followed_id`.

**Estados da relação** (por par A→B):

```text
não segue ──POST /follow──▶ segue
   ▲  │                     │  │
   │  └──DELETE /follow─────┘  └──POST /follow (idempotente, continua "segue")
   └──DELETE /follow (idempotente, continua "não segue")
```

## Visão `Profile` (não persistida)

Montada pelo `ProfileService` a partir de `User` + existência de `Follow(viewer, user)`:

| Campo | Origem |
|---|---|
| `username` | `users.username` |
| `bio` | `users.bio` (ou `null`) |
| `image` | `users.image` (ou `null`) |
| `following` | `false` sem usuário autenticado; senão `exists` em `follows` para `(viewer.id, user.id)` |

## Migrations

| Arquivo | Conteúdo |
|---|---|
| `V1__criar_tabela_users.sql` | tabela `users` com `pk_users`, `uq_users_username`, `uq_users_email`, `ck_users_*` |
| `V2__criar_tabela_follows.sql` | tabela `follows` com `pk_follows`, `fk_follows_follower`, `fk_follows_followed`, `ck_follows_not_self`, `ix_follows_followed_id` |

Não há dados de referência nem seed obrigatório. Um seed de desenvolvimento fictício é opcional e, se criado, fica em `db/dev-seed` (perfil `dev`).

## Repositórios

- `UserRepository extends JpaRepository<User, Long>`: `findByEmail(String)`, `findByUsername(String)`, `existsByEmail(String)`, `existsByUsername(String)`, `existsByEmailAndIdNot(String, Long)`, `existsByUsernameAndIdNot(String, Long)`.
- `FollowRepository extends JpaRepository<Follow, FollowId>`: `existsById(FollowId)`; `seguir(followerId, followedId)` — `INSERT ... ON CONFLICT DO NOTHING` nativo; `deixarDeSeguir(followerId, followedId)` — `DELETE` com `WHERE`.
