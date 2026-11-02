# tcc-wks

API REST de um blog (somente backend) em Spring Boot 4 + Maven + PostgreSQL (Docker local).

As regras do projeto ficam em `.claude/rules/` e são carregadas automaticamente pelo Claude Code:

- `.claude/rules/product.md` — escopo do produto
- `.claude/rules/structure.md` — layout de diretórios e organização de pacotes
- `.claude/rules/tech.md` — acesso ao PostgreSQL, banco local em Docker e migrations, padrões Java/Spring Boot

Em caso de conflito entre regras, vale a mais restritiva.

## Comandos principais

- Subir o banco: `docker compose up -d db`
- Validar o projeto: `./mvnw verify`

## Escopo de trabalho

Trabalhe somente dentro deste diretório. Não leia, não liste e não altere arquivos fora dele.

## Stack

A stack tecnológica do projeto backend foi padronizada em Java (versão 25 LTS ou 27), Spring Boot (série 4.1.x ou 4.2.0), gerenciador de dependências Apache Maven e banco de dados relacional PostgreSQL.

Versões fixadas neste projeto: Java 25 (`java.version` do `pom.xml`), Spring Boot 4.1.2, Maven 3.10.0 (Maven Wrapper) e PostgreSQL 18.6 (`postgres:18.6`).

## Saída de comandos

- Rode o Maven com `-q` (por exemplo, `./mvnw -q verify`).
- Mostre ao usuário só os erros e as últimas 50 linhas de log.
- Na suíte Hurl, mostre só as requisições reprovadas.
- Não cole saídas longas na conversa; resuma em uma ou duas frases.
