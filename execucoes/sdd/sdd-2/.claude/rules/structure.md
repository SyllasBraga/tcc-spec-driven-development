# Estrutura do projeto

Aplicação Spring Boot de módulo único, com build Maven, API REST e banco PostgreSQL executado localmente em Docker. Novos arquivos devem seguir o layout abaixo. Não crie diretórios na raiz sem necessidade.

## Layout de diretórios

Substitua `<pacote/base>` pelo pacote raiz definido no `pom.xml` (o mesmo `groupId` e `artifactId` do projeto).

```
.
├── CLAUDE.md                          # instruções do projeto para o Claude Code
├── .claude/
│   ├── rules/                         # product.md, tech.md, structure.md (carregados automaticamente)
│   └── skills/                        # skills do projeto (<nome>/SKILL.md), se houver
├── src/
│   ├── main/
│   │   ├── java/<pacote/base>/
│   │   │   ├── Application.java       # classe @SpringBootApplication, no pacote raiz
│   │   │   ├── <funcionalidade>/      # um pacote por funcionalidade
│   │   │   │   ├── web/               # controllers e DTOs (records) de entrada e saída
│   │   │   │   ├── service/           # regras de negócio e fronteiras de transação
│   │   │   │   └── persistence/       # entidades JPA e repositories
│   │   │   └── shared/                # configuração, tratamento de erros, segurança, utilitários
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/
│   │           ├── migration/         # migrations Flyway (V<n>__descricao.sql)
│   │           └── dev-seed/          # dados fictícios, carregados só no perfil dev
│   └── test/
│       ├── java/<pacote/base>/        # espelha a estrutura de src/main/java
│       └── resources/
├── compose.yaml                       # banco PostgreSQL local em Docker
├── .env.example                       # variáveis de ambiente, sem valores reais (versionado)
├── .env                               # valores locais (NÃO versionado)
├── .gitignore
├── mvnw, mvnw.cmd, .mvn/              # Maven Wrapper
├── pom.xml
└── README.md
```

## Regras de organização

- **Um pacote por funcionalidade**, com as subdivisões `web`, `service` e `persistence`. A dependência segue o sentido `web` → `service` → `persistence`. Nunca no sentido inverso.
- O pacote `shared` guarda o que é transversal e **não depende** de pacotes de funcionalidade.
- A classe `Application` fica no pacote raiz, para a varredura de componentes cobrir todos os subpacotes.
- Os testes **espelham** a estrutura de `src/main/java`. Testes unitários terminam em `Test` e testes de integração em `IT`.
- Migrations ficam **somente** em `src/main/resources/db/migration`. Dados de desenvolvimento ficam em `db/dev-seed`, nunca em `db/migration`.
- Os arquivos do Docker (`compose.yaml`, `.env.example`) ficam na raiz do projeto.
- Não são versionados: `target/`, `.env`, arquivos de IDE e dados locais do banco.
- Nomes de pacotes em minúsculas, sem sublinhado. Nomes de classes seguem as convenções da seção "Padrões de desenvolvimento Java e Spring Boot" de `tech.md`.
