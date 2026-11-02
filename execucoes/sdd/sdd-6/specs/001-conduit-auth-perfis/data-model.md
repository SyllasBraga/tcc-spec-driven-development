# Data Model: Conduit — Recorte A

**Feature**: [spec.md](spec.md) | **Research**: [research.md](research.md)

O esquema é criado somente por migrations Flyway em `src/main/resources/db/migration`, e o Hibernate apenas o valida (`ddl-auto: validate`).

## Entidade `Usuario` → tabela `public.usuario`

| Coluna | Tipo | Nulo | Regra |
|---|---|---|---|
| `id` | `bigint generated always as identity` | não | `pk_usuario` |
| `email` | `varchar(255)` | não | `uq_usuario_email`; gravado em minúsculas e sem espaços nas pontas |
| `username` | `varchar(50)` | não | `uq_usuario_username`; comparação exata |
| `senha_hash` | `varchar(255)` | não | hash do `PasswordEncoder` delegante (`{bcrypt}...`); nunca exposto |
| `bio` | `text` | **sim** | opcional (`null` permitido pelo contrato) |
| `image` | `text` | **sim** | opcional, URL livre |
| `criado_em` | `timestamptz` | não | `DEFAULT now()` |
| `atualizado_em` | `timestamptz` | não | `DEFAULT now()`; atualizado pela aplicação (com `Clock` injetado) |

Restrições adicionais:

- `ck_usuario_email_nao_vazio`: `CHECK (btrim(email) <> '')`
- `ck_usuario_username_nao_vazio`: `CHECK (btrim(username) <> '')`

As restrições `UNIQUE` já criam índices sobre `email` e `username`, que são as colunas de busca do login e do perfil.

**Validação na borda** (Bean Validation, `422`):

- cadastro:
  - `username`: obrigatório, não vazio, no máximo 50 caracteres;
  - `email`: obrigatório, não vazio, formato válido, no máximo 255 caracteres;
  - `password`: obrigatório e não vazio;
- login: `email` e `password` obrigatórios e não vazios;
- atualização: ao menos um campo, e `email`, `username` e `password`, quando presentes, não podem ser nulos nem vazios.

**Comportamento da entidade** (sem *setters* genéricos):

- `alterarEmail(String)`
- `alterarUsername(String)`
- `alterarSenhaHash(String)`
- `alterarBio(String)`
- `alterarImage(String)`

`equals` e `hashCode` usam o `id` e tratam entidades ainda não persistidas como diferentes.

## Entidade `Seguimento` → tabela `public.seguimento`

Representa a relação direcionada "seguidor segue seguido".

| Coluna | Tipo | Nulo | Regra |
|---|---|---|---|
| `seguidor_id` | `bigint` | não | `fk_seguimento_seguidor` → `usuario(id)` `ON DELETE CASCADE` |
| `seguido_id` | `bigint` | não | `fk_seguimento_seguido` → `usuario(id)` `ON DELETE CASCADE` |
| `criado_em` | `timestamptz` | não | `DEFAULT now()` |

- **PK**: `pk_seguimento (seguidor_id, seguido_id)`. Garante no máximo uma relação por par e indexa `seguidor_id`.
- **Índice**: `ix_seguimento_seguido_id (seguido_id)`, para a chave estrangeira e para consultas de seguidores.
- Seguir a si mesmo é permitido, então não há `CHECK` para isso.
- O `ON DELETE CASCADE` é só defensivo, porque não há exclusão de usuário neste recorte.

Mapeamento JPA: entidade com `@EmbeddedId` (`SeguimentoId(seguidorId, seguidoId)`, que implementa `equals`/`hashCode`) e colunas de id simples. Não há associação `@ManyToOne`, porque o recorte só precisa de `existsById`, `save` e `deleteById`. Isso evita carregamento desnecessário.

## Objetos não persistidos

- **Token de acesso (JWT)**: claims `sub` (id do usuário), `iss`, `iat` e `exp`, com validade padrão de 24 h. É gerado a cada resposta do recurso `user` e não é armazenado.
- **Perfil**: projeção de `Usuario` (`username`, `bio`, `image`) mais `following`, que é `true` quando existe `seguimento(seguidor = usuário autenticado, seguido = perfil)`. Em consulta anônima, é sempre `false`.

## Migrations planejadas

| Arquivo | Conteúdo |
|---|---|
| `V1__criar_tabela_usuario.sql` | tabela `usuario`, PK, UNIQUEs e CHECKs |
| `V2__criar_tabela_seguimento.sql` | tabela `seguimento`, PK composta, FKs e índice |

## Transições de estado

- **Usuário**: criado no cadastro e alterado por `PUT /api/user`. Não há exclusão.
- **Seguimento**: inexistente → existe (`POST .../follow`, idempotente) → inexistente (`DELETE .../follow`, idempotente).
