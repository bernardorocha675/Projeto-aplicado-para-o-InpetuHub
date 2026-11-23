"""Relatório de teste: a linha do tempo das últimas sessões, para a pessoa mandar à equipe.

Os logs já são feitos sem dados pessoais; aqui eles são lidos de novo e só passa o que está no formato e nas listas
abaixo. O que não passa vira uma contagem. Nada é enviado: a pessoa escolhe onde salvar.
"""
import datetime
import json
import platform
import re

from inpetu_iniciador import VERSAO

SESSOES = 5
ROTAS = {
    "/", "/admin", "/admin/cadastros/espacos", "/admin/cadastros/espacos/editar",
    "/admin/cadastros/espacos/excluir", "/admin/cadastros/espacos/salvar", "/admin/reservas",
    "/admin/reservas/{id}", "/admin/solicitacoes", "/admin/solicitacoes/aceitar", "/admin/solicitacoes/recusar",
    "/agenda", "/conta/senha", "/disponibilidade", "/login", "/reserva", "/reservas/recebida", "/reservas/resumo",
    "/reservas/solicitar", "/sair", "/saude", "/tela", "ASSET", "OTHER"
}
CAMPOS = {
    "academico", "ativo", "atual", "busca", "capacidade", "cargo", "cidade", "como", "data", "disponivel",
    "documento", "email", "empresa", "espaco", "espacoId", "financiado", "forcaMaior", "horaFim", "horaInicio",
    "id", "identidade", "inscricaoPaga", "justificativa", "mobiliario", "motivo", "nome", "nova",
    "numeroParticipantes", "objetivo", "observacoes", "persona", "personaId", "segmento", "senha", "situacao",
    "status", "telefone", "tipoDocumento", "titulo", "usuario"
}
ALVOS = CAMPOS | {"agenda.js", "autenticacao", "confirmar.js", "contas", "espacos", "mostrar-senha.js", "reservas",
                  "solicitacao", "tela.js"}
CODIGO = re.compile(r"[A-Z][A-Z_]{0,39}")
NUMERO = re.compile(r"[0-9]{1,9}")
PONTO = re.compile(r"[A-Za-z]{1,60}\.[A-Za-z]{1,60}:[0-9]{1,6}")
ACAO = re.compile(r"/admin/reservas/\{id\}/(aprovar|recusar|cancelar|alterar|pagamento|concluir)")
CLASSE = re.compile(r"(java|javax|jakarta|jdk|sun|br\.com\.inpetu|org\.(h2|eclipse\.jetty|flywaydb|apache))\."
                    r"[A-Za-z0-9_.$<>]{1,200}(:-?[0-9]{1,6})?")
VERSOES = re.compile(r"1\.[0-9]{1,4}\.[0-9]{1,4}(-(alpha|beta|rc)\.[0-9]{1,4}|-SNAPSHOT)?")
INTERNAS = re.compile(r"InPETU Hub pronto em http://127\.0\.0\.1:\d+/")
HORA = re.compile(r"20\d\d-\d\d-\d\dT\d\d:\d\d:\d\d(\.\d{1,9})?(Z|\+00:00)")
SESSAO = re.compile(r"[0-9a-f]{32}")
PEDIDO = re.compile(r"[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")
NIVEIS = {"INFO", "WARN", "ERROR", "DEBUG"}


def _rota(valor):
    return valor in ROTAS or ACAO.fullmatch(valor) is not None


def _alvo(valor):
    return valor in ALVOS or CODIGO.fullmatch(valor) is not None


def _campos(valor):
    return all(c in CAMPOS for c in valor.split(","))


def _formato(padrao):
    return lambda valor: padrao.fullmatch(valor) is not None


CHAVES = {"event": _formato(CODIGO), "method": _formato(CODIGO), "route": _rota, "page": _rota,
          "status": _formato(NUMERO), "durationMs": _formato(NUMERO), "line": _formato(NUMERO),
          "action": _formato(CODIGO), "result": _formato(CODIGO), "type": _formato(CODIGO),
          "failure": _formato(CODIGO), "target": _alvo, "form": lambda v: v in ("solicitacao", "espacos"),
          "fields": _campos, "rule": lambda v: v == "OTHER" or PONTO.fullmatch(v) is not None,
          "task": lambda v: v in ("thread", "vencimento") or CODIGO.fullmatch(v) is not None}


def _mensagem(texto):
    """A mensagem do sistema, se todas as partes estiverem no formato; senão None."""
    if not isinstance(texto, str) or not texto.startswith("event="):
        return None
    for parte in texto.split(" "):
        chave, igual, valor = parte.partition("=")
        if not igual or chave not in CHAVES or not CHAVES[chave](valor):
            return None
    return texto


def _falha(evento):
    partes = []
    tipo = evento.get("exceptionType")
    if isinstance(tipo, str) and CLASSE.fullmatch(tipo):
        partes.append(tipo)
    causas = evento.get("causes")
    if isinstance(causas, list):
        partes += [c for c in causas[:8] if isinstance(c, str) and CLASSE.fullmatch(c)]
    pontos = evento.get("rootFrames") or evento.get("frames")
    if isinstance(pontos, list):
        partes += [f"em {p}" for p in pontos[:5] if isinstance(p, str) and CLASSE.fullmatch(p)]
    falha = evento.get("failure")
    if isinstance(falha, str) and CODIGO.fullmatch(falha):
        partes.append(f"falha={falha}")
    return partes


def _hora(texto):
    """Só depois do formato conferido; uma data impossível vira o começo do tempo, para não derrubar o relatório."""
    try:
        return _momento(texto)
    except ValueError:
        return datetime.datetime(2000, 1, 1, tzinfo=datetime.timezone.utc)


def _momento(texto):
    return datetime.datetime.fromisoformat(texto.replace("Z", "+00:00"))


def _ler(linha, do_sistema):
    """(hora, sessão, texto, erro, versão do sistema), None se fora do formato ou () se é só ruído conhecido."""
    if INTERNAS.fullmatch(linha):
        return ()
    try:
        e = json.loads(linha)
    except ValueError:
        return None
    if not isinstance(e, dict):
        return None
    hora, sessao = e.get("time"), e.get("sessionId")
    if not (isinstance(hora, str) and HORA.fullmatch(hora) and isinstance(sessao, str) and SESSAO.fullmatch(sessao)):
        return None
    if do_sistema:
        if e.get("message") == "FRAMEWORK_EVENT" and e.get("level") != "ERROR":
            return ()
        texto = _mensagem(e.get("message"))
        if texto is None:
            return None
        nivel = e.get("level") if e.get("level") in NIVEIS else "INFO"
        partes = [f"sistema    {nivel:<5} {texto}"]
        detalhes = _falha(e)
        pedido = e.get("requestId")
        if nivel == "ERROR" and isinstance(pedido, str) and PEDIDO.fullmatch(pedido):
            detalhes.append(f"referência {pedido}")
        partes += [f"           {d}" for d in detalhes]
        versao = e.get("appVersion")
        versao = versao if isinstance(versao, str) and VERSOES.fullmatch(versao) else None
        return _hora(hora), sessao, "\n           ".join(partes), nivel == "ERROR", versao
    evento = e.get("event")
    if not isinstance(evento, str) or not CODIGO.fullmatch(evento):
        return None
    campos = [f"{c}={e[c]}" for c in ("port", "durationMs", "exitCode", "count", "attempt", "status")
              if isinstance(e.get(c), int) and not isinstance(e.get(c), bool)]
    campos += [f"{c}={e[c]}" for c in ("result", "failure") if isinstance(e.get(c), str) and CODIGO.fullmatch(e[c])]
    erro = evento.endswith("_FAILED") and evento != "UPDATE_CHECK_FAILED"
    return _hora(hora), sessao, " ".join([f"iniciador  {'ERROR' if erro else 'INFO':<5} {evento}", *campos]), erro, None


def gerar(pastas, sessao):
    eventos, omitidas = [], 0
    for nome, do_sistema in (("iniciador.log", False), ("sistema.log", True)):
        for sufixo in (".3", ".2", ".1", ""):
            try:
                linhas = (pastas.logs / (nome + sufixo)).read_text(encoding="utf-8", errors="replace").splitlines()
            except OSError:
                continue
            for linha in linhas:
                evento = _ler(linha, do_sistema)
                # Cada aviso da página já é um evento; a consulta que o trouxe e os arquivos só fazem volume.
                if evento is None:
                    omitidas += 1
                elif evento and " route=/tela status=204" not in evento[2] and " route=ASSET status=2" not in evento[2]:
                    eventos.append(evento)
    eventos.sort(key=lambda e: e[0])
    ordem = {}
    for e in eventos:
        ordem[e[1]] = e[0]
    ultimas = sorted(ordem, key=ordem.get)[-SESSOES:]
    if sessao in ordem and sessao not in ultimas:
        ultimas = [*ultimas[1:], sessao]
    escolhidos = [e for e in eventos if e[1] in ultimas]
    saida = ["Relatório de teste do InPETU Hub", f"Versão: {VERSAO}",
             f"Sistema: {platform.system()} {platform.release()} {platform.machine()}",
             f"Sessões: {len(ultimas)}   Eventos: {len(escolhidos)}   Erros: {sum(e[3] for e in escolhidos)}"
             f"   Linhas fora do formato: {omitidas}",
             "Horários em UTC (Brasília: 3 horas a menos). Sem senhas, formulários ou pastas.", ""]
    for s in ultimas:
        versoes = sorted({e[4] for e in escolhidos if e[1] == s and e[4]})
        saida.append(f"== Sessão {s[-8:]}" + (f", sistema {', '.join(versoes)}" if versoes else "")
                     + (" (atual)" if s == sessao else ""))
        saida += [f"{hora:%Y-%m-%d %H:%M:%S}.{hora.microsecond // 1000:03d}  {texto}" for hora, dona, texto, _, _ in escolhidos if dona == s]
        saida.append("")
    return "\n".join(saida)


def nome_sugerido():
    return f"InPETU-relatorio-v{VERSAO}-{datetime.datetime.now():%Y%m%d-%H%M}.txt"


def salvar(pastas, registro, destino):
    """Grava o relatório onde a pessoa escolheu. Devolve False se não deu."""
    try:
        with open(destino, "w", encoding="utf-8") as arquivo:
            arquivo.write(gerar(pastas, registro.sessao))
    except OSError:
        return False
    registro.evento("REPORT_SAVED")
    return True
