# Data Model: Conduit — Recorte A

Esquema criado somente por migrations Flyway em `src/main/resources/db/migration`, esquema `public`.

## Tabela `usuario` (migration `V1__criar_tabela_usuario.sql`)

| Coluna | Tipo | Nulo | Regra |
|---|---|---|---|
| `id` | `bigint generated always as identity` | não | `pk_usuario` |
| `username` | `varchar(255)` | não | `uq_usuario_username` (diferencia maiúsculas) |
| `email` | `varchar(255)` | não | índice único `uq_usuario_email_lower` em `lower(email)` |
| `senha_hash` | `varchar(100)` | não | hash BCrypt; nunca exposto |
| `bio` | `text` | sim | |
| `image` | `text` | sim | URL livre |
| `criado_em` | `timestamptz` | não | `DEFAULT now()` |
| `atualizado_em` | `timestamptz` | não | `DEFAULT now()`; atualizado pela aplicação |

Restrições adicionais:
- `ck_usuario_username_nao_vazio`: `btrim(username) <> ''`
- `ck_usuario_email_nao_vazio`: `btrim(email) <> ''`

**Entidade JPA** `Usuario` (`usuario.persistence`): campos acima; métodos de negócio `alterarEmail`, `alterarUsername`, `alterarSenhaHash`, `alterarBio`, `alterarImage`. `equals`/`hashCode` pelo `id` (classe estável, entidade nova não é igual a nenhuma outra).

**Repository** `UsuarioRepository`:
- `Optional<Usuario> findByUsername(String username)`
- `Optional<Usuario> findByEmailIgnoreCase(String email)` (gera `lower(email) = lower(?)`)
- `boolean existsByUsername(String username)`, `boolean existsByEmailIgnoreCase(String email)`
- `boolean existsByUsernameAndIdNot(...)`, `boolean existsByEmailIgnoreCaseAndIdNot(...)` para o `PUT`.

## Tabela `seguimento` (migration `V2__criar_tabela_seguimento.sql`)

Relação direcionada "seguidor segue seguido".

| Coluna | Tipo | Nulo | Regra |
|---|---|---|---|
| `seguidor_id` | `bigint` | não | `fk_seguimento_seguidor` → `usuario(id)` `ON DELETE CASCADE` |
| `seguido_id` | `bigint` | não | `fk_seguimento_seguido` → `usuario(id)` `ON DELETE CASCADE` |
| `criado_em` | `timestamptz` | não | `DEFAULT now()` |

Restrições e índices:
- `pk_seguimento` em `(seguidor_id, seguido_id)` — impede duplicidade (FR-030).
- `ck_seguimento_nao_si_mesmo`: `seguidor_id <> seguido_id` (FR-032).
- `ix_seguimento_seguido_id` em `seguido_id` (a PK já cobre `seguidor_id`).

**Entidade JPA** `Seguimento` (`perfil.persistence`) com chave composta `SeguimentoId` (`@Embeddable` record não é permitido para entidades; usar classe `@Embeddable` com `equals`/`hashCode`) guardando apenas os ids — sem associações `@ManyToOne` carregadas, evitando N+1.

**Repository** `SeguimentoRepository`:
- `boolean existsByIdSeguidorIdAndIdSeguidoId(long seguidorId, long seguidoId)`
- `void deleteByIdSeguidorIdAndIdSeguidoId(...)` (ou `deleteById`).

## Regras de validação (origem nos requisitos)

| Campo | Cadastro | Login | Atualização |
|---|---|---|---|
| `username` | obrigatório, não branco; único → 409 | — | se presente: não nulo/branco; único → 422 |
| `email` | obrigatório, não branco, formato e-mail; único sem maiúsculas → 409 | obrigatório, não branco | se presente: não nulo/branco, formato; único → 422 |
| `password` | obrigatório, não branco | obrigatório, não branco | se presente: não nulo/branco; grava novo hash |
| `bio` / `image` | — (inicia `null`) | — | se presente: valor ou `null` (apaga) |

Pelo menos um campo aceito deve estar presente no `PUT` (FR-019).

## Visões derivadas (não armazenadas)

- **Usuário autenticado** (`user`): `email`, `token`, `username`, `bio`, `image`.
- **Perfil** (`profile`): `username`, `bio`, `image`, `following` — `following` = existe `seguimento(usuarioAtual, alvo)`; `false` para anônimo.

## Transições de estado

`seguimento`: inexistente → (POST follow) → existente → (DELETE follow) → inexistente. Ambas as operações são idempotentes.
