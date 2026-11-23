"""Sobe e para o sistema: runtime/bin/java -jar app/inpetu-hub-sistema.jar numa porta livre de 127.0.0.1.

Antes de subir: para um sistema esquecido por uma execução anterior e copia o banco. Para parar: cria o arquivo de
parada (o sistema encerra em ordem e libera o banco) e só força depois do prazo.
"""
import dataclasses
import datetime
import os
import pathlib
import re
import signal
import subprocess
import time
import urllib.request

from inpetu_iniciador import VERSAO, backup, porta
from inpetu_iniciador.registro import girar

JAR = "inpetu-hub-sistema.jar"
MENSAGENS = {
    "DATABASE_IN_USE": "O banco está em uso por outra cópia do sistema. Feche a outra cópia e tente de novo.",
    "PORT_IN_USE": "A porta escolhida ficou ocupada. Tente iniciar de novo.",
    "MIGRATION_FAILED": "O banco não pôde ser atualizado. Os backups estão na pasta backups.",
    "FILE_ACCESS": "Sem permissão para usar a pasta de dados.",
    "OUT_OF_MEMORY": "O computador ficou sem memória para o sistema.",
}
FALHA = re.compile(r"event=SERVER_START_FAILED failure=([A-Z_]{1,40})")


class ErroDeSubida(Exception):
    """O sistema não subiu; a mensagem é para a pessoa e a categoria vai para o log."""

    def __init__(self, mensagem, falha="OTHER"):
        super().__init__(mensagem)
        self.falha = falha


@dataclasses.dataclass(frozen=True)
class Pacote:
    java: pathlib.Path
    jar: pathlib.Path

    @staticmethod
    def em(base):
        base = pathlib.Path(base)
        pacote = Pacote(base / "runtime" / "bin" / "java", base / "app" / JAR)
        if not pacote.java.is_file() or not pacote.jar.is_file():
            raise ErroDeSubida("O pacote do sistema está incompleto. Baixe o pacote de novo.", "PACKAGE_INCOMPLETE")
        return pacote


class Sistema:

    def __init__(self, pacote, pastas, registro, sessao, prazo_parada=20, prazo_subida=90):
        self.pacote = pacote
        self.pastas = pastas
        self.registro = registro
        self.sessao = sessao
        self.prazo_parada = prazo_parada
        self.prazo_subida = prazo_subida
        self._processo = None
        self._saida = None

    @property
    def rodando(self):
        return self._processo is not None and self._processo.poll() is None

    def iniciar(self):
        """Sobe o sistema e devolve o endereço quando /saude responder."""
        if self.rodando:
            return self.url
        self.pastas.criar()
        self._parar_esquecido()
        copia = backup.copiar(self.pastas, datetime.datetime.now())
        if copia is not None:
            self.registro.evento("DATABASE_BACKED_UP")
        numero = porta.livre()
        comando = [str(self.pacote.java), f"-Dinpetu.dados={self.pastas.dados}",
                   f"-Dinpetu.parada={self.pastas.parada}", f"-Dinpetu.session.id={self.sessao}",
                   f"-Dinpetu.launcher.version={VERSAO}", "-Dfile.encoding=UTF-8", "-jar", str(self.pacote.jar),
                   str(numero)]
        log = self.pastas.logs / "sistema.log"
        girar(log)
        self._saida = open(log, "ab")
        inicio = time.monotonic()
        self._processo = subprocess.Popen(comando, stdin=subprocess.DEVNULL, stdout=self._saida,
                                          stderr=subprocess.STDOUT, start_new_session=True)
        self.pastas.pid.write_text(str(self._processo.pid), encoding="utf-8")
        self.url = f"http://127.0.0.1:{numero}/"
        while time.monotonic() - inicio < self.prazo_subida:
            if self._processo.poll() is not None:
                codigo = self._processo.returncode
                self._encerrado()
                falha = self._falha_no_log(log)
                self.registro.evento("SYSTEM_START_FAILED", failure=falha, exitCode=max(codigo, 0))
                raise ErroDeSubida(MENSAGENS.get(falha, "O sistema não iniciou. Veja o log em logs/sistema.log."),
                                   falha)
            if _responde(self.url):
                self.registro.evento("SYSTEM_READY", port=numero,
                                     durationMs=int((time.monotonic() - inicio) * 1000))
                return self.url
            time.sleep(0.2)
        self.parar()
        self.registro.evento("SYSTEM_START_FAILED", failure="TIMEOUT")
        raise ErroDeSubida("O sistema demorou demais para iniciar. Tente de novo.", "TIMEOUT")

    def parar(self):
        """Encerra em ordem pelo arquivo de parada; força só depois do prazo. Devolve GRACEFUL, FORCED ou NOT_RUNNING."""
        if not self.rodando:
            self._encerrado()
            return "NOT_RUNNING"
        processo = self._processo
        resultado = "GRACEFUL" if _pedir_parada(self.pastas.parada, lambda: processo.poll() is not None,
                                                self.prazo_parada) else None
        if resultado is None:
            self._processo.kill()
            self._processo.wait(timeout=10)
            self.registro.evento("SYSTEM_STOP_FORCED")
            resultado = "FORCED"
        self._encerrado()
        self.registro.evento("SYSTEM_STOPPED", result=resultado)
        return resultado

    def _encerrado(self):
        if self._saida is not None:
            self._saida.close()
            self._saida = None
        self.pastas.pid.unlink(missing_ok=True)
        self.pastas.parada.unlink(missing_ok=True)

    def _parar_esquecido(self):
        """Um sistema que ficou rodando de uma execução anterior (o iniciador caiu) segura o banco: para antes."""
        try:
            pid = int(self.pastas.pid.read_text(encoding="utf-8").strip())
        except (OSError, ValueError):
            return
        if not _e_o_sistema(pid):
            self.pastas.pid.unlink(missing_ok=True)
            return
        if not _pedir_parada(self.pastas.parada, lambda: _colher(pid) or not _vivo(pid), self.prazo_parada):
            try:
                os.kill(pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
        self.pastas.pid.unlink(missing_ok=True)
        self.pastas.parada.unlink(missing_ok=True)
        self.registro.evento("ORPHAN_STOPPED")

    @staticmethod
    def _falha_no_log(log):
        try:
            fim = log.read_bytes()[-20000:].decode("utf-8", "replace")
        except OSError:
            return "OTHER"
        achados = FALHA.findall(fim)
        return achados[-1] if achados else "OTHER"


def _responde(url):
    try:
        with urllib.request.urlopen(url + "saude", timeout=2) as resposta:
            return resposta.status == 200
    except OSError:
        return False


def _vivo(pid):
    try:
        os.kill(pid, 0)
    except ProcessLookupError:
        return False
    except PermissionError:
        return True
    try:
        estado = pathlib.Path(f"/proc/{pid}/stat").read_text().split(")")[-1].split()[0]
        return estado != "Z"
    except OSError:
        return True


def _pedir_parada(arquivo, saiu, prazo):
    """Cria o arquivo de parada e espera o processo sair. Devolve se saiu dentro do prazo."""
    arquivo.write_text("parar", encoding="utf-8")
    limite = time.monotonic() + prazo
    while time.monotonic() < limite:
        if saiu():
            return True
        time.sleep(0.1)
    return saiu()


def _colher(pid):
    """Recolhe um processo que já saiu, se for filho deste (senão ele fica como zumbi e parece vivo). Devolve False."""
    try:
        os.waitpid(pid, os.WNOHANG)
    except ChildProcessError:
        pass
    return False


def _e_o_sistema(pid):
    """Só mexe num processo que é mesmo o sistema (pela linha de comando), nunca num pid reaproveitado."""
    try:
        linha = pathlib.Path(f"/proc/{pid}/cmdline").read_bytes().split(b"\0")
    except OSError:
        return False
    return any(parte.endswith(JAR.encode()) for parte in linha) and _vivo(pid)
