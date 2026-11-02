# Data Model: Recorte A da API Conduit

Esquema PostgreSQL 18.6 criado só por migrations Flyway; Hibernate em `ddl-auto: validate`. Decisões em [research.md](research.md).

## Tabela `public.usuario` — `V1__criar_tabela_usuario.sql`

| Coluna | Tipo | Nulo | Regra |
|---|---|---|---|
| `id` | `bigint generated always as identity` | não | `pk_usuario` |
| `email` | `varchar(255)` | não | `uq_usuario_email`; gravado com `trim` e minúsculas |
| `username` | `varchar(255)` | não | `uq_usuario_username`; gravado com `trim`, caixa preservada |
| `senha_hash` | `varchar(255)` | não | *hash* delegado (`{bcrypt}...`) |
| `bio` | `text` | **sim** | contrato permite `null` |
| `image` | `text` | **sim** | contrato permite `null` |
| `criado_em` | `timestamptz` | não | `DEFAULT now()` |
| `atualizado_em` | `timestamptz` | não | `DEFAULT now()`; atualizado pela aplicação via `Clock` |

- `ck_usuario_email_nao_vazio`: `btrim(email) <> ''`.
- `ck_usuario_username_nao_vazio`: `btrim(username) <> ''`.
- `bio` e `image` anuláveis por exigência do contrato (exceção justificada ao "`NOT NULL` por padrão").
- Os índices únicos de `uq_` cobrem as buscas por e-mail (login) e username (perfil).

**Entidade JPA** `com.example.blog.usuario.persistence.Usuario`: classe (não record), construtor protegido sem argumentos para o JPA, construtor de criação com e-mail, username e *hash*. Métodos de negócio em vez de setters: `alterarEmail`, `alterarUsername`, `alterarSenhaHash`, `alterarBio`, `alterarImage`. `equals`/`hashCode` pelo `id`, tratando entidade não persistida.

**Repository** `UsuarioRepository extends JpaRepository<Usuario, Long>`: `findByEmail`, `findByUsername`, `existsByEmail`, `existsByUsername`, `existsByEmailAndIdNot`, `existsByUsernameAndIdNot`.

## Tabela `public.seguidor` — `V2__criar_tabela_seguidor.sql`

| Coluna | Tipo | Nulo | Regra |
|---|---|---|---|
| `seguidor_id` | `bigint` | não | `fk_seguidor_usuario_seguidor` → `usuario(id)` `ON DELETE CASCADE` |
| `seguido_id` | `bigint` | não | `fk_seguidor_usuario_seguido` → `usuario(id)` `ON DELETE CASCADE` |
| `criado_em` | `timestamptz` | não | `DEFAULT now()` |

- `pk_seguidor (seguidor_id, seguido_id)`: garante unicidade do par; o índice da PK cobre a FK `seguidor_id`.
- `ix_seguidor_seguido_id (seguido_id)`: índice da segunda FK.
- `ck_seguidor_distintos`: `seguidor_id <> seguido_id`.

**Entidade JPA** `com.example.blog.perfil.persistence.Seguimento`: chave composta `@EmbeddedId SeguimentoId(seguidorId, seguidoId)` (classe `@Embeddable` com `equals`/`hashCode`). Guarda só os ids, sem `@ManyToOne` para `Usuario`, para não acoplar entidades de pacotes de funcionalidade diferentes.

**Repository** `SeguimentoRepository extends JpaRepository<Seguimento, SeguimentoId>`: `existsById`, `deleteById`.

## Regras de validação (camada `web` e `service`)

| Campo | Cadastro | Login | Atualização |
|---|---|---|---|
| `email` | obrigatório, não branco, formato e-mail, único | obrigatório, não branco | opcional; se presente: não branco, formato, único (exceto o próprio) |
| `username` | obrigatório, não branco, único | — | opcional; se presente: não branco, único (exceto o próprio) |
| `password` | obrigatório, não branco | obrigatório, não branco | opcional; se presente: não branco; regrava o *hash* |
| `bio` | — (nasce `null`) | — | opcional; `null` limpa |
| `image` | — (nasce `null`) | — | opcional; `null` limpa |

Pelo menos um campo aceito é obrigatório na atualização.

## Modelos de API (records em `web`)

- Entrada: `RegistroRequest(user: NovoUsuario)`, `LoginRequest(user: Credenciais)`, `AtualizacaoUsuarioRequest(user: ObjectNode)`.
- Saída: `UsuarioResponse(user: UsuarioDados{email, token, username, bio, image})`, `PerfilResponse(profile: PerfilDados{username, bio, image, following})`.
- Erro: `ErroResponse(errors: Map<String, List<String>>)` em `shared/web`.
- Comando de serviço: `AtualizacaoUsuario` (por campo: informado sim/não e valor).

## Transições de estado

- Seguimento: `não segue` --`POST follow`--> `segue`; `segue` --`DELETE follow`--> `não segue`. As duas transições são idempotentes.
- Usuário: criado no cadastro, alterado no update; sem exclusão no escopo.
