"""Orquestra as execuções do experimento (ad-hoc e SDD), uma por vez, sem IA no controle.

O script só decide e mede. A IA entra apenas como o próprio Claude Code, chamado em modo headless
(`claude -p`) para gerar o código e para aplicar cada correção.

Fluxo de cada execução:
  1. Prepara a pasta a partir do template e cria o repositório git com o commit inicial.
  2. Ad-hoc: envia o prompt inicial de prompts-adhoc.md. SDD: roda os comandos do Spec Kit em
     sessões separadas (especificação, depois implementação).
  3. Sobe o banco limpo e a aplicação, roda a suíte Hurl e guarda o resultado inicial.
  4. Com falhas, envia ao agente só os blocos de erro do Hurl (esperado e retornado) e repete,
     até o máximo de correções (padrão 3).
  5. Rotula as sessões no agentsview e preenche a coluna da planilha com coletar_metricas.py.

Uso:
    python orquestrar_execucoes.py --simular        # mostra o plano sem alterar nada
    python orquestrar_execucoes.py --so-preparar    # cria pastas, settings e git, sem executar
    python orquestrar_execucoes.py --sim            # executa tudo (pode levar horas e gastar tokens)
    python orquestrar_execucoes.py --sim --apenas adhoc-2 sdd-2

Pastas que já têm código gerado fora do orquestrador (as execuções manuais) nunca são alteradas.
Elas são puladas, e novas pastas numeradas são criadas a partir do template até haver 5 por grupo (adhoc-2 a adhoc-6 e sdd-2 a sdd-6).
"""
import argparse
import hashlib
import json
import os
import re
import shutil
import socket
import subprocess
import sys
import time
import uuid
from datetime import datetime, timedelta
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import urlopen

sys.stdout.reconfigure(encoding="utf-8")

RAIZ = Path(__file__).resolve().parent
EXEC = RAIZ / "execucoes"
GRUPOS = ("adhoc", "sdd")
TEMPLATES = {g: EXEC / f"{g}-template" for g in GRUPOS}
RESULTADOS = RAIZ / "resultados"
ORACULO = Path.home() / "tcc-oracle"
ARQUIVOS_HURL = ("auth.hurl", "errors_auth.hurl", "profiles.hurl", "errors_profiles.hurl")
TOTAL_REQUISICOES = 53
IGNORAR_NA_COPIA = (".env", "target", ".git")
PREFIXO_SUITE = "Executei a suíte de testes e tive estes erros:"
PREFIXO_INFRA = "A aplicação não sobe corretamente. Últimas linhas do log:"
FERRAMENTAS_PERMITIDAS = "Bash,Read,Edit,Write,Glob,Grep,Skill"


RE_LIMITE = re.compile(r"(hit|reached) your .{0,30}limit|usage limit", re.I)
MARGEM_RESET_S = 90
ESPERA_SEM_HORARIO_MIN = 30
DIAS_SEMANA = ("mon", "tue", "wed", "thu", "fri", "sat", "sun")
MESES = ("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")


class ErroOrquestracao(Exception):
    pass


class LimiteAtingido(ErroOrquestracao):
    """Estouro do limite de uso do Claude Code; reset é o horário em que o crédito volta (ou None)."""

    def __init__(self, mensagem, reset):
        super().__init__(mensagem)
        self.reset = reset


def log(msg):
    print(f"[{datetime.now():%H:%M:%S}] {msg}", flush=True)


def padrao_permissao(caminho):
    """Formato das regras de permissão do Claude Code: ~/... para o que está na home (o arquivo não
    carrega o nome do usuário) e //c/... para o resto. Padrões relativos com ../ não são aplicados."""
    p = Path(caminho).resolve()
    try:
        return "~/" + p.relative_to(Path.home()).as_posix()
    except ValueError:
        s = p.as_posix()
        return "//" + s[0].lower() + s[2:] if re.match(r"^[A-Za-z]:", s) else s


def matar_arvore(proc):
    subprocess.run(["taskkill", "/PID", str(proc.pid), "/T", "/F"], capture_output=True)


def rodar(cmd, cwd=None, timeout=None, env=None):
    return subprocess.run(cmd, cwd=cwd, capture_output=True, text=True, encoding="utf-8",
                          errors="replace", timeout=timeout, env=env)


# ---------------------------------------------------------------- pastas e settings

class Execucao:
    def __init__(self, grupo, numero):
        self.grupo, self.numero = grupo, numero
        self.nome = f"{grupo}-{numero}"
        self.caminho = EXEC / grupo / self.nome
        self.res = RESULTADOS / self.nome
        self.projeto = self.nome.replace("-", "_")

    @property
    def estado_arquivo(self):
        return self.res / "estado.json"

    def carregar_estado(self):
        if self.estado_arquivo.exists():
            return json.loads(self.estado_arquivo.read_text(encoding="utf-8"))
        return {}

    def salvar_estado(self, estado):
        self.res.mkdir(parents=True, exist_ok=True)
        self.estado_arquivo.write_text(json.dumps(estado, indent=2, ensure_ascii=False), encoding="utf-8")


def arquivos_com_hash(base):
    achados = {}
    for pasta, dirs, arquivos in os.walk(base):
        dirs[:] = [d for d in dirs if d not in IGNORAR_NA_COPIA]
        for nome in arquivos:
            caminho = Path(pasta) / nome
            relativo = caminho.relative_to(base).as_posix()
            if relativo in (".env", ".claude/settings.json"):
                continue
            achados[relativo] = hashlib.sha1(caminho.read_bytes()).hexdigest()
    return achados


def esta_limpa(execucao):
    return arquivos_com_hash(execucao.caminho) == arquivos_com_hash(TEMPLATES[execucao.grupo])


def criar_do_template(execucao):
    shutil.copytree(TEMPLATES[execucao.grupo], execucao.caminho,
                    ignore=shutil.ignore_patterns(*IGNORAR_NA_COPIA))


def dirs_de_execucao():
    dirs = [TEMPLATES[g] for g in GRUPOS]
    for g in GRUPOS:
        base = EXEC / g
        if base.exists():
            dirs += sorted(d for d in base.iterdir() if d.is_dir() and not d.name.startswith("_"))
    return dirs


def regenerar_settings():
    """Cada pasta nega leitura e escrita nas outras, no oráculo e nos documentos da pesquisa.
    As pastas ad-hoc também negam o Spec Kit."""
    todas = dirs_de_execucao()
    extras = [f"{padrao_permissao(RAIZ)}/*.md", f"{padrao_permissao(RAIZ)}/*.xlsx",
              f"{padrao_permissao(RAIZ)}/*.py", f"{padrao_permissao(RAIZ)}/resultados/**",
              f"{padrao_permissao(RAIZ)}/backups-planilha/**", f"{padrao_permissao(ORACULO)}/**"]
    for pasta in todas:
        negar = []
        for alvo in [f"{padrao_permissao(o)}/**" for o in todas if o != pasta] + extras:
            negar += [f"Read({alvo})", f"Edit({alvo})"]
        if pasta.parent.name == "adhoc" or pasta.name == "adhoc-template":
            negar += ["Bash(specify:*)", "Bash(uvx:*)", "Skill(speckit-*)"]
        destino = pasta / ".claude" / "settings.json"
        destino.parent.mkdir(parents=True, exist_ok=True)
        destino.write_text(json.dumps({"permissions": {"deny": negar}}, indent=2) + "\n", encoding="utf-8")


def planejar(por_grupo, politica):
    """Escolhe as execuções de cada grupo. Retorna (selecionadas, a_criar, puladas)."""
    selecionadas, a_criar, puladas = {}, [], []
    for g in GRUPOS:
        escolhidas, n = [], 1
        while len(escolhidas) < por_grupo:
            ex = Execucao(g, n)
            n += 1
            if not ex.caminho.exists():
                a_criar.append(ex)
                escolhidas.append(ex)
            elif ex.estado_arquivo.exists() or esta_limpa(ex):
                escolhidas.append(ex)
            elif politica == "arquivar":
                a_criar.append(ex)
                escolhidas.append(ex)
            else:
                puladas.append(ex)
        selecionadas[g] = escolhidas
    return selecionadas, a_criar, puladas


def alternar(selecionadas):
    ordem = []
    for i in range(max(len(v) for v in selecionadas.values())):
        for g in GRUPOS:
            if i < len(selecionadas[g]):
                ordem.append(selecionadas[g][i])
    return ordem


def git(caminho, *args):
    r = rodar(["git", "-c", "user.name=orquestrador", "-c", "user.email=orquestrador@local", *args],
              cwd=caminho)
    if r.returncode != 0:
        erros = [l for l in r.stderr.splitlines() if l.strip() and not l.startswith("warning:")]
        raise ErroOrquestracao(f"git {' '.join(args)} falhou: {' | '.join(erros)}")
    return r.stdout.strip()


def preparar_git(execucao, estado):
    if (execucao.caminho / ".git").exists():
        return
    git(execucao.caminho, "init", "-q")
    git(execucao.caminho, "add", "-A")
    git(execucao.caminho, "commit", "-q", "-m", "estado inicial")
    estado["commit_inicial"] = git(execucao.caminho, "rev-parse", "--short", "HEAD")


# ---------------------------------------------------------------- Claude Code headless

def executavel_claude():
    atalho = shutil.which("claude")
    if not atalho:
        raise ErroOrquestracao("claude não está no PATH")
    nativo = Path(atalho).parent / "node_modules" / "@anthropic-ai" / "claude-code" / "bin" / "claude.exe"
    return str(nativo if nativo.exists() else atalho)


def extrair_prompt_adhoc(execucao):
    texto = (execucao.caminho / "prompts-adhoc.md").read_text(encoding="utf-8")
    achado = re.search(r"````markdown\n(.*?)\n````", texto, re.S)
    if not achado:
        raise ErroOrquestracao(f"{execucao.nome}: bloco do prompt 1 não encontrado em prompts-adhoc.md")
    return achado.group(1)


def horario_reset(texto, agora=None):
    """Lê 'resets 3:20pm', 'resets Mon 3pm' ou 'resets Nov 3, 3pm' e devolve o próximo instante local."""
    achado = re.search(r"resets?\s+([^\n\"\\]*)", texto, re.I)
    if not achado:
        return None
    resto = achado.group(1)
    hora = re.search(r"(\d{1,2})(?::(\d{2}))?\s*([ap])\.?m", resto, re.I)
    if not hora:
        return None
    agora = agora or datetime.now()
    h = int(hora.group(1)) % 12 + (12 if hora.group(3).lower() == "p" else 0)
    alvo = agora.replace(hour=h, minute=int(hora.group(2) or 0), second=0, microsecond=0)
    mes = re.search(r"\b(" + "|".join(MESES) + r")[a-z]*\.?\s+(\d{1,2})\b", resto, re.I)
    dia = re.search(r"\b(" + "|".join(DIAS_SEMANA) + r")", resto, re.I)
    if mes:
        alvo = alvo.replace(month=MESES.index(mes.group(1).lower()) + 1, day=int(mes.group(2)))
        if alvo < agora:
            alvo = alvo.replace(year=alvo.year + 1)
    elif dia:
        alvo += timedelta(days=(DIAS_SEMANA.index(dia.group(1).lower()) - alvo.weekday()) % 7)
        if alvo <= agora:
            alvo += timedelta(days=7)
    elif alvo <= agora:
        alvo += timedelta(days=1)
    return alvo


def esperar_reset(execucao, reset):
    """Fica em stand-by até o crédito voltar, impedindo o Windows de suspender a máquina."""
    if reset is None:
        alvo = datetime.now() + timedelta(minutes=ESPERA_SEM_HORARIO_MIN)
        log(f"{execucao.nome}: horário do reset não identificado; nova tentativa em {ESPERA_SEM_HORARIO_MIN} min")
    else:
        alvo = reset + timedelta(seconds=MARGEM_RESET_S)
        log(f"{execucao.nome}: limite atingido; stand-by até {alvo:%d/%m %H:%M} (reset {reset:%H:%M} + margem)")
    try:
        import ctypes
        ctypes.windll.kernel32.SetThreadExecutionState(0x80000001)  # ES_CONTINUOUS | ES_SYSTEM_REQUIRED
    except Exception:
        pass
    try:
        ultimo = 0
        while True:
            falta = (alvo - datetime.now()).total_seconds()
            if falta <= 0:
                break
            if time.time() - ultimo >= 600:
                ultimo = time.time()
                log(f"{execucao.nome}: em stand-by, faltam {int(falta) // 60 + 1} min")
            time.sleep(min(30, falta))
    finally:
        try:
            ctypes.windll.kernel32.SetThreadExecutionState(0x80000000)
        except Exception:
            pass
    log(f"{execucao.nome}: stand-by encerrado, retomando")


def chamar_claude(execucao, prompt, sessao_id, retomar, rotulo, args):
    """Roda uma chamada headless e devolve o evento final de resultado."""
    execucao.res.mkdir(parents=True, exist_ok=True)
    saida = execucao.res / f"claude-{rotulo}.jsonl"
    cmd = [executavel_claude(), "-p"]
    por_stdin = not (prompt.startswith("/") and "\n" not in prompt)
    if not por_stdin:
        cmd.append(prompt)
    cmd += ["--output-format", "stream-json", "--verbose", "--model", args.modelo,
            "--effort", args.effort, "--permission-mode", "acceptEdits",
            "--allowedTools", FERRAMENTAS_PERMITIDAS,
            "--resume" if retomar else "--session-id", sessao_id]
    ambiente = {**os.environ, "DISABLE_AUTOUPDATER": "1"}
    log(f"{execucao.nome}: claude ({rotulo}) {'retomando' if retomar else 'nova sessão'} {sessao_id[:8]}")
    inicio = time.time()
    with saida.open("wb") as arquivo:
        proc = subprocess.Popen(cmd, cwd=execucao.caminho, stdin=subprocess.PIPE if por_stdin else subprocess.DEVNULL,
                                stdout=arquivo, stderr=subprocess.STDOUT, env=ambiente)
        if por_stdin:
            proc.stdin.write(prompt.encode("utf-8"))
            proc.stdin.close()
        ultimo_aviso = inicio
        while proc.poll() is None:
            time.sleep(5)
            agora = time.time()
            if agora - inicio > args.timeout_fase_min * 60:
                matar_arvore(proc)
                raise ErroOrquestracao(f"{execucao.nome}: {rotulo} passou de {args.timeout_fase_min} min")
            if agora - ultimo_aviso >= 60:
                ultimo_aviso = agora
                log(f"{execucao.nome}: {rotulo} em andamento ({int(agora - inicio) // 60} min, "
                    f"{saida.stat().st_size // 1024} KB de saída)")
    resultado = None
    for linha in saida.read_text(encoding="utf-8", errors="replace").splitlines():
        if linha.startswith("{") and '"type":"result"' in linha.replace(" ", ""):
            resultado = json.loads(linha)
    if proc.returncode != 0 or resultado is None or resultado.get("is_error"):
        resumo = (resultado or {}).get("result") or "sem evento de resultado"
        bruto = saida.read_text(encoding="utf-8", errors="replace")[-4000:]
        for texto in (str(resumo), bruto):
            if RE_LIMITE.search(texto):
                reset = horario_reset(texto)
                raise LimiteAtingido(f"{execucao.nome}: limite de uso atingido em {rotulo}: {str(resumo)[:200]}", reset)
        raise ErroOrquestracao(f"{execucao.nome}: claude ({rotulo}) falhou (código {proc.returncode}): "
                               f"{str(resumo)[:300]}. Detalhes em {saida}")
    log(f"{execucao.nome}: {rotulo} concluído em {int(time.time() - inicio) // 60} min, "
        f"{resultado.get('num_turns')} turnos, US$ {resultado.get('total_cost_usd', 0):.2f}")
    return resultado


# ---------------------------------------------------------------- banco, aplicação e Hurl

def ler_env(caminho):
    valores = {}
    arquivo = caminho / ".env"
    if arquivo.exists():
        for linha in arquivo.read_text(encoding="utf-8").splitlines():
            if "=" in linha and not linha.lstrip().startswith("#"):
                chave, valor = linha.split("=", 1)
                valores[chave.strip()] = valor.strip().strip('"').strip("'")
    return valores


def porta_ocupada(porta):
    with socket.socket() as s:
        s.settimeout(1)
        return s.connect_ex(("127.0.0.1", porta)) == 0


def liberar_porta(porta):
    """Mata quem escuta na porta (e o Maven que o iniciou, se for o caso). Devolve True se ficou livre."""
    if not porta_ocupada(porta):
        return True
    pids = set()
    # sem -p TCP: o Spring escuta em [::]:porta, que só aparece como TCPv6
    for linha in rodar(["netstat", "-ano"]).stdout.splitlines():
        partes = linha.split()
        if len(partes) >= 5 and partes[1].endswith(f":{porta}") and partes[4].isdigit() and int(partes[4]) > 4:
            pids.add(int(partes[4]))
    for pid in pids:
        consulta = (f"$p = Get-CimInstance Win32_Process -Filter 'ProcessId={pid}'; "
                    "$pai = Get-CimInstance Win32_Process -Filter \"ProcessId=$($p.ParentProcessId)\"; "
                    "Write-Output $pai.ProcessId; Write-Output $pai.CommandLine")
        saida = rodar(["powershell", "-NoProfile", "-Command", consulta]).stdout.splitlines()
        log(f"porta {porta} ocupada pelo PID {pid}; encerrando")
        subprocess.run(["taskkill", "/PID", str(pid), "/T", "/F"], capture_output=True)
        if len(saida) >= 2 and saida[0].strip().isdigit() and re.search(r"maven|mvnw", saida[1], re.I):
            subprocess.run(["taskkill", "/PID", saida[0].strip(), "/T", "/F"], capture_output=True)
    for _ in range(10):
        if not porta_ocupada(porta):
            return True
        time.sleep(1)
    return False


def derrubar_banco(execucao):
    if (execucao.caminho / "compose.yaml").exists():
        rodar(["docker", "compose", "down", "-v"], cwd=execucao.caminho, timeout=180)


def subir_banco(execucao):
    r = rodar(["docker", "compose", "up", "-d", "--wait", "--wait-timeout", "120", "db"],
              cwd=execucao.caminho, timeout=300)
    return r.returncode == 0, (r.stdout + r.stderr).strip()


def cauda(texto, linhas=30):
    return "\n".join(texto.strip().splitlines()[-linhas:])


def subir_app(execucao, rodada, args):
    """Sobe a aplicação com ./mvnw spring-boot:run e espera responder. Devolve (processo, erro)."""
    if not liberar_porta(args.porta):
        raise ErroOrquestracao(f"a porta {args.porta} já está em uso e não consegui liberá-la")
    log_app = execucao.res / f"app-{rodada}.log"
    ambiente = {**os.environ, **ler_env(execucao.caminho), "SERVER_PORT": str(args.porta)}
    arquivo = log_app.open("wb")
    proc = subprocess.Popen(["cmd", "/c", r".\mvnw.cmd", "-q", "spring-boot:run"], cwd=execucao.caminho,
                            stdout=arquivo, stderr=subprocess.STDOUT, env=ambiente)
    limite = time.time() + args.timeout_app_s
    while time.time() < limite:
        if proc.poll() is not None:
            arquivo.close()
            return None, cauda(log_app.read_text(encoding="utf-8", errors="replace"))
        try:
            urlopen(f"http://localhost:{args.porta}/", timeout=3)
            return proc, None
        except HTTPError:
            return proc, None
        except (URLError, ConnectionError, TimeoutError, OSError):
            time.sleep(3)
    matar_arvore(proc)
    arquivo.close()
    return None, "a aplicação não respondeu a tempo.\n" + cauda(log_app.read_text(encoding="utf-8", errors="replace"))


def extrair_blocos(texto):
    """Cada bloco vai da linha seguinte a 'error:' até a próxima linha em branco."""
    blocos, atual = [], None
    for linha in texto.splitlines():
        if linha.startswith("error:"):
            if atual:
                blocos.append("\n".join(atual).rstrip())
            atual = []
        elif atual is not None:
            if not linha.strip():
                if atual:
                    blocos.append("\n".join(atual).rstrip())
                atual = None
            else: 
                atual.append(linha)
    if atual:
        blocos.append("\n".join(atual).rstrip())
    return blocos


def rodar_hurl(execucao, rodada, args):
    r = rodar(["hurl", "--test", "--continue-on-error", "--jobs", "1",
               "--variable", f"host=http://localhost:{args.porta}",
               "--variable", f"uid={int(time.time())}", *ARQUIVOS_HURL], cwd=ORACULO, timeout=900)
    texto = r.stderr + "\n" + r.stdout
    (execucao.res / f"hurl-{rodada}.txt").write_text(texto, encoding="utf-8")
    achado = re.search(r"Executed requests:\s+(\d+)", texto)
    if not achado:
        raise ErroOrquestracao(f"{execucao.nome}: saída do Hurl sem 'Executed requests'. Veja hurl-{rodada}.txt")
    blocos = extrair_blocos(texto)
    return int(achado.group(1)), blocos


def executar_rodada(execucao, rodada, args):
    """Banco limpo, aplicação no ar, suíte completa. Devolve o resultado da rodada."""
    derrubar_banco(execucao)
    ok, erro = subir_banco(execucao)
    if not ok:
        return {"rodada": rodada, "infra": "banco", "executadas": 0, "falhas": TOTAL_REQUISICOES,
                "aprovadas": 0, "feedback": f"{PREFIXO_INFRA}\n{cauda(erro)}"}
    proc, erro = subir_app(execucao, rodada, args)
    if proc is None:
        derrubar_banco(execucao)
        return {"rodada": rodada, "infra": "app", "executadas": 0, "falhas": TOTAL_REQUISICOES,
                "aprovadas": 0, "feedback": f"{PREFIXO_INFRA}\n{erro}"}
    try:
        executadas, blocos = rodar_hurl(execucao, rodada, args)
    finally:
        matar_arvore(proc)
        derrubar_banco(execucao)
    falhas = len(blocos)
    return {"rodada": rodada, "infra": None, "executadas": executadas, "falhas": falhas,
            "aprovadas": max(executadas - falhas, 0),
            "feedback": PREFIXO_SUITE + "\n\n" + "\n\n".join(blocos) if blocos else ""}


# ---------------------------------------------------------------- execução completa

def rotular_sessao(sessao_id, fase):
    r = rodar(["agentsview", "session", "label", sessao_id, f"fase={fase}"])
    if r.returncode != 0:
        log(f"aviso: não consegui rotular {sessao_id[:8]} como {fase}: {r.stderr.strip()}")


def marcar_checkpoint(execucao, estado, nome):
    git(execucao.caminho, "add", "-A")  # target/ e .env já estão no .gitignore do template
    git(execucao.caminho, "commit", "-q", "--allow-empty", "-m", nome)
    estado.setdefault("checkpoints", {})[nome] = git(execucao.caminho, "rev-parse", "HEAD")


def reverter_fase(execucao, estado, fase, descartar, preservar_env):
    """Volta o código ao checkpoint do início da fase e descarta as sessões dela."""
    checkpoint = estado["checkpoints"][f"antes-{fase}"]
    try:
        derrubar_banco(execucao)
    except Exception:
        pass
    git(execucao.caminho, "reset", "-q", "--hard", checkpoint)
    git(execucao.caminho, "clean", "-fdxq", *(["-e", ".env"] if preservar_env else []))
    perdidas = execucao.res / "interrompidas"
    perdidas.mkdir(parents=True, exist_ok=True)
    inicio = estado.get("fase_inicio_ts", 0) - 1
    carimbo = datetime.now().strftime("%Y%m%d-%H%M%S")
    for arq in execucao.res.glob("claude-*.jsonl"):
        if arq.stat().st_mtime >= inicio:
            arq.replace(perdidas / f"{carimbo}-{arq.name}")
    descartadas = [estado["sessoes"].pop(k) for k in descartar if k in estado["sessoes"]]
    estado.setdefault("interrupcoes", []).append(
        {"fase": fase, "quando": datetime.now().isoformat(timespec="seconds"), "sessoes_perdidas": descartadas})
    execucao.salvar_estado(estado)
    log(f"{execucao.nome}: fase '{fase}' desfeita (rollback para {checkpoint[:7]}); "
        f"sessões descartadas: {', '.join(s[:8] for s in descartadas) or 'nenhuma'}")


def executar_fase(execucao, estado, args, fase, descartar, acao, preservar_env=True):
    """Roda acao() com rollback automático: se o limite estourar (ou a execução anterior foi
    interrompida no meio da fase), desfaz o código da fase, espera o reset e recomeça do zero."""
    if args.sem_git:
        return acao()
    if estado.get("fase_em_curso") == fase:
        log(f"{execucao.nome}: fase '{fase}' ficou incompleta na execução anterior")
        reverter_fase(execucao, estado, fase, descartar, preservar_env)
    else:
        marcar_checkpoint(execucao, estado, f"antes-{fase}")
    estado["fase_em_curso"] = fase
    estado["fase_inicio_ts"] = time.time()
    execucao.salvar_estado(estado)
    while True:
        try:
            resultado = acao()
            break
        except LimiteAtingido as limite:
            log(str(limite))
            reverter_fase(execucao, estado, fase, descartar, preservar_env)
            esperar_reset(execucao, limite.reset)
            estado["fase_inicio_ts"] = time.time()
    estado.pop("fase_em_curso", None)
    execucao.salvar_estado(estado)
    return resultado


def executar(execucao, args):
    estado = execucao.carregar_estado()
    if estado.get("concluida"):
        log(f"{execucao.nome}: já concluída, pulando")
        return estado
    estado.setdefault("sessoes", {})
    estado.setdefault("rodadas", [])
    estado.setdefault("inicio", datetime.now().isoformat(timespec="seconds"))
    if not args.sem_git:
        preparar_git(execucao, estado)
    execucao.salvar_estado(estado)
    if not args.sem_planilha:
        garantir_coluna(execucao)

    if execucao.grupo == "sdd":
        if not estado.get("especificacao_ok"):
            def especificar():
                sessao = estado["sessoes"].setdefault("especificacao", str(uuid.uuid4()))
                for i, comando in enumerate(args.sdd_especificacao):
                    prompt = f"/speckit-{comando}"
                    if comando == "specify":
                        prompt += " Use o arquivo descricao-base.md, na raiz do projeto, como única fonte de requisitos."
                    chamar_claude(execucao, prompt, sessao, i > 0, f"especificacao-{comando}", args)
                rotular_sessao(sessao, "especificacao")
            executar_fase(execucao, estado, args, "especificacao", ["especificacao"], especificar, preservar_env=False)
            estado["especificacao_ok"] = True
            execucao.salvar_estado(estado)
        if not estado.get("geracao_ok"):
            def implementar():
                sessao = estado["sessoes"].setdefault("geracao", str(uuid.uuid4()))
                chamar_claude(execucao, f"/speckit-{args.sdd_implementacao}", sessao, False, "geracao", args)
                rotular_sessao(sessao, "geracao")
            executar_fase(execucao, estado, args, "geracao", ["geracao"], implementar, preservar_env=False)
            estado["geracao_ok"] = True
            execucao.salvar_estado(estado)
    elif not estado.get("geracao_ok"):
        def gerar():
            sessao = estado["sessoes"].setdefault("geracao", str(uuid.uuid4()))
            chamar_claude(execucao, extrair_prompt_adhoc(execucao), sessao, False, "geracao", args)
            rotular_sessao(sessao, "geracao")
        executar_fase(execucao, estado, args, "geracao", ["geracao"], gerar, preservar_env=False)
        estado["geracao_ok"] = True
        execucao.salvar_estado(estado)

    while True:
        rodadas = estado["rodadas"]
        if not rodadas:
            log(f"{execucao.nome}: suíte inicial")
            rodadas.append(executar_rodada(execucao, 0, args))
        else:
            ultima = rodadas[-1]
            if ultima["falhas"] == 0 or len(rodadas) - 1 >= args.max_correcoes:
                break
            ciclo = len(rodadas)
            if estado.get("correcoes_chamadas", 0) < ciclo:
                feedback = ultima["feedback"]
                (execucao.res / f"feedback-{ciclo}.txt").write_text(feedback, encoding="utf-8")

                def corrigir():
                    sessao = estado["sessoes"].setdefault("correcao", str(uuid.uuid4()))
                    chamar_claude(execucao, feedback, sessao, ciclo > 1, f"correcao-{ciclo}", args)
                # no ciclo 1 a sessão de correção nasce na fase; nos seguintes ela já tem ciclos concluídos
                executar_fase(execucao, estado, args, f"correcao-{ciclo}", [] if ciclo > 1 else ["correcao"], corrigir)
                estado["correcoes_chamadas"] = ciclo
                execucao.salvar_estado(estado)
            log(f"{execucao.nome}: suíte após a correção {ciclo}")
            rodadas.append(executar_rodada(execucao, ciclo, args))
        r = rodadas[-1]
        log(f"{execucao.nome}: rodada {r['rodada']}: {r['aprovadas']}/{TOTAL_REQUISICOES} aprovadas"
            + (f" (falha de {r['infra']})" if r["infra"] else ""))
        execucao.salvar_estado(estado)

    if "correcao" in estado["sessoes"] and estado.get("correcoes_chamadas"):
        rotular_sessao(estado["sessoes"]["correcao"], "correcao")
    estado["concluida"] = True
    estado["fim"] = datetime.now().isoformat(timespec="seconds")
    estado["resumo"] = {"aprovadas_inicial": rodadas[0]["aprovadas"], "aprovadas_final": rodadas[-1]["aprovadas"],
                        "ciclos": estado.get("correcoes_chamadas", 0), "chegou_a_100": rodadas[-1]["falhas"] == 0}
    execucao.salvar_estado(estado)
    if not args.sem_planilha:
        preencher_planilha(execucao, estado, args)
    return estado


def garantir_coluna(execucao):
    """Cria a coluna da execução na planilha, se ainda não existir."""
    r = rodar([sys.executable, str(RAIZ / "coletar_metricas.py"), execucao.nome, "--so-coluna"], timeout=120)
    saida = (r.stdout + r.stderr).strip().splitlines()
    if r.returncode != 0:
        log(f"aviso: não consegui garantir a coluna de {execucao.nome} na planilha: {saida[-1] if saida else ''}")
    elif saida and "criada" in saida[0]:
        log(f"{execucao.nome}: {saida[0]}")


def preencher_planilha(execucao, estado, args):
    comando = [sys.executable, str(RAIZ / "coletar_metricas.py"), execucao.nome, "--limiar-min", "30",
               "--hurl-inicial", str(execucao.res / "hurl-0.txt"),
               "--hurl-final", str(execucao.res / f"hurl-{len(estado['rodadas']) - 1}.txt")]
    r = rodar(comando, timeout=600)
    (execucao.res / "planilha.log").write_text(r.stdout + r.stderr, encoding="utf-8")
    if r.returncode != 0:
        log(f"aviso: planilha não preenchida para {execucao.nome}: {cauda(r.stdout + r.stderr, 3)}")
    else:
        log(f"{execucao.nome}: planilha preenchida (detalhes em {execucao.res / 'planilha.log'})")


# ---------------------------------------------------------------- verificações e principal

def verificar_ambiente(args):
    problemas = []
    for programa in ("docker", "hurl", "git", "agentsview", "claude", "java"):
        if not shutil.which(programa):
            problemas.append(f"{programa} não está no PATH")
    if shutil.which("docker") and rodar(["docker", "info", "--format", "{{.ServerVersion}}"]).returncode != 0:
        problemas.append("o daemon do Docker não está rodando")
    faltando = [a for a in ARQUIVOS_HURL if not (ORACULO / a).exists()]
    if faltando:
        problemas.append(f"arquivos do oráculo ausentes em {ORACULO}: {', '.join(faltando)}")
    if not liberar_porta(args.porta):
        problemas.append(f"a porta {args.porta} já está em uso e não consegui liberá-la")
    for g in GRUPOS:
        if not TEMPLATES[g].exists():
            problemas.append(f"template ausente: {TEMPLATES[g]}")
    return problemas


def imprimir_tabela(execucoes):
    print(f"\n{'execução':10} {'inicial':>8} {'final':>8} {'ciclos':>7}  situação")
    for ex in execucoes:
        e = ex.carregar_estado()
        r = e.get("resumo")
        if r:
            print(f"{ex.nome:10} {r['aprovadas_inicial']:>5}/53 {r['aprovadas_final']:>5}/53 {r['ciclos']:>7}  "
                  f"{'100%' if r['chegou_a_100'] else 'parou no teto'}")
        else:
            print(f"{ex.nome:10} {'-':>8} {'-':>8} {'-':>7}  {'incompleta' if e else 'não iniciada'}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--por-grupo", type=int, default=5,
                    help="execuções por grupo, sem contar as puladas (5 com adhoc-1/sdd-1 puladas = até o 6)")
    ap.add_argument("--sujas", choices=("pular", "arquivar"), default="pular",
                    help="pasta com código gerado fora do orquestrador: pular (padrão) ou arquivar em "
                         "execucoes/_arquivadas e recriar do template")
    ap.add_argument("--apenas", nargs="*", default=[], metavar="NOME", help="roda só estas execuções (ex.: adhoc-2)")
    ap.add_argument("--max-correcoes", type=int, default=3)
    ap.add_argument("--porta", type=int, default=8081)
    ap.add_argument("--modelo", default="claude-opus-5-5")
    ap.add_argument("--effort", default="medium", choices=("low", "medium", "high", "xhigh", "max"))
    ap.add_argument("--sdd-especificacao", default="specify,plan,tasks",
                    type=lambda s: [c for c in s.split(",") if c], help="comandos do Spec Kit da fase de especificação")
    ap.add_argument("--sdd-implementacao", default="implement")
    ap.add_argument("--timeout-fase-min", type=int, default=90)
    ap.add_argument("--timeout-app-s", type=int, default=300)
    ap.add_argument("--sem-git", action="store_true")
    ap.add_argument("--sem-planilha", action="store_true")
    ap.add_argument("--simular", action="store_true", help="mostra o plano sem alterar nada")
    ap.add_argument("--so-preparar", action="store_true", help="cria pastas, settings e git, sem executar")
    ap.add_argument("--sim", action="store_true", help="confirma a execução sem perguntar")
    args = ap.parse_args()

    selecionadas, a_criar, puladas = planejar(args.por_grupo, args.sujas)
    ordem = alternar(selecionadas)
    if args.apenas:
        ordem = [ex for ex in ordem if ex.nome in args.apenas]

    print("Plano:")
    for ex in ordem:
        situacao = "criar do template" if ex in a_criar else ("retomar" if ex.estado_arquivo.exists() else "usar (limpa)")
        print(f"  {ex.nome:10} {situacao}")
    for ex in puladas:
        print(f"  {ex.nome:10} pulada: tem código gerado fora do orquestrador (intocada)")
    if args.simular:
        return

    for ex in selecionadas_para_criar(a_criar, ordem, args):
        if ex.caminho.exists():
            destino = EXEC / "_arquivadas" / f"{ex.nome}-{datetime.now():%Y%m%d-%H%M%S}"
            destino.parent.mkdir(exist_ok=True)
            shutil.move(str(ex.caminho), str(destino))
            log(f"arquivada: {ex.nome} -> {destino}")
        criar_do_template(ex)
        log(f"criada a partir do template: {ex.nome}")
    regenerar_settings()
    log("settings.json regenerados em todas as pastas")
    if args.so_preparar:
        return

    problemas = verificar_ambiente(args)
    if problemas:
        sys.exit("Ambiente não está pronto:\n  " + "\n  ".join(problemas))
    if not args.sim:
        print("\nO script vai:\n"
              "  - chamar o Claude Code (consome tokens da sua assinatura e pode levar horas);\n"
              "  - subir e derrubar o banco de cada pasta com 'docker compose down -v' (apaga os dados locais dela);\n"
              f"  - iniciar a aplicação na porta {args.porta}.")
        if input("Digite 'sim' para continuar: ").strip().lower() != "sim":
            sys.exit("cancelado")

    try:
        for ex in ordem:
            log(f"=== {ex.nome} ===")
            executar(ex, args)
    except ErroOrquestracao as erro:
        imprimir_tabela(ordem)
        sys.exit(f"\nparado: {erro}\nRode de novo para retomar de onde parou.")
    imprimir_tabela(ordem)


def selecionadas_para_criar(a_criar, ordem, args):
    return [ex for ex in a_criar if ex in ordem]


if __name__ == "__main__":
    main()
