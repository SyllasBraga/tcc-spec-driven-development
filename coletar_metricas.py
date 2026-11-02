"""Preenche a planilha de métricas com os dados do agentsview.

Uso:
    python coletar_metricas.py adhoc-1 --simular
    python coletar_metricas.py adhoc-1 --rotular-auto
    python coletar_metricas.py sdd-1 --hurl-inicial hurl-inicial.txt --hurl-final hurl-final.txt

Cada sessão do agentsview precisa de um rótulo de fase: fase=especificacao, fase=geracao ou
fase=correcao. Com --rotular-auto, o script rotula pela ordem de início quando o número de sessões
bate com o esperado (ad-hoc: geração e correção; sdd: especificação, geração e correção).
"""
import argparse
import re
import shutil
import subprocess
import sys
import unicodedata
from datetime import datetime
from pathlib import Path

import json
from copy import copy

from openpyxl import load_workbook
from openpyxl.formula.translate import Translator
from openpyxl.utils import get_column_letter

sys.stdout.reconfigure(encoding="utf-8")

PLANILHA_PADRAO = Path(__file__).with_name("metricas-execucoes.xlsx")
LINHA_CABECALHO = 3
FASES = ("especificacao", "geracao", "correcao")
PALAVRA_FASE = {"especificacao": "especificacao", "geracao": "geracao", "correcao": "correcoes"}
FASES_ESPERADAS = {
    "adhoc": ("geracao", "correcao"),
    "sdd": ("especificacao", "geracao", "correcao"),
}
LOTE_MENSAGENS = 500


def agentsview(*args):
    r = subprocess.run(["agentsview", *args], capture_output=True, text=True, encoding="utf-8")
    if r.returncode != 0:
        sys.exit(f"erro no agentsview {' '.join(args)}: {r.stderr.strip() or r.stdout.strip()}")
    return r.stdout


def agentsview_json(*args):
    return json.loads(agentsview(*args, "--json"))


def normalizar(texto):
    sem_acento = unicodedata.normalize("NFKD", str(texto)).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]+", " ", sem_acento.lower()).strip()


def listar_sessoes(projeto):
    dados = agentsview_json("session", "list", "--project", projeto, "--include-children",
                            "--include-one-shot", "--include-automated")
    return sorted(dados["sessions"], key=lambda s: s["started_at"])


def fase_da_sessao(sessao):
    for rotulo in sessao.get("labels") or []:
        if rotulo.startswith("fase="):
            return rotulo.removeprefix("fase=")
    return None


def rotular_auto(pasta, sessoes, simular):
    esperado = FASES_ESPERADAS[pasta.split("-")[0]]
    uteis = [s for s in sessoes if s["message_count"] > 1 or s["total_output_tokens"]]
    if len(uteis) != len(esperado):
        sys.exit(f"{len(uteis)} sessões com conteúdo, esperado {len(esperado)} ({', '.join(esperado)}). "
                 "Rotule manualmente: agentsview session label <id> fase=<fase>")
    for sessao, fase in zip(uteis, esperado):
        sessao["labels"] = [*(sessao.get("labels") or []), f"fase={fase}"]
        print(f"rótulo fase={fase} -> {sessao['id'][:8]} ({sessao.get('display_name') or 'sem nome'})")
        if not simular:
            agentsview("session", "label", sessao["id"], f"fase={fase}")


def mensagens(sessao_id):
    todas, inicio = [], 0
    while True:
        lote = agentsview_json("session", "messages", sessao_id, "--from", str(inicio),
                               "--limit", str(LOTE_MENSAGENS))["messages"]
        todas += lote
        if len(lote) < LOTE_MENSAGENS:
            return todas
        inicio = lote[-1]["ordinal"] + 1


def tempo_ativo_segundos(sessao_id, limiar_s):
    """Soma os intervalos até cada mensagem do agente. Intervalos acima do limiar são espera humana
    (aprovação de permissão, por exemplo) e ficam de fora."""
    pontos = [(datetime.fromisoformat(m["timestamp"].replace("Z", "+00:00")), m["role"])
              for m in mensagens(sessao_id) if m.get("timestamp")]
    total = 0.0
    for (antes, _), (depois, papel) in zip(pontos, pontos[1:]):
        intervalo = (depois - antes).total_seconds()
        if papel != "user" and intervalo <= limiar_s:
            total += intervalo
    return total


def somar_fase(sessoes, limiar_s):
    soma = {"entrada": 0, "saida": 0, "cache_escrita": 0, "cache_leitura": 0, "custo": 0.0,
            "segundos": 0.0, "mensagens_usuario": 0, "sessoes": 0}
    modelos, versoes, inicios = set(), set(), []
    for s in sessoes:
        uso = agentsview_json("session", "usage", s["id"])
        itens = uso.get("breakdown") or []
        soma["entrada"] += sum(i["input_tokens"] for i in itens)
        soma["saida"] += sum(i["output_tokens"] for i in itens)
        soma["cache_escrita"] += sum(i["cache_creation_input_tokens"] for i in itens)
        soma["cache_leitura"] += sum(i["cache_read_input_tokens"] for i in itens)
        soma["custo"] += uso.get("cost_usd") or 0.0
        soma["segundos"] += tempo_ativo_segundos(s["id"], limiar_s)
        soma["mensagens_usuario"] += s["user_message_count"]
        soma["sessoes"] += 1
        modelos.update(uso.get("models") or [])
        versoes.add(s.get("source_version"))
        inicios.append(s["started_at"])
        if uso.get("total_output_tokens") != sum(i["output_tokens"] for i in itens):
            print(f"aviso: saída da sessão {s['id'][:8]} difere entre o total e o detalhamento")
    soma["modelos"], soma["versoes"], soma["inicios"] = modelos, versoes, inicios
    return soma


def ler_hurl(caminho):
    texto = Path(caminho).read_text(encoding="utf-8", errors="replace")
    executadas = re.search(r"Executed requests:\s+(\d+)", texto)
    if not executadas:
        sys.exit(f"{caminho}: não achei 'Executed requests' no resultado do Hurl")
    falhas = re.findall(r"^error:.*\n\s+--> (\S+)", texto, re.M)
    return int(executadas.group(1)), falhas


def grupo_da_coluna(nome):
    texto = normalizar(nome)
    return "sdd" if texto.startswith("sdd") else "adhoc" if texto.startswith("adhoc") else None


def exibir(valor, formato):
    if formato and formato.startswith("[h]"):
        segundos = round(valor * 86400)
        return f"{segundos // 3600}:{segundos % 3600 // 60:02d}:{segundos % 60:02d}"
    return valor


class Planilha:
    def __init__(self, caminho, coluna_nome, sobrescrever, simular, prefixos=()):
        self.caminho, self.sobrescrever, self.simular = Path(caminho), sobrescrever, simular
        self.prefixos = tuple(normalizar(p) for p in prefixos)
        self.wb = load_workbook(self.caminho)
        self.ws = self.wb["Execucoes"]
        self.linhas = {}
        for linha in range(1, self.ws.max_row + 1):
            rotulo = self.ws.cell(linha, 1).value
            if rotulo:
                self.linhas.setdefault(normalizar(rotulo), linha)
        self.escritos, self.pulados, self.ausentes = [], [], []
        self.coluna_criada = False
        cabecalho = self._cabecalho()
        if normalizar(coluna_nome) not in cabecalho:
            self._criar_coluna(coluna_nome)
            cabecalho = self._cabecalho()
        self.coluna = cabecalho[normalizar(coluna_nome)]

    def _cabecalho(self):
        return {normalizar(self.ws.cell(LINHA_CABECALHO, c).value): c
                for c in range(3, self.ws.max_column + 1) if self.ws.cell(LINHA_CABECALHO, c).value}

    def _criar_coluna(self, nome):
        """Cria a coluna copiando estilo e fórmulas da última coluna do mesmo grupo.
        Valores digitados ficam em branco; só constantes iguais em todas as colunas são mantidas."""
        ws = self.ws
        colunas = {c: ws.cell(LINHA_CABECALHO, c).value for c in range(3, ws.max_column + 1)
                   if ws.cell(LINHA_CABECALHO, c).value}
        mesmo_grupo = [c for c, n in colunas.items() if grupo_da_coluna(n) == grupo_da_coluna(nome)]
        origem = max(mesmo_grupo or colunas)
        nova = max(colunas) + 1
        for linha in range(1, ws.max_row + 1):
            antiga, destino = ws.cell(linha, origem), ws.cell(linha, nova)
            destino._style = copy(antiga._style)
            valor = antiga.value
            if linha == LINHA_CABECALHO:
                destino.value = nome
            elif isinstance(valor, str) and valor.startswith("="):
                destino.value = Translator(valor, origin=antiga.coordinate).translate_formula(destino.coordinate)
            elif valor == "n/a":
                destino.value = "n/a"
            elif valor is not None and self._constante(linha, colunas):
                destino.value = valor
        letra_origem, letra_nova = get_column_letter(origem), get_column_letter(nova)
        ws.column_dimensions[letra_nova].width = ws.column_dimensions[letra_origem].width
        self._ampliar_resumo(nome, letra_nova, colunas)
        self.coluna_criada = True
        print(f"coluna '{nome}' criada na planilha ({letra_nova}), copiada de '{colunas[origem]}'")

    def _constante(self, linha, colunas):
        rotulo = normalizar(self.ws.cell(linha, 1).value or "")
        if rotulo in ("data", "modelo", "versao do claude code"):
            return False
        valores = [self.ws.cell(linha, c).value for c in colunas]
        return (len(valores) > 1 and all(isinstance(v, str) and not v.startswith("=") and v != "n/a" for v in valores)
                and len(set(valores)) == 1)

    def _ampliar_resumo(self, nome, letra_nova, colunas):
        """Acrescenta a coluna nova às listas de referências do grupo na aba Resumo."""
        if "Resumo" not in self.wb.sheetnames:
            return
        nomes = {get_column_letter(c): n for c, n in colunas.items()}
        lista = re.compile(r"Execucoes!\$?[A-Z]+\$?\d+(?:,Execucoes!\$?[A-Z]+\$?\d+)*")

        def ampliar(achado):
            refs = re.findall(r"Execucoes!\$?([A-Z]+)\$?(\d+)", achado.group(0))
            letras = [letra for letra, _ in refs]
            if grupo_da_coluna(nomes.get(letras[0], "")) != grupo_da_coluna(nome) or letra_nova in letras:
                return achado.group(0)
            return f"{achado.group(0)},Execucoes!{letra_nova}{refs[0][1]}"

        for linha in self.wb["Resumo"].iter_rows():
            for celula in linha:
                if isinstance(celula.value, str) and celula.value.startswith("="):
                    celula.value = lista.sub(ampliar, celula.value)

    def tem_linha(self, rotulo):
        return normalizar(rotulo) in self.linhas

    def escrever(self, rotulo, valor, formato=None):
        linha = self.linhas.get(normalizar(rotulo))
        if linha is None:
            self.ausentes.append(rotulo)
            return
        celula = self.ws.cell(linha, self.coluna)
        atual = celula.value
        numero_como_texto = isinstance(atual, str) and re.fullmatch(r"\s*\d+([.,]\d+)?\s*", atual)
        vazio = atual is None or numero_como_texto or (
            isinstance(atual, str) and (not atual.strip() or atual.startswith("=")))
        forcar = self.sobrescrever or normalizar(rotulo).startswith(self.prefixos)
        if not vazio and not forcar:
            if str(atual).strip() == "n/a":
                motivo = "n/a"
            elif isinstance(atual, (int, float)) and isinstance(valor, (int, float)) \
                    and abs(atual - valor) > (0.006 if isinstance(valor, float) and valor > 0.5 else 0):
                motivo = f"DIFERE: planilha {atual!r}, agentsview {exibir(valor, formato)}"
            else:
                motivo = f"já tem {atual!r}"
            self.pulados.append(f"{rotulo}: {motivo}")
            return
        celula.value = valor
        if formato:
            celula.number_format = formato
        self.escritos.append(f"{rotulo} = {exibir(valor, formato)}")

    def salvar(self):
        if self.simular:
            return
        pasta_backup = self.caminho.parent / "backups-planilha"
        pasta_backup.mkdir(exist_ok=True)
        copia = pasta_backup / f"{self.caminho.stem}-{datetime.now():%Y%m%d-%H%M%S}{self.caminho.suffix}"
        shutil.copy2(self.caminho, copia)
        self.wb.calculation.fullCalcOnLoad = True
        try:
            self.wb.save(self.caminho)
        except PermissionError:
            sys.exit("não consegui salvar: feche a planilha no Excel e rode de novo")
        print(f"backup: {copia}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("pasta", help="nome da pasta da execução, igual ao cabeçalho da planilha (ex.: adhoc-1)")
    ap.add_argument("--planilha", default=str(PLANILHA_PADRAO))
    ap.add_argument("--rotular-auto", action="store_true")
    ap.add_argument("--limiar-min", type=float, default=5.0,
                    help="intervalos maiores que isso contam como espera humana (padrão 5)")
    ap.add_argument("--hurl-inicial")
    ap.add_argument("--hurl-final")
    ap.add_argument("--total-requisicoes", type=int, default=53)
    ap.add_argument("--sobrescrever", action="store_true")
    ap.add_argument("--sobrescrever-linhas", nargs="*", default=[], metavar="ROTULO",
                    help="sobrescreve só as linhas cujo rótulo começa com estes textos")
    ap.add_argument("--simular", action="store_true", help="mostra o que seria escrito, sem salvar")
    ap.add_argument("--sem-sync", action="store_true")
    ap.add_argument("--so-coluna", action="store_true",
                    help="só garante que a coluna da execução exista na planilha, sem preencher dados")
    args = ap.parse_args()

    if args.so_coluna:
        planilha = Planilha(args.planilha, args.pasta, args.sobrescrever, args.simular)
        if planilha.coluna_criada:
            planilha.salvar()
        else:
            print(f"coluna '{args.pasta}' já existe")
        return

    if not shutil.which("agentsview"):
        sys.exit("agentsview não está no PATH")
    if not args.sem_sync:
        agentsview("sync")

    projeto = args.pasta.replace("-", "_")
    sessoes = listar_sessoes(projeto)
    if not sessoes:
        sys.exit(f"nenhuma sessão no projeto '{projeto}' do agentsview")
    if args.rotular_auto:
        rotular_auto(args.pasta, sessoes, args.simular)

    por_fase = {f: [s for s in sessoes if fase_da_sessao(s) == f] for f in FASES}
    sem_fase = [s for s in sessoes if fase_da_sessao(s) not in FASES]
    for s in sem_fase:
        print(f"ignorada (sem rótulo de fase): {s['id'][:8]} {s.get('display_name') or ''} "
              f"[{s['message_count']} msgs, saída {s['total_output_tokens']}]")

    planilha = Planilha(args.planilha, args.pasta, args.sobrescrever, args.simular, args.sobrescrever_linhas)
    com_cache = planilha.tem_linha("Tokens de cache leitura geração")
    limiar = args.limiar_min * 60
    resumo = {f: somar_fase(por_fase[f], limiar) for f in FASES if por_fase[f]}
    if not resumo:
        sys.exit("nenhuma sessão com rótulo de fase. Rode com --rotular-auto ou rotule com: "
                 "agentsview session label <id> fase=<especificacao|geracao|correcao>")

    print(f"\n{projeto}: custo calculado pelo agentsview com tabela de preços da API (não é cobrança da assinatura)")
    for fase, r in resumo.items():
        print(f"  {fase:14} sessões {r['sessoes']} | tempo ativo {r['segundos'] / 60:5.1f} min | "
              f"entrada {r['entrada']:>10,} cache escrita {r['cache_escrita']:>10,} "
              f"cache leitura {r['cache_leitura']:>12,} | saída {r['saida']:>8,} | US$ {r['custo']:.4f}")

    for fase, r in resumo.items():
        palavra = PALAVRA_FASE[fase]
        planilha.escrever(f"Tempo {palavra}", r["segundos"] / 86400, "[h]:mm:ss")
        planilha.escrever(f"Tokens de saída {palavra}", r["saida"])
        planilha.escrever(f"Custo {palavra}", round(r["custo"], 4))
        if com_cache:
            planilha.escrever(f"Tokens de entrada {palavra}", r["entrada"])
            planilha.escrever(f"Tokens de cache leitura {palavra}", r["cache_leitura"])
            planilha.escrever(f"Tokens de cache escrita {palavra}", r["cache_escrita"])
        else:
            planilha.escrever(f"Tokens de entrada {palavra}",
                              r["entrada"] + r["cache_escrita"] + r["cache_leitura"])

    inicios = [i for r in resumo.values() for i in r["inicios"]]
    if inicios:
        data = datetime.fromisoformat(min(inicios).replace("Z", "+00:00")).astimezone().date()
        planilha.escrever("Data", datetime(data.year, data.month, data.day), "dd/mm/yyyy")
    versoes = {v for r in resumo.values() for v in r["versoes"] if v}
    modelos = {m for r in resumo.values() for m in r["modelos"]}
    if versoes:
        planilha.escrever("Versão do Claude Code", ", ".join(sorted(versoes)))
    if modelos:
        planilha.escrever("Modelo", ", ".join(sorted(modelos)))
    if "correcao" in resumo:
        planilha.escrever("Ciclos de correção", resumo["correcao"]["mensagens_usuario"])

    for rotulo, caminho in (("inicial", args.hurl_inicial), ("final", args.hurl_final)):
        if not caminho:
            continue
        executadas, falhas = ler_hurl(caminho)
        if executadas != args.total_requisicoes:
            print(f"aviso: {caminho} executou {executadas} requisições, esperado {args.total_requisicoes}")
        planilha.escrever(f"Requisições aprovadas {rotulo}", executadas - len(falhas))
        if falhas:
            print(f"\nfalhas do Hurl ({rotulo}), para classificar por categoria:")
            for local in falhas:
                print(f"  {local}")

    print("\nescrito:" if not args.simular else "\nseria escrito (simulação):")
    for item in planilha.escritos:
        print(f"  {item}")
    if planilha.pulados:
        print("pulado (use --sobrescrever para trocar):")
        for item in planilha.pulados:
            print(f"  {item}")
    if planilha.ausentes:
        print("linhas não encontradas na planilha:")
        for item in planilha.ausentes:
            print(f"  {item}")
    if not com_cache:
        print("nota: a planilha não tem linhas de cache; 'Tokens de entrada' recebeu entrada + cache.")
    planilha.salvar()


if __name__ == "__main__":
    main()
