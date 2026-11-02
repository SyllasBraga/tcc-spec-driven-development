# Data Model: Conduit — Recorte A

**Fase 1 do plano** · Data: 2026-11-01 · Spec: [spec.md](spec.md) · Decisões: [research.md](research.md)

## Visão geral

```text
usuario 1 ──< seguimento >── 1 usuario
         (seguidor_id)    (seguido_id)
```

Duas tabelas no esquema `public`, criadas por migrations Flyway em `src/main/resources/db/migration`:

| Migration | Conteúdo |
|---|---|
| `V1__criar_tabela_usuario.sql` | tabela `usuario`, PK, unicidades de username e e-mail |
| `V2__criar_tabela_seguimento.sql` | tabela `seguimento`, PK composta, FKs, check e índice |

O token de acesso **não** é persistido (JWT sem estado).

---

## Entidade `Usuario` (tabela `usuario`)

Pacote: `com.example.blog.usuario.persistence`

| Coluna | Tipo SQL | Nulo | Campo Java | Regras |
|---|---|---|---|---|
| `id` | `bigint generated always as identity` | não | `Long id` | `pk_usuario` |
| `username` | `text` | não | `String username` | `uq_usuario_username` (sensível a maiúsculas); `ck_usuario_username_nao_vazio`: `btrim(username) <> ''` |
| `email` | `text` | não | `String email` | `uq_usuario_email`: índice único em `lower(email)`; `ck_usuario_email_nao_vazio`: `btrim(email) <> ''` |
| `senha_hash` | `text` | não | `String senhaHash` | *hash* do `PasswordEncoder` (`{bcrypt}...`); nunca exposto |
| `bio` | `text` | **sim** | `String bio` | opcional (contrato: `string \| null`) |
| `imagem` | `text` | **sim** | `String imagem` | opcional, URL livre (contrato: `string \| null`) |
| `criado_em` | `timestamptz` | não | `Instant criadoEm` | `DEFAULT now()` |
| `atualizado_em` | `timestamptz` | não | `Instant atualizadoEm` | `DEFAULT now()`; atualizado pela aplicação a cada alteração |

`bio` e `imagem` são as únicas colunas anuláveis: o contrato as define como `string | null`.

**Validações (aplicação, na borda e no serviço)**

- `username`, `email`, `password`: obrigatórios no cadastro; no PUT, se enviados, não podem ser `null`, vazios ou só espaços → `can't be blank`.
- `email`: formato de e-mail (Bean Validation `@Email`) → `is invalid`.
- `username` e `email` passam por `trim` antes de gravar e comparar.
- Unicidade: e-mail sem diferenciar maiúsculas; username exato. Cadastro → 409; PUT → 422; o próprio usuário pode reenviar seus valores atuais.

**Comportamento (métodos de negócio, sem setters genéricos)**

- construtor `Usuario(username, email, senhaHash, Instant agora)` — `bio` e `imagem` nulos;
- `alterarEmail(String)`, `alterarUsername(String)`, `alterarSenhaHash(String)`, `alterarBio(String nullable)`, `alterarImagem(String nullable)`, cada um atualizando `atualizadoEm` via `Clock` recebido do serviço (ou `registrarAlteracao(Instant)`);
- `equals`/`hashCode` pelo `id` (entidades não persistidas não são iguais entre si).

**Repositório** `UsuarioRepository extends JpaRepository<Usuario, Long>`

- `Optional<Usuario> findByUsername(String username)`
- `Optional<Usuario> findByEmailIgnoreCase(String email)`
- `boolean existsByUsername(String username)`
- `boolean existsByEmailIgnoreCase(String email)`
- `boolean existsByUsernameAndIdNot(String username, Long id)`
- `boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id)`

`findByEmailIgnoreCase` gera `lower(email) = lower(?)`, que usa o índice `uq_usuario_email`.

---

## Entidade `Seguimento` (tabela `seguimento`)

Pacote: `com.example.blog.perfil.persistence`

| Coluna | Tipo SQL | Nulo | Campo Java | Regras |
|---|---|---|---|---|
| `seguidor_id` | `bigint` | não | `SeguimentoId.seguidorId` | `fk_seguimento_seguidor` → `usuario(id)` |
| `seguido_id` | `bigint` | não | `SeguimentoId.seguidoId` | `fk_seguimento_seguido` → `usuario(id)`; `ix_seguimento_seguido_id` |
| `criado_em` | `timestamptz` | não | `Instant criadoEm` | `DEFAULT now()` |

- `pk_seguimento`: `(seguidor_id, seguido_id)` — um par existe no máximo uma vez (FR-023, SC-006). O índice da PK cobre consultas por `seguidor_id`.
- `ck_seguimento_nao_segue_a_si_mesmo`: `seguidor_id <> seguido_id`.
- FKs `ON DELETE` padrão (`NO ACTION`): não há exclusão de usuário no recorte.
- Mapeamento: `@EmbeddedId SeguimentoId`, classe `@Embeddable` com construtor sem argumentos protegido, campos `seguidorId`/`seguidoId` e `equals`/`hashCode` pelos dois ids (escolha mais portável entre versões do Hibernate). Sem associações `@ManyToOne`, apenas ids.

**Repositório** `SeguimentoRepository extends JpaRepository<Seguimento, SeguimentoId>`

- `existsById(SeguimentoId)` — calcula `following`;
- `deleteById(SeguimentoId)` — deixar de seguir.

**Estados da relação** (para um par seguidor → seguido)

```text
[não segue] --POST follow--> [segue]
[segue]     --POST follow--> [segue]       (idempotente, sem duplicar)
[segue]     --DELETE follow--> [não segue]
[não segue] --DELETE follow--> [não segue] (idempotente)
seguidor == seguido --POST follow--> 422 (estado inalterado)
```

---

## Objetos de API (records, pacotes `web`)

Não são entidades; mapeamento explícito entidade → DTO no serviço ou em um mapeador da camada web.

| Record | Pacote | Forma JSON |
|---|---|---|
| `CadastroRequest(NovoUsuario user)` / `NovoUsuario(username, email, password)` | `usuario.web` | `{"user":{"username","email","password"}}` |
| `LoginRequest(Credenciais user)` / `Credenciais(email, password)` | `usuario.web` | `{"user":{"email","password"}}` |
| `AtualizacaoRequest(JsonNode user)` | `usuario.web` | `{"user":{...campos opcionais}}` (ver research R7) |
| `UsuarioEnvelope(UsuarioResponse user)` / `UsuarioResponse(email, token, username, bio, image)` | `usuario.web` | contrato `User` |
| `PerfilEnvelope(PerfilResponse profile)` / `PerfilResponse(username, bio, image, following)` | `perfil.web` | contrato `Profile` |
| `ErrosResponse(Map<String, List<String>> errors)` | `shared.erro` | contrato `GenericErrorModel` |

Comandos e resultados de serviço (pacote `service`): `NovoCadastro`, `AtualizacaoUsuario` (com `CampoAtualizado<T>`), `UsuarioAutenticadoDto(id, email, username, bio, imagem)` + token, `PerfilDto(username, bio, imagem, seguindo)`.

Mapeamento de nomes: coluna `imagem` ↔ campo JSON `image`; `senha_hash` nunca sai do serviço.
