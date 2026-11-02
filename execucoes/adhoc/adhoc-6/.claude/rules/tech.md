# Regras técnicas do projeto

> Este documento consolida as regras de acesso ao PostgreSQL, operação do banco local com Docker e desenvolvimento Java/Spring Boot.

---

## Sumário

- [Acesso ao banco de dados PostgreSQL via terminal](#acesso-ao-banco-de-dados-postgresql-via-terminal)
- [Banco local em Docker e migrations](#banco-local-em-docker-e-migrations)
- [Padrões de desenvolvimento Java e Spring Boot](#padrões-de-desenvolvimento-java-e-spring-boot)

---

# Acesso ao banco de dados PostgreSQL via terminal

Estas regras definem **como o agente deve acessar o banco**. A especificação de conexão da empresa ou do projeto define **onde e com quais credenciais** (conexão, usuário e senha). Esta regra não contém credenciais e nunca deve passar a contê-las. Quando duas regras conflitarem, vale a **mais restritiva**.

## 0. Especificação de conexão (preencher com dados não secretos)

Preencha o bloco abaixo com as informações **não secretas** da especificação. Nunca escreva senhas aqui.

```
Documento da especificação: <caminho ou link interno>
Ambiente permitido ao agente: banco PostgreSQL local em contêiner Docker (ver seção Banco local em Docker e migrations)
Host: <host do ambiente permitido>
Porta: <porta>
Banco: <nome do banco>
Usuário do agente: <usuário com privilégios mínimos>
Origem da senha: <variável de ambiente PGPASSWORD, arquivo ~/.pgpass ou PGSERVICE>
```

## 1. Credenciais

- Use **exclusivamente** a conexão definida na especificação. Não invente, não troque e não adivinhe host, banco, usuário ou senha.
- **Credenciais nunca ficam em arquivos versionados**: código, `application.yml` versionado, regras técnicas, specs, README, scripts, histórico de commits ou mensagens do chat.
- A senha é fornecida pelo ambiente, por um destes meios, nesta ordem de preferência:
  1. arquivo de senhas **`~/.pgpass`** (ou `PGPASSFILE`), com permissão `0600`;
  2. arquivo de serviços `pg_service.conf` (`PGSERVICE`) combinado com o arquivo de senhas;
  3. variáveis de ambiente do libpq: `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`.
- **Nunca passe a senha na linha de comando** nem em URI de conexão (`postgresql://usuario:senha@host/banco`). Ela ficaria visível no histórico do shell, na lista de processos e nos registros do agente.
- **Nunca imprima, repita ou registre** senhas ou variáveis de ambiente. Isso inclui `env`, `printenv`, `set`, `echo $PGPASSWORD` e `cat ~/.pgpass`, além de respostas, commits, logs e mensagens.
- Se as credenciais não estiverem configuradas, **pare** e peça ao usuário para configurá-las fora do chat. Nunca peça que a senha seja colada na conversa.
- Se uma credencial aparecer por engano em um arquivo, em uma saída de terminal ou em um commit, avise imediatamente e recomende a **troca da senha**.

## 2. Ambientes e privilégios

- O agente opera por padrão **apenas em desenvolvimento local ou em homologação isolada**. Quando o projeto tiver um banco local em Docker, **ele é o ambiente permitido**. Acesso a **produção é proibido**, salvo autorização explícita do usuário e, mesmo assim, apenas com usuário **somente leitura**.
- Antes do primeiro comando da sessão, confirme o destino com `\conninfo` e informe ao usuário o banco, o usuário e o host efetivamente conectados. Se não forem os esperados, interrompa.
- Nunca aponte para banco de outro projeto, cliente ou equipe.
- O usuário do agente deve ter **privilégios mínimos**: leitura por padrão e escrita somente nas tabelas necessárias em ambiente de desenvolvimento. Não use superusuário nem o dono do esquema em ambientes compartilhados. Se o usuário configurado tiver mais privilégios do que o necessário, alerte o usuário.
- **Não inclua `psql`** (nem `docker compose exec ... psql`) na lista `permissions.allow` do Claude Code (`.claude/settings.json` ou `.claude/settings.local.json`). Mantenha a aprovação manual para qualquer comando que altere dados ou esquema.

## 3. Como invocar o `psql`

- Use sempre o modo **não interativo** e explícito. Nunca abra uma sessão interativa que bloqueie o terminal.
- Flags padrão: `-X` (ignora o `~/.psqlrc`), `-w` (nunca pedir senha, para não travar o terminal), `-v ON_ERROR_STOP=1` (interrompe no primeiro erro) e `-P pager=off` (sem paginador).
- Use `-c "<comando>"` para comandos curtos e `-f arquivo.sql` para scripts.
- Para sessões de leitura, force o modo somente leitura e um tempo máximo por comando:

```bash
PGOPTIONS='-c default_transaction_read_only=on -c statement_timeout=30000' \
  psql -X -w -v ON_ERROR_STOP=1 -P pager=off \
  -c "SELECT id, nome FROM cliente ORDER BY id LIMIT 20;"
```

- O modo somente leitura da sessão é uma **camada extra de proteção** e pode ser contornado por quem tem permissão de escrita. A proteção real vem dos **privilégios do usuário do banco**.
- Os parâmetros de conexão vêm do ambiente. Não os repita na linha de comando sem necessidade.
- Nunca use `\!` (comando de shell dentro do `psql`) nem execute scripts de origem desconhecida ou não revisados.

## 4. Consultas de leitura (modo padrão)

- **Leitura é o modo padrão.** Antes de consultar uma tabela desconhecida, inspecione a estrutura: `\dt`, `\d nome_tabela`, `\dn`, `\di`.
- Liste **colunas explícitas**. Evite `SELECT *`.
- Em exploração, use sempre **`LIMIT`** (de 20 a 100 linhas) e filtros com `WHERE`. Evite varrer tabelas grandes sem filtro.
- Para saber o tamanho de uma tabela grande, prefira estimativas (`pg_class.reltuples`) a `COUNT(*)` completo.
- Use `EXPLAIN` para analisar planos. **`EXPLAIN ANALYZE` executa a consulta de verdade.** Em `INSERT`, `UPDATE` e `DELETE`, use-o somente dentro de `BEGIN; ... ROLLBACK;`.
- Defina `statement_timeout` e, quando houver risco de bloqueio, `lock_timeout`.
- Em ambientes compartilhados, evite consultas pesadas que concorram com outras pessoas.

## 5. Proteção de dados sensíveis

- **Tudo o que uma consulta retorna entra no contexto do modelo e no histórico da conversa.** Trate o resultado como informação que sai da máquina.
- Não consulte nem exiba dados pessoais, credenciais, tokens, *hashes* de senha ou dados financeiros sem necessidade real.
- Selecione apenas as colunas necessárias. Prefira **agregações** (`COUNT`, `GROUP BY`) a linhas brutas e **mascare** valores quando precisar mostrar exemplos (por exemplo, `left(email, 3) || '***'`).
- **Nunca copie dados reais** do banco para código, testes, *fixtures*, especificações, prompts ou commits. Use dados fictícios.
- Não gere *dumps* nem exportações de dados reais (`pg_dump`, `COPY ... TO`, `\copy`) sem autorização explícita do usuário.
- Respeite a LGPD e as políticas de dados da empresa.

## 6. Alteração de dados (DML)

- **Nunca** execute `INSERT`, `UPDATE`, `DELETE` ou `TRUNCATE` sem pedido explícito do usuário e sem confirmação do comando exato.
- Siga este procedimento:
  1. Mostre ao usuário o SQL completo e o ambiente de destino.
  2. Execute um `SELECT` com o mesmo `WHERE` para contar as linhas que serão afetadas.
  3. Faça um **ensaio** em transação com `ROLLBACK` e confira o número de linhas afetadas (use `RETURNING` quando útil).
  4. Só execute a versão com `COMMIT` depois de confirmação explícita do usuário.
- Exemplo de script para `-f` (ensaio):

```sql
BEGIN;
UPDATE cliente SET ativo = false WHERE id = 42 RETURNING id, ativo;
ROLLBACK; -- trocar por COMMIT somente após confirmação do usuário
```

- `UPDATE` e `DELETE` **sempre com `WHERE`**. Sem `WHERE`, só com autorização explícita e separada.
- Operações em massa (`TRUNCATE`, `DELETE` de grande volume) exigem confirmação própria, com descrição do impacto, e só podem ocorrer em banco de desenvolvimento descartável.
- **Nunca desabilite** *triggers*, restrições ou verificações (`ALTER TABLE ... DISABLE TRIGGER`, `session_replication_role`) para "fazer funcionar".
- Ao final, informe ao usuário exatamente o que foi alterado.

## 7. Alteração de esquema (DDL)

- Mudanças de esquema do projeto vão em **migrations versionadas** (Flyway ou Liquibase), revisadas e versionadas no repositório. Elas não são feitas por comandos soltos no `psql`.
- DDL direto só é aceitável para experimentos em banco local descartável. Se o experimento der certo, transforme-o em migration.
- **Nunca** execute `DROP`, `ALTER ... DROP COLUMN`, `DROP SCHEMA` ou `DROP DATABASE` sem confirmação explícita. Não use `DROP ... CASCADE` sem antes listar as dependências afetadas.
- Antes de qualquer DDL, verifique o objeto existente (`\d`) e considere **bloqueios** em tabelas grandes. Use `CREATE INDEX CONCURRENTLY` nesses casos (não funciona dentro de transação) e defina `lock_timeout`.
- O agente **não aplica migrations em produção**.

## 8. Comandos que exigem autorização explícita

Os comandos abaixo só podem ser executados com pedido explícito e específico do usuário, e nunca em produção:

- `DROP DATABASE`, `DROP SCHEMA`, `TRUNCATE`;
- `DELETE` ou `UPDATE` sem `WHERE`;
- `ALTER SYSTEM`;
- `CREATE`, `ALTER` ou `DROP` de `ROLE` ou `USER`, e `GRANT` ou `REVOKE`;
- `COPY ... PROGRAM`;
- `pg_terminate_backend` e `pg_cancel_backend` em sessões de terceiros;
- `VACUUM FULL` e `REINDEX` em tabelas grandes;
- instalação de extensões.

Nunca crie nem altere usuários, senhas ou permissões por conta própria.

## 9. Erros e relato

- Se um comando falhar, mostre o erro (**sem credenciais**) e diagnostique a causa. Não repita variações de comandos destrutivos para "tentar de novo".
- Em caso de **`permission denied`**, pare e relate. Não tente contornar com outro usuário, elevação de privilégios ou outro banco.
- Em falhas de conexão, verifique se as variáveis estão definidas **sem imprimir seus valores**, e confira rede, VPN e `pg_isready`. Não tente outros hosts.
- Ao concluir, informe ao usuário o comando executado (sem segredos), o ambiente, e um resumo do resultado.
- Limite a saída no terminal. Não despeje milhares de linhas.

## 10. Banco de dados no desenvolvimento da aplicação

- A aplicação lê as credenciais da **configuração externa** (variáveis de ambiente ou cofre de segredos), nunca do código nem de arquivos versionados.
- Testes automatizados **não** usam o banco compartilhado de desenvolvimento. Usam banco descartável (por exemplo, Testcontainers).
- Os dados de teste são fictícios e criados pelo próprio teste ou por *seed* de teste. Não dependa de dados existentes no banco de desenvolvimento.
- Use o `psql` para **inspecionar e depurar**. O esquema e a evolução dele vêm das migrations.

## 11. Checklist antes de executar um comando no banco

- [ ] O ambiente foi confirmado com `\conninfo` e não é produção.
- [ ] As credenciais vêm do ambiente ou do `~/.pgpass`, e nunca da linha de comando.
- [ ] Consultas de leitura têm colunas explícitas, filtro, `LIMIT` e `statement_timeout`.
- [ ] Alterações têm pedido explícito, SQL mostrado, contagem prévia, ensaio com `ROLLBACK` e confirmação antes do `COMMIT`.
- [ ] Mudanças de esquema do projeto vão em migration versionada.
- [ ] Nenhum dado sensível será exibido, copiado ou gravado em arquivos.
- [ ] O comando não está na lista da seção 8, ou há autorização explícita para ele.

---

# Banco local em Docker e migrations

Esta seção define **onde o banco roda** (um PostgreSQL local em contêiner Docker) e **como o esquema muda** (somente por migrations versionadas). Ela complementa a seção [Acesso ao banco de dados PostgreSQL via terminal](#acesso-ao-banco-de-dados-postgresql-via-terminal), que define como acessar o banco com `psql`. Em caso de conflito, vale a regra mais restritiva.

## 1. Princípios

- O banco usado pelo agente é **um PostgreSQL local, executado em contêiner Docker**, definido por um `compose.yaml` no repositório. O agente não se conecta a nenhum outro banco.
- O banco deve ser **reproduzível**: a partir do zero, um comando sobe o contêiner e as migrations levam o esquema ao estado atual.
- **O esquema só muda por migrations.** Nada de `CREATE`, `ALTER` ou `DROP` soltos no `psql` para mudar o esquema do projeto, e nada de `ddl-auto` do Hibernate criando ou alterando tabelas.
- O contêiner e o volume são **recursos descartáveis de desenvolvimento**, mas apagar o volume destrói os dados locais e exige confirmação.

## 2. Banco local com Docker Compose

### 2.1 Arquivo `compose.yaml`

Mantenha na raiz do repositório e use a imagem oficial **`postgres:18.6`**, com essa tag exata, nunca `latest`. O PostgreSQL 18.6 é a versão do projeto, inclusive nos testes com Testcontainers.

O nome do banco (`POSTGRES_DB`) é **o nome da pasta raiz do projeto, com hífens trocados por sublinhados**. Exemplo: na pasta `projeto-1`, o banco se chama `projeto_1`. Use esse mesmo nome no `.env.example` e no `DB_URL` (`jdbc:postgresql://localhost:<porta>/projeto_1`). Não use outro nome nem o banco padrão `postgres`.

```yaml
services:
  db:
    image: postgres:18.6            # tag exata do projeto, nunca latest
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "127.0.0.1:${DB_PORT:-5432}:5432"   # somente na interface local
    volumes:
      - pgdata:/var/lib/postgresql          # PostgreSQL 18 ou superior
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  pgdata:
```

Atenção ao ponto de montagem do volume, que **depende da versão** da imagem:

- **PostgreSQL 18 ou superior:** monte em `/var/lib/postgresql`. Montar em `/var/lib/postgresql/data` faz o contêiner falhar ao iniciar.
- **PostgreSQL 17 ou inferior:** monte em `/var/lib/postgresql/data`. Montar o diretório pai pode fazer os dados se perderem quando o contêiner for recriado.

### 2.2 Credenciais locais

- As credenciais ficam em um arquivo **`.env`** na raiz, **fora do controle de versão** (`.env` no `.gitignore`).
- Versione um **`.env.example`** com os nomes das variáveis e valores de exemplo claramente fictícios (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DB_PORT`, `DB_URL`).
- As credenciais locais são descartáveis, mas **nunca reutilize senhas reais**, nem as de outros ambientes ou da empresa.
- A porta é publicada **somente em `127.0.0.1`**, nunca em `0.0.0.0`.

**Criação obrigatória do `.env`.** Antes de qualquer `docker compose`, o agente deve criar o `.env` a partir de `.env.example`, caso ainda não exista. Depois disso, pode ler e alterar o `.env` livremente. Valores:

- `POSTGRES_DB`: nome da pasta raiz do projeto, com `-` trocado por `_` (por exemplo, `adhoc-1` vira `adhoc_1`);
- `POSTGRES_USER`: `agente`;
- `POSTGRES_PASSWORD`: senha aleatória de 32 caracteres gerada pelo próprio agente com o módulo `secrets` do Python, gravada direto no arquivo;
- `DB_PORT`: `5432`;
- `DB_URL`: `jdbc:postgresql://localhost:5432/<POSTGRES_DB>`.

O `.env` não é versionado e as credenciais são descartáveis.

### 2.3 Regras para o Docker

- Opere apenas sobre os recursos **deste projeto** (serviços definidos no `compose.yaml`).
- **Nunca** execute `docker system prune`, `docker volume prune`, `docker rm -f` ou `docker rmi` em recursos que não sejam do projeto. Não use `--privileged`, não monte o diretório raiz ou pastas pessoais do host no contêiner e não use imagens de origem desconhecida.
- Use imagens **oficiais** com tag exata.
- Se o Docker não estiver disponível ou o contêiner não subir, **pare e informe o usuário**. Não conecte em outro banco como alternativa.

## 3. Operação do contêiner

Comandos padrão, executados na raiz do repositório:

| Objetivo | Comando |
|---|---|
| Subir o banco | `docker compose up -d db` |
| Ver o estado e a saúde | `docker compose ps` |
| Ver os logs | `docker compose logs --tail=100 db` |
| Parar mantendo os dados | `docker compose stop db` ou `docker compose down` |
| Apagar contêiner **e dados** | `docker compose down -v` (exige confirmação) |

- **Antes de qualquer operação no banco**, confirme que o contêiner está em execução e saudável (`docker compose ps`). Se não estiver, suba-o e aguarde ficar saudável.
- Para executar o `psql` **dentro do contêiner**, que não exige `psql` instalado no host nem senha (no contêiner oficial, a conexão pelo socket local não pede senha):

```bash
docker compose exec -T db sh -c \
  'psql -X -v ON_ERROR_STOP=1 -P pager=off -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "\\conninfo"'
```

- Também é possível usar o `psql` do host com `-h 127.0.0.1 -p <porta>`. Nesse caso a senha vem do ambiente ou do `~/.pgpass`, **nunca da linha de comando**.
- Todas as regras de uso do `psql` (somente leitura por padrão, confirmação para alterações, proteção de dados) continuam valendo e estão na seção anterior.

## 4. Reinicialização do banco local

- O banco pode ser recriado do zero quando o esquema local divergir das migrations ou quando for necessário um banco limpo, por exemplo, antes de uma suíte de testes de API que exige estado limpo.
- Como `docker compose down -v` **apaga todos os dados locais**, o agente deve:
  1. avisar o usuário do impacto e pedir **confirmação explícita**;
  2. confirmar que o alvo é o banco local do projeto (`docker compose ps`);
  3. executar `docker compose down -v`, depois `docker compose up -d db` e aguardar o estado saudável;
  4. subir a aplicação (ou rodar o comando de migration) para reaplicar todas as migrations e confirmar o resultado.
- Nunca recrie o banco para "esconder" um erro de migration que ainda não foi entendido.

## 5. Aplicação e configuração do datasource

- A aplicação lê os dados de conexão de **variáveis de ambiente**, sem valores fixos no código ou em arquivos versionados.

```yaml
spring:
  datasource:
    url: ${DB_URL}              # jdbc:postgresql://localhost:<porta>/<banco>
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate        # nunca create, create-drop ou update
    open-in-view: false
  flyway:
    enabled: true
```

- O `ddl-auto: validate` faz o Hibernate **conferir** se as entidades batem com o esquema criado pelas migrations, sem alterar nada. Se a validação falhar ao subir, o problema está na migration ou na entidade, e deve ser corrigido lá.
- Perfis: o perfil de desenvolvimento aponta para o banco local em Docker. Testes automatizados **não** usam esse banco.

## 6. Migrations com Flyway

### 6.1 Dependências (Spring Boot 4)

No Spring Boot 4 a configuração automática do Flyway fica em um módulo próprio. Declare o starter do Flyway e o módulo do PostgreSQL, **sem versão** (gerenciadas pelo Spring Boot):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Sem o módulo `flyway-database-postgresql`, o Flyway não reconhece o PostgreSQL. Não sobrescreva a versão do Flyway sem necessidade real e sem rodar todas as migrations em teste depois.

### 6.2 Localização e nomenclatura

- Local padrão: `src/main/resources/db/migration`.
- Nome: **`V<versão>__<descrição_em_snake_case>.sql`**, com **dois** sublinhados após a versão. Exemplos: `V1__criar_tabela_usuario.sql`, `V2__adicionar_indice_email_usuario.sql`.
- Use versões inteiras e sequenciais (`V1`, `V2`, `V3`). Antes de criar uma nova, **liste as existentes** e use o próximo número. Nunca crie uma migration com versão **menor** que a última já aplicada.
- Migrations repetíveis (`R__descricao.sql`) são reservadas para objetos recriáveis, como views e funções. Nunca para tabelas ou dados.
- A descrição diz **o que** a migration faz, em verbos de ação: `criar_`, `adicionar_`, `remover_`, `renomear_`, `popular_`.

### 6.3 Regras de escrita

- **Uma migration, uma mudança lógica.** Prefira várias migrations pequenas a uma grande.
- **Migrations aplicadas são imutáveis.** Nunca edite uma migration que já foi aplicada em qualquer banco além do seu local, nem uma que já foi commitada ou compartilhada. O Flyway valida o *checksum* e a aplicação deixará de subir. A correção é uma **nova migration**.
- SQL puro do PostgreSQL, determinístico, sem dependência de dados externos.
- **Não use `IF NOT EXISTS` ou `IF EXISTS` para esconder divergências** de esquema. Se o objeto existe quando não deveria, investigue.
- Escreva o esquema explicitamente (`public` ou o esquema definido pelo projeto) e identificadores em `snake_case`, sem aspas.
- **Nomeie as restrições** explicitamente, para facilitar migrations futuras: `pk_<tabela>`, `fk_<tabela>_<referencia>`, `uq_<tabela>_<colunas>`, `ck_<tabela>_<regra>`, `ix_<tabela>_<colunas>`.
- Tipos recomendados: chave primária `bigint generated always as identity` (ou `uuid`, se o projeto optar), instantes em `timestamptz`, valores monetários e exatos em `numeric(p, s)`, textos em `text` (ou `varchar(n)` quando houver limite de negócio), booleanos em `boolean`.
- Colunas **`NOT NULL` por padrão**. Defina `DEFAULT` quando fizer sentido. Crie índices para chaves estrangeiras e colunas usadas em filtros e junções.
- A integridade fica **no banco** (`NOT NULL`, `UNIQUE`, `CHECK`, chaves estrangeiras), não só na aplicação.
- Em PostgreSQL, o DDL é transacional e o Flyway executa cada migration em uma transação por padrão: se uma falhar, nada é aplicado. Comandos que não podem rodar em transação, como `CREATE INDEX CONCURRENTLY`, exigem configuração específica do Flyway. Consulte a documentação antes de usá-los.

### 6.4 Mudanças seguras (evitar quebrar dados)

- Para **adicionar coluna obrigatória** em tabela com dados: (1) adicione como `NULL`; (2) preencha os valores existentes; (3) em outra migration, aplique `NOT NULL`.
- Para **renomear ou trocar o tipo** de coluna, faça em etapas: crie a nova coluna, copie os dados, ajuste a aplicação e só depois remova a antiga.
- Migrations **destrutivas** (`DROP TABLE`, `DROP COLUMN`, `TRUNCATE`, `DELETE` em massa, `DROP ... CASCADE`) exigem **confirmação explícita** do usuário. Antes de usar `CASCADE`, liste as dependências afetadas.
- Nunca desabilite *triggers* ou restrições para fazer uma migration passar.
- Migrations de estrutura **não carregam dados de negócio ou pessoais**. Dados de referência estáticos, por exemplo, uma tabela de status, são aceitáveis em migration própria.

### 6.5 Dados de desenvolvimento (seed)

- Dados fictícios para desenvolvimento ficam **fora de `db/migration`**, em um local separado, por exemplo `db/dev-seed`, carregado apenas no perfil de desenvolvimento por meio de `spring.flyway.locations`.
- Nunca use dados reais. Nunca coloque seed de desenvolvimento no caminho que roda em outros ambientes.

### 6.6 Comandos proibidos

- Não use o comando `clean` do Flyway. Ele apaga todo o esquema. Mantenha-o desabilitado, que é o padrão do Flyway. Para recriar o banco local, use o procedimento da seção 4.
- Não faça `repair` ou ajuste manual da tabela `flyway_schema_history` para "fazer a aplicação subir". Investigue a causa.
- Não aplique migrations manualmente com `psql` em lugar da aplicação, exceto em depuração explicada ao usuário. O histórico do Flyway deixaria de refletir a realidade.

## 7. Procedimento para criar ou alterar o esquema

1. **Entenda o estado atual:** liste os arquivos de `db/migration` e consulte o histórico aplicado:
   `SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;`
   Inspecione a estrutura com `\d <tabela>`.
2. **Defina a próxima versão** (último número + 1) e escreva a migration seguindo a seção 6.
3. **Ajuste o código** que depende da mudança (entidades, repositórios, DTOs), de modo que `ddl-auto: validate` continue passando.
4. **Aplique no banco local:** suba a aplicação, pois o Flyway aplica as pendentes, e confirme que a migration consta como `success = true` no histórico.
5. **Verifique o resultado** com `\d <tabela>` e, se aplicável, com consultas de leitura de amostra, sem expor dados sensíveis.
6. **Rode os testes:** `./mvnw verify`, incluindo o teste de integração que aplica todas as migrations em banco vazio.
7. **Registre a mudança:** entregue a migration e o código dependente **juntos** no mesmo conjunto de alterações.

Se a migration falhar ao aplicar, o PostgreSQL desfaz a transação. Se ela **nunca saiu do seu banco local**, não foi commitada nem compartilhada, pode ser corrigida e reaplicada. Se já foi compartilhada, crie uma nova migration de correção.

## 8. Testes e migrations

- Testes automatizados usam **banco descartável via Testcontainers**, com a **mesma versão do PostgreSQL** do projeto, e não o banco local de desenvolvimento. A integração com o Spring pode usar `@ServiceConnection`.
- Mantenha um teste de integração que **aplica todas as migrations em um banco vazio** e valida o mapeamento JPA (`ddl-auto: validate`). Ele deve falhar se uma migration estiver quebrada ou se a entidade divergir do esquema.
- Não use H2 nem outros bancos em memória para validar migrations, porque o DDL específico do PostgreSQL não seria testado.
- Cada teste cria os próprios dados fictícios e não depende de dados existentes no banco de desenvolvimento.

## 9. Checklist

- [ ] O contêiner do banco está em execução e saudável (`docker compose ps`).
- [ ] O destino é o banco local do projeto, confirmado com `\conninfo`.
- [ ] A mudança de esquema está em uma migration nova, com versão sequencial e nome descritivo, e nenhuma migration já aplicada foi editada.
- [ ] Restrições nomeadas, colunas `NOT NULL` por padrão e índices nas chaves estrangeiras.
- [ ] Mudanças destrutivas foram confirmadas pelo usuário e feitas em etapas quando há dados.
- [ ] `ddl-auto` está como `validate` e a aplicação sobe com a migration aplicada.
- [ ] O teste de migrations em banco vazio (Testcontainers) e `./mvnw verify` passam.
- [ ] Nenhum segredo foi escrito em arquivo versionado (`.env` fora do Git, `.env.example` sem valores reais).
- [ ] `docker compose down -v` só foi usado com confirmação explícita.

---

# Padrões de desenvolvimento Java e Spring Boot

Este documento define as convenções para escrever código Java legível, seguro e fácil de manter. A primeira parte traz as normas gerais de Java e a segunda traz as normas do framework Spring Boot. Em caso de conflito, a configuração existente no projeto (formatador, regras de análise estática) e qualquer contrato de API formalmente definido, por exemplo uma especificação OpenAPI ou uma suíte de testes de contrato, prevalecem sobre este documento.

# Parte 1 — Java

## 1. Princípios gerais

- Priorize **clareza** sobre esperteza. Código é lido muito mais vezes do que escrito.
- Prefira a solução **mais simples** que atenda ao requisito. Não crie abstração, interface ou configuração para um uso hipotético.
- Prefira **imutabilidade**: campos `final`, objetos que não mudam depois de criados, coleções imutáveis.
- Falhe **cedo e de forma explícita**: valide entradas nas fronteiras e lance exceções com mensagem útil.
- Mantenha classes e métodos **pequenos e coesos**: uma responsabilidade por classe, um nível de abstração por método.
- Não duplique lógica de negócio. Extraia quando a repetição representar a mesma regra, não apenas código parecido.

## 2. Organização do projeto

- Siga o layout padrão do Maven: `src/main/java`, `src/main/resources`, `src/test/java`, `src/test/resources`.
- Organize pacotes **por funcionalidade**, por exemplo `pedido` e `cliente`, em vez de por tipo técnico, como `controllers` e `services`, sempre que o projeto crescer além do trivial.
- Dentro de cada funcionalidade, separe com clareza o que é domínio, o que é acesso a dados e o que é entrada/saída (API).
- Evite **dependências cíclicas** entre pacotes. O domínio não deve depender de detalhes de infraestrutura.
- Uma classe, interface, record ou enum **pública por arquivo**, com o nome do arquivo igual ao do tipo.

## 3. Nomenclatura

| Elemento | Convenção | Exemplo |
|---|---|---|
| Pacote | minúsculas, sem underscore | `br.com.empresa.pedido` |
| Classe, record, enum, interface | `PascalCase`, substantivo | `PedidoService`, `StatusPedido` |
| Método | `camelCase`, começa com verbo | `calcularTotal`, `buscarPorId` |
| Variável e parâmetro | `camelCase`, nome descritivo | `valorTotal` |
| Constante (`static final`) | `MAIUSCULAS_COM_UNDERSCORE` | `LIMITE_MAXIMO` |
| Booleano | prefixo `is`, `has`, `can`, `should` | `isAtivo`, `hasPermissao` |
| Parâmetro de tipo (generics) | uma letra maiúscula ou nome curto | `T`, `K`, `V` |

Regras adicionais:

- Use nomes que revelem a **intenção**. Evite abreviações obscuras (`tmp`, `obj`, `dt`) e nomes genéricos (`Manager`, `Helper`, `Util`) quando houver um nome mais preciso.
- Não use prefixo `I` em interfaces nem sufixo `Impl` por reflexo. Dê à implementação um nome que diga o que a diferencia (`PedidoRepositoryJdbc`).
- Escreva código e identificadores de forma consistente no mesmo idioma ao longo do projeto. Termos de domínio podem ficar em português; termos técnicos (`Service`, `Repository`) seguem o padrão da indústria.

## 4. Estilo e formatação

- Use o **formatador configurado no projeto**, por exemplo Spotless com google-java-format. Se não houver, adote 4 espaços de indentação, codificação UTF-8 e linhas de até 120 colunas.
- Nunca use tabulação para indentar.
- Use **chaves** em todos os blocos `if`, `else`, `for` e `while`, mesmo com uma única instrução.
- **Imports explícitos**, sem curinga (`import java.util.*`). Remova imports não usados.
- Ordem dos membros em uma classe: constantes, campos, construtores, métodos públicos, métodos privados.
- Uma declaração de variável por linha. Declare variáveis o mais perto possível do primeiro uso e no menor escopo.
- Evite números e textos "mágicos" no meio do código: extraia para constantes nomeadas ou enums.

## 5. Recursos modernos da linguagem

Use os recursos da versão do Java configurada no projeto, de forma deliberada:

- **Records** para DTOs, objetos de valor e resultados imutáveis. Valide invariantes no construtor compacto.
- **Interfaces `sealed`** para hierarquias fechadas, combinadas com `switch` com *pattern matching* para tratamento exaustivo.
- **`switch` como expressão** em vez de cadeias de `if/else` sobre enums e tipos.
- **Pattern matching para `instanceof`**, evitando casts manuais.
- **Blocos de texto** (`"""`) para JSON, SQL e textos de várias linhas.
- **`var`** apenas quando o tipo for óbvio pela expressão do lado direito. Nunca em casos em que esconda o tipo e prejudique a leitura.
- **`Optional`** somente como **tipo de retorno** de métodos que podem não ter resultado. Não use em campos, parâmetros ou coleções, e nunca chame `get()` sem verificar a presença. Prefira `orElseThrow`, `map` e `ifPresentOrElse`.
- **Streams** para transformações simples e declarativas. Use `for` quando o laço for mais legível, tiver efeitos colaterais ou precisar de controle de fluxo (`break`, `continue`). Evite streams aninhados e lambdas longas, extraindo-as para métodos com nome.
- **Recursos em modo preview**, que exigem `--enable-preview`, não devem ser usados sem decisão explícita da equipe.

## 6. Design orientado a objetos

- **Composição sobre herança.** Use herança só para relações "é um" verdadeiras e estáveis.
- Crie **interfaces** para contratos com mais de uma implementação real ou para fronteiras que precisam ser substituídas em testes. Não crie uma interface para cada classe.
- **Injeção de dependências por construtor**, com campos `final`. Evite injeção por campo e dependências obtidas por métodos estáticos.
- **Encapsule o estado.** Campos privados; exponha comportamento, com métodos que tenham significado de negócio, em vez de getters e setters para tudo.
- Evite **`null`**: retorne coleções vazias em vez de `null`, use `Optional` em retornos opcionais e valide parâmetros obrigatórios com `Objects.requireNonNull`.
- Evite classes utilitárias com estado e métodos estáticos que dependam de estado global.
- Implemente `equals` e `hashCode` **juntos** e de forma consistente. Records já fazem isso. Para ordenação, use `Comparator` em vez de implementar `Comparable` sem necessidade.
- Prefira **enums** a constantes inteiras ou textuais para conjuntos fechados de valores.
- Sinais de alerta: classes com muitas responsabilidades ("classe deus"), métodos com muitos parâmetros, condicionais repetidas sobre o mesmo tipo. Agrupe parâmetros em um record ou considere polimorfismo ou `sealed` + `switch`.

## 7. Tratamento de erros

- Use **exceções**, não códigos de retorno, para condições excepcionais. Não use exceções para controle de fluxo normal.
- Crie uma **hierarquia de exceções de domínio**, normalmente não verificadas, com nomes que expliquem o erro (`PedidoNaoEncontradoException`). Use exceções verificadas apenas quando o chamador puder realmente se recuperar.
- **Nunca engula exceções** (`catch` vazio). Se capturar, trate, registre com contexto ou relance.
- Preserve a **causa original** ao relançar: `throw new X("mensagem com contexto", e)`.
- Não capture `Exception` ou `Throwable` genéricos, exceto em fronteiras bem definidas, como ponto de entrada da aplicação ou manipulador global de erros.
- Use **try-with-resources** para qualquer recurso que implemente `AutoCloseable`.
- Mensagens de erro devem dizer **o que falhou e com qual dado**, sem expor informação sensível.
- Converta exceções internas em respostas apropriadas **em um único ponto** na borda do sistema, não espalhado pelo código.
- Valide entradas nas fronteiras (API, mensageria, arquivos) e confie nos dados já validados dentro do domínio.

## 8. Coleções, tipos e APIs da biblioteca padrão

- Declare variáveis, parâmetros e retornos pelas **interfaces** (`List`, `Set`, `Map`), não pelas implementações.
- Não exponha coleções mutáveis internas. Retorne cópias ou visões imutáveis (`List.copyOf`, `Collections.unmodifiableList`). Para criar coleções fixas, use `List.of`, `Set.of` e `Map.of`.
- **Não use tipos brutos** (`List` sem parâmetro). Use generics corretamente.
- **Datas e horas**: use `java.time` (`LocalDate`, `LocalDateTime`, `Instant`, `ZonedDateTime`). Nunca `Date` ou `Calendar`. Armazene instantes em UTC e converta para fuso apenas na apresentação. Injete um `Clock` para tornar o tempo testável.
- **Valores monetários e cálculos exatos**: use `BigDecimal`, criado a partir de `String` ou `BigDecimal.valueOf`, nunca de `double`. Defina escala e `RoundingMode` explicitamente e compare com `compareTo`, não com `equals`.
- Compare textos e objetos com `equals`, nunca com `==`. Para comparar com literal, prefira `"literal".equals(valor)` ou `Objects.equals`.
- Em laços que concatenam muitos textos, use `StringBuilder`. Fora disso, a concatenação simples é aceitável.
- Evite reflexão e APIs internas do JDK, a menos que seja indispensável e isolado.

## 9. Concorrência

- Prefira **estado imutável** e estruturas de `java.util.concurrent` a bloqueios manuais.
- Não crie `Thread` diretamente. Use `ExecutorService` gerenciado e encerre-o corretamente.
- Para tarefas com muito I/O bloqueante, considere **threads virtuais** (Java 21 ou superior).
- Minimize o estado mutável compartilhado. Quando for inevitável, use `synchronized` ou `Lock` em seções críticas pequenas, ou variáveis atômicas.
- Em `CompletableFuture`, trate erros explicitamente (`exceptionally`, `handle`) e defina **timeouts**.
- Use `ThreadLocal` com cautela e sempre limpe o valor ao final.
- Documente a **segurança de threads** de classes compartilhadas, indicando se são thread-safe ou não.

## 10. Logging

- Use uma fachada de logging (**SLF4J**). Nunca `System.out.println` ou `e.printStackTrace()` em código de produção.
- Use **mensagens parametrizadas**: `log.info("Pedido {} criado", id)`, sem concatenar textos.
- Escolha o nível adequado: `ERROR` para falhas que exigem atenção, `WARN` para situações anormais recuperáveis, `INFO` para eventos relevantes do negócio, `DEBUG` e `TRACE` para diagnóstico.
- **Nunca registre dados sensíveis**: senhas, tokens, chaves, números de documentos ou dados pessoais.
- Não registre e relance a mesma exceção em várias camadas, pois isso gera logs duplicados. Registre uma vez, onde ela for tratada.
- Ao registrar uma exceção, passe-a como último argumento para que o *stack trace* seja preservado.

## 11. Testes

- Use **JUnit 5**. Bibliotecas de asserção fluentes (AssertJ) e **Mockito** são bem-vindas, com moderação.
- Estrutura de cada teste: **organizar, agir, verificar** (*Arrange, Act, Assert* ou *Given, When, Then*), com uma ideia por teste.
- Nomeie os testes de forma descritiva, dizendo o cenário e o resultado esperado (`deveLancarExcecaoQuandoPedidoNaoExiste`).
- Testes unitários devem ser **rápidos, isolados e determinísticos**: sem rede, sem banco real, sem depender de ordem de execução, de horário ou de dados aleatórios sem semente.
- Testes de integração ficam separados dos unitários e usam dependências reais. Para bancos de dados, prefira **Testcontainers** com o mesmo banco usado em produção em vez de bancos em memória diferentes.
- Convenção de nomes: `*Test` para unitários, executados pelo Surefire, e `*IT` para integração, executados pelo Failsafe.
- Não coloque lógica condicional ou laços dentro dos testes.
- Não teste detalhes de implementação. Teste o **comportamento observável**.
- Cobertura é um indicador, não uma meta. Priorize regras de negócio, casos de borda e caminhos de erro.
- Todo defeito corrigido deve ganhar um teste que o reproduza.

## 12. Build com Maven

- Mantenha o **Maven Wrapper** (`mvnw`) no repositório para builds reproduzíveis.
- Defina a versão do Java em uma propriedade (`maven.compiler.release`) e as versões de dependências e plugins em `<properties>` ou `dependencyManagement`. Não deixe versões soltas e repetidas pelo `pom.xml`.
- **Fixe versões** de plugins e dependências. Evite `LATEST`, `RELEASE` e intervalos de versão.
- Mantenha as dependências no **mínimo necessário** e remova as não utilizadas. Dependências apenas de teste usam escopo `test`.
- Separe testes unitários (Surefire) e de integração (Failsafe).
- O comando padrão de validação é `mvn verify` (ou `./mvnw verify`): compila, roda os testes e executa as verificações configuradas.
- Não versione a pasta `target/`, arquivos de IDE nem credenciais. Mantenha um `.gitignore` adequado.

## 13. Documentação e comentários

- Escreva **Javadoc** em APIs públicas e em classes de domínio importantes, explicando o contrato (o que faz, parâmetros e exceções), não repetindo o nome do método.
- Comentários explicam o **porquê**, não o quê. Se o código precisar de um comentário para ser entendido, considere renomear ou extrair um método.
- Não deixe **código comentado**. O controle de versão guarda o histórico.
- `TODO` e `FIXME` devem ter contexto suficiente para serem acionáveis.
- Mantenha um README atualizado com como construir, testar e executar o projeto.

## 14. Segurança básica

- **Valide e normalize** toda entrada externa.
- Use consultas **parametrizadas** (`PreparedStatement` ou equivalente). Nunca monte SQL concatenando valores.
- Nunca escreva **segredos** (senhas, tokens, chaves) no código ou em arquivos versionados. Use variáveis de ambiente ou um cofre de segredos.
- Evite desserialização de dados não confiáveis (`ObjectInputStream`).
- Armazene senhas somente com algoritmos de *hash* apropriados para senhas, com sal. Nunca em texto puro.
- Mantenha as dependências atualizadas e atente a vulnerabilidades conhecidas.
- Não exponha detalhes internos (stack traces, nomes de tabelas) em mensagens enviadas a clientes.

## 15. Checklist antes de concluir uma tarefa

- [ ] O código compila sem novos avisos.
- [ ] `mvn verify` passa, incluindo os testes novos e os existentes.
- [ ] O comportamento novo ou alterado está coberto por testes, incluindo casos de erro e de borda.
- [ ] A formatação do projeto foi aplicada e não há imports não usados.
- [ ] Nomes de classes, métodos e variáveis revelam a intenção.
- [ ] Não há código morto, código comentado, `System.out` nem segredos no código.
- [ ] Exceções são tratadas ou propagadas com contexto, sem `catch` vazio.
- [ ] Dados sensíveis não aparecem em logs nem em mensagens de erro.
- [ ] Mudanças públicas relevantes estão documentadas (Javadoc, README).

## 16. O que evitar

- Interfaces, fábricas ou camadas sem necessidade real.
- Herança profunda e classes abstratas usadas só para reaproveitar código.
- Getters e setters gerados para todos os campos por hábito.
- Retornar `null` onde cabe coleção vazia ou `Optional`.
- Strings e números "mágicos" espalhados pelo código.
- Capturar e ignorar exceções, ou capturar `Exception` genérica fora das fronteiras.
- Métodos longos que fazem várias coisas, e classes com muitas responsabilidades.
- Estado estático mutável e *singletons* com estado.
- Testes que dependem de ordem, de horário ou de serviços externos.

---

# Parte 2 — Spring Boot

As normas abaixo valem para aplicações Spring Boot 4.x com Maven e PostgreSQL. A versão exata do Spring Boot é a fixada no `pom.xml` do projeto. Se algo aqui conflitar com o contrato de API do projeto, como OpenAPI ou testes de contrato, o contrato prevalece.

## 17. Versões e dependências

- **Não presuma as APIs do Spring Boot 3.x.** O Spring Boot 4 mudou de forma relevante: modularização dos starters, Spring Framework 7, Jakarta EE 11 (Servlet 6.1) e Jackson 3. Em caso de dúvida sobre uma API, anotação ou pacote, consulte a documentação da versão fixada no projeto antes de escrever o código.
- Herde do `spring-boot-starter-parent` ou importe o BOM do Spring Boot e **não declare versão** em dependências gerenciadas por ele.
- Use **starters específicos de tecnologia**, por exemplo `spring-boot-starter-webmvc` no lugar do antigo `spring-boot-starter-web`, e os starters de teste correspondentes. Evite os starters "clássicos", que existem para migração rápida e estão marcados como obsoletos.
- Use sempre o namespace **`jakarta.*`**, nunca `javax.*`.
- **Jackson 3** é a biblioteca de JSON padrão: os pacotes de mapeamento passaram de `com.fasterxml.jackson` para `tools.jackson`, e a configuração de mappers mudou, por exemplo, `JsonMapperBuilderCustomizer`. Não misture as duas gerações.
- Prefira `RestClient` a `RestTemplate` para chamadas HTTP síncronas. O `RestTemplate` está em caminho de descontinuação nas versões 4.2 em pré-lançamento.
- O servidor embutido padrão é o **Tomcat**. O Undertow foi removido.
- Não use APIs marcadas como `@Deprecated`. Em versões novas elas costumam ser removidas na versão seguinte.

## 18. Estrutura da aplicação

- A classe com `@SpringBootApplication` fica no **pacote raiz**, para que a varredura de componentes cubra os subpacotes sem configuração extra.
- Organize por funcionalidade e, dentro de cada uma, siga o fluxo **controller → service → repository**.
- O **controller** só trata a entrada e a saída HTTP: recebe, valida, chama o serviço e monta a resposta. Não contém regra de negócio nem acessa o repository diretamente.
- O **service** concentra as regras de negócio e as fronteiras de transação.
- O **repository** só faz acesso a dados.
- **Nunca exponha entidades JPA na API.** Use DTOs, preferencialmente records, de entrada e saída, com mapeamento explícito entre eles e as entidades.

## 19. Injeção de dependências e beans

- **Injeção por construtor**, com campos `final`. Quando há um único construtor, o `@Autowired` é desnecessário. Não use injeção por campo.
- Use os estereótipos adequados: `@Service`, `@Repository`, `@RestController` e `@Component`. Use `@Configuration` com métodos `@Bean` para configurar objetos de infraestrutura e de bibliotecas de terceiros.
- Beans são **singletons sem estado mutável** por padrão. Não guarde dados de requisição em campos de beans.
- Evite lógica pesada na inicialização da aplicação e dependências circulares entre beans.
- Evite varreduras de componentes amplas ou customizadas sem necessidade.

## 20. Configuração e profiles

- Use `application.yml` ou `application.properties`, de forma consistente em todo o projeto, para a configuração padrão e **profiles** para variações (`dev`, `test`, `prod`).
- Para propriedades próprias, use **`@ConfigurationProperties`** com `record` e `@Validated`, em vez de `@Value` espalhado pelo código.
- **Segredos e credenciais** (senha do banco, chaves de JWT) vêm de variáveis de ambiente ou de um cofre, nunca de arquivos versionados.
- Não coloque valores específicos de ambiente (URLs, hosts, portas) no código.
- Defina `spring.jpa.open-in-view=false` para evitar acesso lento ao banco durante a renderização da resposta.
- Configure o encerramento gracioso (`server.shutdown=graceful`).

## 21. API REST (Spring MVC)

- Use `@RestController` e mapeamentos específicos (`@GetMapping`, `@PostMapping` e demais). Recursos no **plural e como substantivos** (`/pedidos`, `/pedidos/{id}`).
- Use os **verbos e códigos de status corretos**: `200` para leitura e atualização bem-sucedidas, `201` com cabeçalho `Location` na criação, `204` sem corpo, `400` para requisição malformada, `401` e `403` para autenticação e autorização, `404` para recurso inexistente, `409` para conflito e `422` para erro de validação de regras, quando o contrato do projeto assim definir.
- **DTOs como records**, separados para entrada e saída. Valide a entrada com **Jakarta Bean Validation** (`@Valid`, `@NotBlank`, `@Size`, `@Email` e demais) na borda.
- Trate erros em **um único ponto** com `@RestControllerAdvice`, convertendo exceções de domínio e de validação em respostas consistentes. Quando o contrato do projeto não fixar o formato de erro, use `ProblemDetail`, padrão RFC 9457.
- Nunca retorne stack traces ou mensagens internas ao cliente.
- Implemente **paginação** em listagens que podem crescer (`Pageable`, ou parâmetros de limite e deslocamento definidos no contrato), com um limite máximo.
- Mantenha os controllers finos e **não anote controllers com `@Transactional`**.

## 22. Persistência (Spring Data JPA e PostgreSQL)

- Repositories são **interfaces** que estendem `JpaRepository` ou uma interface mais restrita. Use consultas derivadas para casos simples e `@Query`, JPQL ou SQL nativo parametrizado, para consultas complexas. Nunca monte consultas concatenando textos.
- **Entidades JPA não são records**: exigem construtor sem argumentos e campos mutáveis. Mantenha-as no pacote de persistência e fora da API.
- Defina `equals` e `hashCode` de entidades com cuidado, por identificador de negócio ou pelo id, considerando entidades ainda não persistidas.
- Relacionamentos **`LAZY` por padrão**. Evite o problema N+1 usando `join fetch` ou `@EntityGraph` nas consultas que precisam dos dados relacionados, e verifique as consultas geradas em caso de lentidão.
- **O esquema do banco é gerenciado por migrations versionadas** (Flyway ou Liquibase), nunca por `spring.jpa.hibernate.ddl-auto=create` ou `update` fora do desenvolvimento local. Em produção use `validate` ou `none`.
- Garanta a integridade **no banco**, não só na aplicação: `NOT NULL`, `UNIQUE`, chaves estrangeiras e índices para as colunas usadas em filtros e junções.
- Mapeie tipos com atenção: `numeric` para `BigDecimal`, `timestamptz` para `Instant` ou `OffsetDateTime`, `uuid` ou identidade (`bigint`) para chaves primárias.
- Use paginação no banco (`Pageable`), nunca carregue tabelas inteiras na memória para paginar ou filtrar.

## 23. Transações

- Declare **`@Transactional` na camada de serviço**, nos métodos que precisam de atomicidade. Não use em controllers nem em repositories.
- Use `@Transactional(readOnly = true)` em operações apenas de leitura.
- Mantenha transações **curtas** e sem chamadas a serviços externos, filas ou operações lentas dentro delas.
- Atenção à **autoinvocação**: um método `@Transactional` chamado de outro método da mesma classe não passa pelo proxy e não abre transação.
- Por padrão o *rollback* ocorre só para exceções não verificadas. Se precisar reverter por exceções verificadas, declare `rollbackFor`.

## 24. Segurança (Spring Security)

- Configure a segurança com um bean **`SecurityFilterChain`**, de forma explícita: defina quais rotas são públicas e exija autenticação nas demais por padrão.
- Armazene senhas somente com o `PasswordEncoder` do Spring, por exemplo BCrypt ou Argon2. Nunca em texto puro.
- Para JWT, **valide assinatura, expiração e emissor** e prefira o suporte oficial do Spring Security, resource server, a implementações manuais. Mantenha a chave de assinatura fora do código.
- Desabilite CSRF apenas em APIs sem estado que usam tokens, e configure **CORS** de forma explícita, com origens permitidas conhecidas.
- Use autorização por método (`@PreAuthorize`) quando as regras dependerem do usuário ou do recurso.
- Nunca registre tokens, senhas ou dados pessoais em logs.
- Valide a propriedade dos recursos: confirme que o usuário autenticado pode acessar ou alterar o recurso solicitado, e não apenas que está autenticado.

## 25. Observabilidade e operação

- Use o **Actuator** com o mínimo de endpoints expostos (`health`, `info`) e proteja os demais.
- Configure verificações de saúde que incluam o banco de dados.
- Mantenha logs com contexto útil (identificador da requisição ou do recurso), sem dados sensíveis, e use os níveis de log conforme a seção 10.
- Empacote como JAR executável e permita configuração por variáveis de ambiente, para que a mesma imagem rode em ambientes diferentes.

## 26. Testes no Spring Boot

- Teste a **regra de negócio com testes unitários puros**, sem subir o contexto do Spring. Eles são rápidos e devem ser a maioria.
- Use **testes de fatia** para camadas específicas: `@WebMvcTest` para controllers, com `MockMvc`, e `@DataJpaTest` para repositories. Os pacotes dessas anotações mudaram com a modularização do Spring Boot 4, então confira a documentação da versão fixada.
- Use `@SpringBootTest` apenas para testes de integração ponta a ponta, que são mais lentos e devem ser poucos.
- Para dependências simuladas em testes de fatia, use **`@MockitoBean`** e `@MockitoSpyBean`. As anotações `@MockBean` e `@SpyBean` foram removidas.
- Em testes de integração, use **Testcontainers com PostgreSQL**, a mesma família de banco usada em produção, integrado ao Spring com `@ServiceConnection`, em vez de um banco em memória diferente.
- Garanta o **isolamento entre testes**: reverta a transação do teste ou limpe os dados explicitamente. Nenhum teste deve depender da ordem de execução.
- Aplique as migrations do banco nos testes de integração, para validar também o esquema.
- Valide o contrato da API, incluindo status, formato do JSON e mensagens de erro, em testes de controller ou de integração.

## 27. Checklist adicional para Spring Boot

- [ ] Controllers não contêm regra de negócio e não retornam entidades JPA.
- [ ] A entrada externa é validada com Bean Validation e os erros passam pelo tratamento global.
- [ ] Os status HTTP e o formato das respostas seguem o contrato do projeto.
- [ ] Injeção é por construtor e não há `@Autowired` em campos.
- [ ] `@Transactional` está nos serviços, com `readOnly` nas leituras, e não há chamadas externas dentro das transações.
- [ ] Mudanças no esquema do banco estão em migrations versionadas.
- [ ] Não há segredos em arquivos versionados e a configuração sensível vem do ambiente.
- [ ] Consultas novas foram verificadas contra o problema N+1 e têm os índices necessários.
- [ ] Testes de fatia e de integração cobrem os endpoints novos e usam `@MockitoBean`, não `@MockBean`.
- [ ] Nenhuma API obsoleta ou do Spring Boot 3.x foi usada sem conferência na documentação da versão do projeto.
