#!/usr/bin/env python3
"""Teste de fumaça do pacote pronto: extrai o zip numa pasta com espaço e acento e roda o executável de verdade.

Uso: python3 scripts/smoke_pacote.py dist/InPETU-Hub-Linux-x64-v1.0.0-alpha.2.zip [--anterior OUTRO.zip]
Com --anterior (um pacote de versão menor), confere também a passagem: com a versão nova instalada, o executável
antigo passa a vez para o iniciador novo.
Confere: sobe sem janela e sem consultar o GitHub; agenda, disponibilidade, login de demonstração e painel; um
orçamento em reais (os dados de idioma do Java mínimo); encerramento em ordem por sinal; logs sem a pasta da pessoa
e com a versão do pacote; e, na segunda abertura, o backup do banco.
"""
import datetime
import http.cookiejar
import json
import pathlib
import re
import shutil
import signal
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile

ESPERA = 180


class Falha(Exception):
    pass


def falhar(texto):
    raise Falha(texto)


def erros_do_sistema(dados):
    """O tipo e as linhas dos erros que o sistema registrou (o log não tem dados pessoais)."""
    log = dados / "logs" / "sistema.log"
    linhas = []
    if log.exists():
        for linha in log.read_text(encoding="utf-8", errors="replace").splitlines():
            try:
                evento = json.loads(linha)
            except ValueError:
                linhas.append(linha[:300])
                continue
            if "exceptionType" in evento:
                linhas.append(f"{evento['message']} {evento['exceptionType']} {evento.get('causes', [])}"
                              f" {evento.get('frames', [])[:6]} {evento.get('rootFrames', [])[:6]}")
    return "\n".join(linhas[-5:])


def extrair(pacote, destino):
    """Extrai como o iniciador faria (os modos de execução vêm do zip)."""
    with zipfile.ZipFile(pacote) as z:
        for info in z.infolist():
            alvo = destino / info.filename
            if info.is_dir():
                alvo.mkdir(parents=True, exist_ok=True)
                continue
            alvo.parent.mkdir(parents=True, exist_ok=True)
            with z.open(info) as origem, open(alvo, "wb") as saida:
                shutil.copyfileobj(origem, saida)
            alvo.chmod(0o755 if (info.external_attr >> 16) & 0o111 else 0o644)
    return next(destino.iterdir())


def subir(executavel, ambiente, registro):
    processo = subprocess.Popen([str(executavel), "--sem-janela", "--sem-atualizacao"], env=ambiente,
                                stdout=open(registro, "w", encoding="utf-8"), stderr=subprocess.STDOUT)
    limite = time.monotonic() + ESPERA
    while time.monotonic() < limite:
        texto = registro.read_text(encoding="utf-8", errors="replace")
        achado = re.search(r"Pronto em (http://127\.0\.0\.1:\d+/)", texto)
        if achado:
            return processo, achado.group(1)
        if processo.poll() is not None:
            falhar(f"o iniciador saiu com {processo.returncode}: {texto[-2000:]}")
        time.sleep(0.5)
    processo.kill()
    falhar("o sistema não ficou pronto a tempo")


def parar(processo):
    processo.send_signal(signal.SIGTERM)
    try:
        codigo = processo.wait(timeout=60)
    except subprocess.TimeoutExpired:
        processo.kill()
        falhar("o iniciador não encerrou em 60 s depois do SIGTERM")
    if codigo != 0:
        falhar(f"o iniciador encerrou com {codigo}")


def conferir_telas(url):
    biscoitos = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def abrir(caminho, dados=None):
        corpo = urllib.parse.urlencode(dados).encode() if dados else None
        with biscoitos.open(url + caminho.lstrip("/"), data=corpo, timeout=30) as resposta:
            return resposta.status, resposta.read().decode("utf-8")

    status, agenda = abrir("/agenda")
    if status != 200 or "Agenda de Eventos" not in agenda:
        falhar("a agenda não abriu (as telas JSP precisam do compilador no Java mínimo)")
    status, json_dia = abrir("/disponibilidade?mes=2026-11")
    if status != 200 or json.loads(json_dia)["dias"][0]["espacos"].__len__() != 10:
        falhar("a disponibilidade não trouxe os 10 espaços")
    _, login = abrir("/login")
    token = re.search(r'name="_csrf" value="([^"]+)"', login).group(1)
    status, painel = abrir("/login", {"_csrf": token, "usuario": "bernardo", "senha": "bernardo040"})
    if "<h1>Painel</h1>" not in painel:
        falhar("o login de demonstração não levou ao painel")
    dia = datetime.date.today() + datetime.timedelta(days=7)
    while dia.weekday() == 6:
        dia += datetime.timedelta(days=1)
    if dia.year == 2026:
        _, formulario = abrir(f"/reserva?data={dia}&espacoId=1&horaInicio=08:00&horaFim=12:00")
        token = re.search(r'name="_csrf" value="([^"]+)"', formulario).group(1)
        persona = re.search(r'<option value="(\d+)">P6 ', formulario).group(1)
        _, resumo = abrir("/reservas/resumo", {"_csrf": token, "data": str(dia), "espacoId": "1",
                                                "horaInicio": "08:00", "horaFim": "12:00", "nome": "Teste Fumaça",
                                                "tipoDocumento": "NENHUM", "documento": "",
                                                "email": "fumaca@example.com", "telefone": "48999991234",
                                                "cidade": "Florianópolis", "titulo": "Fumaça", "personaId": persona,
                                                "numeroParticipantes": "5", "objetivo": "Testar o pacote"})
        if "R$ 1.600,00" not in resumo:
            achados = re.findall(r"R\$[^<]{0,15}|role=\"alert\">[^<]{0,200}", resumo)[:4]
            falhar(f"o orçamento não saiu em reais: {achados}")
    else:
        print("aviso: sem tabela de preços para", dia.year, "— orçamento não conferido")


def conferir_passagem(anterior, ambiente, base, dados, versao):
    raiz = extrair(anterior, base / "Pacote Antigo")
    processo, url = subir(raiz / "inpetu-hub", ambiente, base / "saida-anterior.txt")
    try:
        conferir_telas(url)
    finally:
        parar(processo)
    eventos = [json.loads(l)["event"] for l in (dados / "logs" / "iniciador.log").read_text().splitlines()]
    if "LAUNCHER_HANDOVER" not in eventos:
        falhar("o executável antigo não passou a vez para o iniciador novo")
    ultimo = [l for l in (dados / "logs" / "sistema.log").read_text(encoding="utf-8").splitlines()
              if "SERVER_READY" in l][-1]
    if f'"launcherVersion":"{versao}"' not in ultimo:
        falhar("depois da passagem, o sistema não subiu pelo iniciador novo")
    print("passagem para o iniciador novo aprovada")


def main():
    pacote = pathlib.Path(sys.argv[1]).resolve()
    anterior = pathlib.Path(sys.argv[sys.argv.index("--anterior") + 1]).resolve() if "--anterior" in sys.argv else None
    versao = re.search(r"-v(1\.[^/]+)\.zip$", pacote.name).group(1)
    base = pathlib.Path(tempfile.mkdtemp(prefix="fumaça inpetu "))
    try:
        dados = base / "Configuração de Fulano" / "InPETU-Hub" / "1"
        raiz = extrair(pacote, base / "Pacote Ação")
        ambiente = {"PATH": "/usr/bin:/bin", "HOME": str(base / "Casa de Fulano"),
                    "XDG_CONFIG_HOME": str(base / "Configuração de Fulano"), "LANG": "C"}
        for vez in (1, 2):
            inicio = time.monotonic()
            processo, url = subir(raiz / "inpetu-hub", ambiente, base / f"saida-{vez}.txt")
            print(f"vez {vez}: pronto em {time.monotonic() - inicio:.1f} s")
            try:
                conferir_telas(url)
            finally:
                parar(processo)
        eventos = [json.loads(l)["event"] for l in (dados / "logs" / "iniciador.log").read_text().splitlines()]
        if eventos.count("SYSTEM_STOPPED") != 2 or "SYSTEM_STOP_FORCED" in eventos:
            falhar(f"paradas fora de ordem: {eventos}")
        if "EMBEDDED_INSTALLED" not in eventos:
            falhar("o pacote do executável não foi instalado em versoes/")
        if not (dados / "versoes" / versao / "inpetu-hub").is_file():
            falhar("a versão não ficou em versoes/")
        sistema = (dados / "logs" / "sistema.log").read_text(encoding="utf-8")
        if sistema.count("event=SERVER_STOPPED") != 2:
            falhar("o sistema não registrou as duas paradas")
        if f'"appVersion":"{versao}"' not in sistema or f'"launcherVersion":"{versao}"' not in sistema:
            falhar("as versões do pacote não chegaram ao log")
        for log in (dados / "logs").iterdir():
            if "Fulano" in log.read_text(encoding="utf-8", errors="replace"):
                falhar(f"{log.name} guarda a pasta da pessoa")
        if len(list((dados / "backups").glob("inpetu-*.mv.db"))) != 1:
            falhar("faltou o backup antes da segunda abertura")
        if anterior is not None:
            conferir_passagem(anterior, ambiente, base, dados, versao)
        print(f"Fumaça aprovada: {pacote.name}")
    except (Falha, urllib.error.URLError) as erro:
        raise SystemExit(f"FALHOU: {erro}\n{erros_do_sistema(dados)}") from None
    finally:
        shutil.rmtree(base, ignore_errors=True)


if __name__ == "__main__":
    main()
