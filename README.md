# Experimento do TCC: Spec-Driven Development vs. prompts livres

Este repositório reúne o material do experimento do Trabalho de Conclusão de Curso **"Spec-Driven Development no desenvolvimento assistido por IA: um estudo sobre consumo de tokens, custos e qualidade de artefatos de software"** (Graduação em Sistemas de Informação, Universidade Vale do Rio Doce).

O experimento compara duas formas de usar um agente de codificação para construir o mesmo backend:

- **Grupo ad-hoc:** um prompt livre com a descrição do sistema, seguido de prompts de correção.
- **Grupo SDD:** o fluxo do [GitHub Spec Kit](https://github.github.com/spec-kit/index.html) (especificação, plano e tarefas antes de qualquer código).

Os dois grupos usam o mesmo agente, o mesmo modelo, a mesma stack e a mesma suíte de testes de contrato, que fica oculta ao agente. O que muda é a presença das especificações estruturadas.

> Este README descreve o método. Ele não traz resultados. Os dados brutos de cada execução ficam em `resultados/` e a consolidação, em `metricas-execucoes.xlsx`.

## Pergunta de pesquisa

De que forma o uso de especificações estruturadas (SDD), em contraste com prompts livres, afeta o **consumo de tokens**, o **custo estimado**, o **volume de retrabalho** e a **qualidade** (aderência ao contrato de API) de artefatos de software gerados por IA?

## Stack

| Camada | Tecnologia | Versão |
|---|---|---|
| Linguagem | Java | 25 (JDK 25.0.5) |
| Framework | Spring Boot | 4.1.2 |
| Build | Apache Maven (Maven Wrapper) | 3.10.0 |
| Banco de dados | PostgreSQL, em Docker local | `postgres:18.6` |
| Migrations | Flyway | gerenciada pelo Spring Boot |
| Testes do projeto | Testcontainers | gerenciada pelo Spring Boot |
| Agente de codificação | Claude Code (CLI) | 2.1.316 |
| Modelo | Claude Opus 5.5 | `claude-opus-5-5`, esforço (*effort*) médio, sem seleção automática de modelo |
| Framework SDD | GitHub Spec Kit | 1.2.5 |
| Oráculo de testes | Hurl | 8.0.1 |
| Medição de uso | agentsview | 0.46.1 |
| Automação | Python 3 (biblioteca padrão, mais `openpyxl` para a planilha) | |

## Metodologia

### Desenho experimental

Estudo comparativo direto entre dois cenários de desenvolvimento assistido por IA, com réplicas:

| | Cenário A: ad-hoc | Cenário B: SDD |
|---|---|---|
| Entrada | Um prompt livre com a descrição funcional do sistema (`prompts-adhoc.md`) | O mesmo texto como descrição base (`descricao-base.md`), entregue ao fluxo do Spec Kit |
| Geração do código | Sessão única de geração | Sessão de especificação (`/speckit-specify`, `/speckit-plan`, `/speckit-tasks`) e, depois, sessão de implementação (`/speckit-implement`) |
| Artefatos | Nenhum documento formal | Artefatos do Spec Kit, com requisitos em notação EARS |
| Correções | Sessão separada, alimentada pelo resultado da suíte | Idem |

A etapa `/speckit-clarify` não é executada, porque é interativa. Os tokens da fase de especificação do SDD são contabilizados integralmente no custo do grupo.

Cada grupo tem **5 réplicas** (`adhoc-2` a `adhoc-6` e `sdd-2` a `sdd-6`), executadas de forma alternada entre os grupos. As execuções `adhoc-1` e `sdd-1` foram pilotos manuais (modo interativo), mantidos separados das demais.

### Objeto de estudo: recorte A do RealWorld (Conduit)

O benchmark é o backend do [RealWorld](https://github.com/realworld-apps/realworld), uma API de rede social no estilo do Medium. O experimento usa o **recorte A**, com 7 das 19 operações da API:

| Método | Rota | Autenticação |
|---|---|---|
| `POST` | `/api/users/login` | não |
| `POST` | `/api/users` | não |
| `GET` | `/api/user` | obrigatória |
| `PUT` | `/api/user` | obrigatória |
| `GET` | `/api/profiles/{username}` | opcional |
| `POST` | `/api/profiles/{username}/follow` | obrigatória |
| `DELETE` | `/api/profiles/{username}/follow` | obrigatória |

A origem, as adaptações feitas no contrato e o texto entregue aos agentes estão em [`realworld-recorte-a.md`](realworld-recorte-a.md). Commit do RealWorld fixado: `c5aecdf9a2e0cf68e8420a7cae55db8d06d45f87`.

### Controle de variáveis

- **Ponto de partida idêntico:** cada execução nasce de um *template* (`execucoes/adhoc-template` e `execucoes/sdd-template`) com o esqueleto Spring Boot, o `compose.yaml`, o Maven Wrapper e os mesmos *steering files* (`CLAUDE.md` e `.claude/rules/`). Esses arquivos não contêm requisitos funcionais; os requisitos estão só no prompt ou na descrição base.
- **Mesmo contrato nos dois grupos:** o texto do recorte A entregue ao agente é idêntico no `prompts-adhoc.md` e no `descricao-base.md`.
- **Isolamento:** cada execução roda em sua própria pasta. Regras de permissão do Claude Code (`.claude/settings.json`, regeneradas pelo orquestrador) negam leitura e escrita nas outras execuções, nos templates, nos documentos da pesquisa e na suíte Hurl. As pastas ad-hoc também negam o Spec Kit.
- **Banco:** o banco de cada execução tem o nome da pasta (com `_` no lugar de `-`) e sobe em Docker. A cada rodada de testes ele é recriado do zero (`docker compose down -v`).
- **Modelo fixo:** `claude-opus-5-5`, esforço médio, modo de permissão `acceptEdits`, com o mesmo conjunto de ferramentas permitidas nos dois grupos.

### Protocolo de cada execução

1. **Inicialização:** a pasta é criada a partir do template do grupo, com um repositório Git local e um commit "estado inicial". (Esses repositórios locais são removidos antes da publicação.)
2. **Geração inicial:** ad-hoc envia o prompt único; SDD roda a especificação e depois a implementação.
3. **Avaliação inicial (oráculo oculto):** a aplicação sobe com banco limpo na porta 8081 e a suíte Hurl (4 arquivos, 53 requisições) é executada com `--continue-on-error`. O agente não tem acesso aos arquivos da suíte.
4. **Ciclos de correção:** se houver requisições reprovadas, o orquestrador envia ao agente apenas os blocos de erro do Hurl (arquivo e linha, requisição, valor esperado e valor recebido) e repete, até a suíte passar integralmente ou o teto de **3 ciclos** de correção. Os ciclos são automáticos, sem intervenção humana.
5. **Coleta:** ao final, as sessões são rotuladas por fase no agentsview e as métricas são gravadas na planilha.

Se o limite de uso do Claude Code estoura no meio de uma fase, o orquestrador espera o horário de reset, desfaz o código da fase interrompida com `git reset` ao checkpoint anterior e a recomeça do zero. As sessões descartadas ficam registradas em `resultados/<execução>/estado.json` e os logs parciais, em `resultados/<execução>/interrompidas/`.

### Métricas coletadas

| Eixo | Métricas | Instrumento |
|---|---|---|
| Consumo e custo | Tokens de entrada, saída, leitura e escrita de cache, por fase (especificação, geração, correção); custo estimado em US$ | agentsview, conferido com os logs de sessão do Claude Code |
| Tempo | Tempo ativo por fase (soma dos intervalos entre mensagens até um limiar de 30 min) | agentsview |
| Qualidade | Requisições aprovadas na rodada inicial e na final (de 53); falhas iniciais por categoria | Suíte Hurl |
| Retrabalho | Ciclos de correção; linhas alteradas nas correções (`git diff`, resumo em `resultados/<execução>/git-resumo.txt`) | Orquestrador e Git |
| Síntese | Tokens e custo até o verde (*cost-to-green*); se atingiu 100% | Planilha |

O custo é uma **estimativa com a tabela de preços da API** calculada pelo agentsview. Não corresponde à cobrança da assinatura.

Categorias de falha: autenticação; validação de dados e mensagens de erro; perfis e seguir; formato das respostas JSON; falhas em cascata (causadas por uma falha anterior na mesma sequência de requisições); outras.

### Análise

Comparação quantitativa por execução e por grupo (mediana, amplitude, variação absoluta e percentual entre SDD e ad-hoc) e análise qualitativa da categorização das falhas. A amostra por grupo é pequena, de modo que a análise é descritiva.

### Limitações conhecidas do método

- Possível contaminação da base de treino do modelo pelo projeto RealWorld.
- O resultado vale para o Claude Opus 5.5, o Claude Code e backends Java/Spring Boot.
- O contrato do recorte A tem pontos ambíguos, registrados em [`observacoes-lacunas-contrato.md`](observacoes-lacunas-contrato.md), que podem gerar falhas independentes da abordagem.
- Os pilotos `adhoc-1` e `sdd-1` foram manuais; as demais execuções rodam em modo não interativo.
- A lista de skills do Spec Kit amplia o contexto inicial do grupo SDD.
- Ambos os grupos usam a mesma configuração global do Claude Code do pesquisador.

## Estrutura do repositório

```
.
├── execucoes/
│   ├── adhoc-template/        # ponto de partida do grupo ad-hoc
│   ├── sdd-template/          # ponto de partida do grupo SDD (inclui .specify e skills speckit-*)
│   ├── adhoc/adhoc-N/         # execuções ad-hoc
│   └── sdd/sdd-N/             # execuções SDD
├── resultados/<execução>/     # estado, logs do Claude Code (.jsonl), saídas do Hurl, feedbacks de correção
├── orquestrar_execucoes.py    # roda as execuções, uma por vez, sem IA no controle do fluxo
├── coletar_metricas.py        # preenche a planilha a partir do agentsview e do Hurl
├── metricas-execucoes.xlsx    # métricas por execução e resumo por grupo
├── realworld-recorte-a.md     # origem, escopo e contrato do recorte A
└── observacoes-lacunas-contrato.md
```

## Pré-requisitos e preparação

Ferramentas no `PATH`: Python 3 (com `openpyxl`), Docker (daemon ativo), Git, JDK 25, [Hurl](https://hurl.dev) 8.0.1, agentsview e Claude Code. O Maven Wrapper vem nas pastas de execução.

A suíte de testes **não está neste repositório**, por ser o oráculo oculto aos agentes. Baixe os quatro arquivos Hurl do commit fixado para uma pasta fora do experimento:

```bash
SHA=c5aecdf9a2e0cf68e8420a7cae55db8d06d45f87
mkdir -p ~/tcc-oracle && cd ~/tcc-oracle
for f in auth errors_auth profiles errors_profiles; do
  curl -sSfL -o $f.hurl https://raw.githubusercontent.com/realworld-apps/realworld/$SHA/specs/api/hurl/$f.hurl
done
```

Os caminhos `~/Documents/tcc-wks` (repositório) e `~/tcc-oracle` (oráculo) são os usados nas regras de permissão geradas pelo orquestrador.

## Como executar

```bash
python orquestrar_execucoes.py --simular                   # mostra o plano sem alterar nada
python orquestrar_execucoes.py --so-preparar               # cria pastas, settings e git, sem executar
python orquestrar_execucoes.py --sim                       # executa tudo (horas de execução e consumo de tokens)
python orquestrar_execucoes.py --sim --apenas adhoc-6 sdd-6
```

O orquestrador pula execuções que já têm código gerado fora dele e retoma as que têm estado salvo. Principais opções: `--por-grupo`, `--max-correcoes`, `--modelo`, `--effort`, `--porta`, `--timeout-fase-min`, `--sem-planilha`.

Atenção: a execução consome tokens da assinatura do Claude Code, apaga os dados locais do banco de cada pasta (`docker compose down -v`) e encerra qualquer processo que esteja ocupando a porta 8081.

## Créditos

O contrato de API e a suíte de testes são do projeto [RealWorld](https://github.com/realworld-apps/realworld) (licença MIT, Thinkster). O fluxo SDD usa o [GitHub Spec Kit](https://github.com/github/spec-kit).
