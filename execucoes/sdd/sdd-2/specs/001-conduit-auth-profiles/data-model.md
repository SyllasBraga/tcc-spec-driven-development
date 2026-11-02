# Data Model: Recorte A da API Conduit

**Feature**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

Esquema criado só por migrations Flyway em `src/main/resources/db/migration`, esquema `public`. Hibernate com `ddl-auto: validate`.

## Tabela `users` — migration `V1__criar_tabela_users.sql`

| Coluna | Tipo | Nulo | Padrão | Observação |
|---|---|---|---|---|
| `id` | `bigint generated always as identity` | não | | `pk_users` |
| `username` | `text` | não | | `uq_users_username` (sensível a maiúsculas); `ck_users_username_not_blank`: `btrim(username) <> ''` |
| `email` | `text` | não | | índice único `uq_users_email` em `lower(email)`; `ck_users_email_not_blank` |
| `password_hash` | `text` | não | | hash BCrypt; nunca exposto |
| `bio` | `text` | sim | `null` | opcional por regra de negócio |
| `image` | `text` | sim | `null` | URL opcional, texto livre |
| `created_at` | `timestamptz` | não | `now()` | |
| `updated_at` | `timestamptz` | não | `now()` | atualizado pela aplicação (`Clock` injetado) |

O nome `users` evita a palavra reservada `user`.

**Entidade JPA** `UserEntity` (`com.example.blog.user.persistence`): id, username, email, passwordHash, bio, image, createdAt (`Instant`), updatedAt (`Instant`). Construtor protegido sem argumentos, `equals`/`hashCode` pelo id. Comportamentos com significado de negócio: `alterarEmail`, `alterarUsername`, `alterarSenha(hash)`, `alterarBio`, `alterarImage`.

**Repository** `UserRepository extends JpaRepository<UserEntity, Long>`:
- `Optional<UserEntity> findByUsername(String username)`
- `Optional<UserEntity> findByEmailIgnoreCase(String email)`
- `boolean existsByUsername(String username)` / `existsByUsernameAndIdNot(String, Long)`
- `boolean existsByEmailIgnoreCase(String email)` / `existsByEmailIgnoreCaseAndIdNot(String, Long)`

### Regras de validação (da spec)

| Campo | Cadastro | Atualização | Erro |
|---|---|---|---|
| `username` | obrigatório, não em branco, único | opcional; se enviado, não em branco e único (exceto o próprio) | `422 can't be blank` / `409 has already been taken` |
| `email` | obrigatório, não em branco, formato válido, único sem diferenciar maiúsculas | idem, opcional | `422 can't be blank` / `422 is invalid` / `409 has already been taken` |
| `password` | obrigatório, não em branco | opcional; se enviado, não em branco | `422 can't be blank` |
| `bio`, `image` | não aceitos no cadastro (iniciam `null`) | opcionais; `null` limpa | — |

Espaços nas pontas de `username` e `email` são removidos antes de validar e gravar. A senha é usada como enviada.

## Tabela `follows` — migration `V2__criar_tabela_follows.sql`

| Coluna | Tipo | Nulo | Observação |
|---|---|---|---|
| `follower_id` | `bigint` | não | `fk_follows_follower` → `users(id)` |
| `followed_id` | `bigint` | não | `fk_follows_followed` → `users(id)`; índice `ix_follows_followed_id` |
| `created_at` | `timestamptz` | não | padrão `now()` |

- Chave primária `pk_follows (follower_id, followed_id)`: no máximo uma relação por par e índice para `follower_id`.
- `ck_follows_not_self`: `follower_id <> followed_id`.

**Entidade JPA** `FollowEntity` (`com.example.blog.profile.persistence`) com `@EmbeddedId FollowId(followerId, followedId)` e `createdAt`. As colunas são ids simples, sem associação `@ManyToOne`, para não carregar usuários sem necessidade.

**Repository** `FollowRepository extends JpaRepository<FollowEntity, FollowId>`:
- `boolean existsById(FollowId id)` (herdado)
- `void deleteById(FollowId id)` (herdado; sem efeito quando não existe)

## Relações

```text
users 1 ──< follows >── 1 users
      (follower_id)      (followed_id)
```

## Transições de estado

Relação de seguir entre A (autenticado) e B:

| Estado atual | `POST /follow` | `DELETE /follow` |
|---|---|---|
| A não segue B | cria → `following: true` | sem mudança → `following: false` |
| A segue B | sem mudança → `following: true` | remove → `following: false` |
| A = B | `422 cannot follow yourself` | sem mudança → `following: false` |

## Projeções de saída (não são entidades)

- **User** (`UserResponse`): `email`, `token`, `username`, `bio`, `image`. `bio`/`image` sempre presentes, mesmo `null`.
- **Profile** (`ProfileResponse`): `username`, `bio`, `image`, `following`.
