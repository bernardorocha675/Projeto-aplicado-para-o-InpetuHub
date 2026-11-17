"""Abre o iniciador: python3 -m inpetu_iniciador [--pacote PASTA | --desenvolvimento] [--sem-janela]."""
import argparse
import pathlib
import shutil
import signal
import sys
import threading
import uuid

from inpetu_iniciador import entrega
from inpetu_iniciador.controle import Controle, Estado
from inpetu_iniciador.github import Releases
from inpetu_iniciador.preparo import preparar
from inpetu_iniciador.pastas import Pastas
from inpetu_iniciador.registro import Registro
from inpetu_iniciador.sistema import ErroDeSubida, Pacote, Sistema
from inpetu_iniciador.trava import Trava

RAIZ_DO_REPOSITORIO = pathlib.Path(__file__).resolve().parents[2]
REPOSITORIO = "bernardorocha675/Projeto-aplicado-para-o-InpetuHub"
API = "https://api.github.com"


def pasta_do_executavel(argumentos):
    return argumentos.pacote or (pathlib.Path(sys.executable).parent if getattr(sys, "frozen", False)
                                 else pathlib.Path.cwd())


def pacote_de_desenvolvimento(argumentos):
    if argumentos.desenvolvimento:
        java = shutil.which("java")
        jar = RAIZ_DO_REPOSITORIO / "sistema" / "target" / "inpetu-hub-sistema.jar"
        if java is None or not jar.is_file():
            raise ErroDeSubida("Para o modo de desenvolvimento, gere o jar (cd sistema && ./mvnw package) e tenha"
                               " o JDK 25 no PATH.", "PACKAGE_INCOMPLETE")
        return Pacote(pathlib.Path(java), jar)
    return None


def avisar(texto):
    try:
        import tkinter
        from tkinter import messagebox
        raiz = tkinter.Tk()
        raiz.withdraw()
        messagebox.showerror("InPETU Hub", texto)
        raiz.destroy()
    except Exception:
        print(texto, file=sys.stderr)


def sem_janela(controle):
    """Sobe, mostra o endereço e para em ordem com Ctrl+C (ou o sinal de encerramento)."""
    controle.abrir = lambda endereco: None  # sem janela (testes e servidores): o navegador não abre sozinho
    fim = threading.Event()
    signal.signal(signal.SIGTERM, lambda *_: fim.set())
    signal.signal(signal.SIGINT, lambda *_: fim.set())
    controle.iniciar().result()
    print(controle.mensagem, flush=True)
    if controle.estado != Estado.RODANDO:
        return 1
    fim.wait()
    return 0


def main(lista=None):
    leitor = argparse.ArgumentParser(prog="inpetu_iniciador")
    leitor.add_argument("--pacote", type=pathlib.Path, help="pasta com runtime/ e app/ (padrão: a do executável)")
    leitor.add_argument("--desenvolvimento", action="store_true", help="usa o java do PATH e sistema/target")
    leitor.add_argument("--sem-janela", action="store_true", help="sobe sem janela e para com Ctrl+C")
    leitor.add_argument("--sem-atualizacao", action="store_true", help="não procura versão nova no GitHub")
    leitor.add_argument(entrega.MARCA, action="store_true", help=argparse.SUPPRESS)
    argumentos = leitor.parse_args(lista)
    pastas = Pastas.do_ambiente()
    pastas.criar()
    trava = Trava(pastas.trava)
    if not trava.adquirir():
        avisar("O iniciador do InPETU Hub já está aberto. Use a janela que já está aberta.")
        return 1
    sessao = uuid.uuid4().hex
    registro = Registro(pastas, sessao)
    registro.evento("LAUNCHER_STARTED")
    controle = None
    try:
        try:
            desenvolvimento = pacote_de_desenvolvimento(argumentos)
        except ErroDeSubida as erro:
            registro.evento("LAUNCHER_FAILED", failure=erro.falha)
            avisar(str(erro))
            return 1
        sistema = Sistema(desenvolvimento, pastas, registro, sessao)
        if desenvolvimento is not None:
            controle = Controle(sistema)
        else:
            releases = None if argumentos.sem_atualizacao else Releases(API, REPOSITORIO)
            repassar = [a for a in (lista if lista is not None else sys.argv[1:]) if a != entrega.MARCA]

            def passar(executavel):
                registro.evento("LAUNCHER_HANDOVER")
                trava.liberar()
                entrega.entregar(executavel, repassar)

            controle = Controle(sistema, preparar=lambda: preparar(pastas, registro, pasta_do_executavel(argumentos),
                                                                   releases, sys.argv[1:]), entregar=passar)
        if argumentos.sem_janela:
            return sem_janela(controle)
        from inpetu_iniciador.janela import Janela
        janela = Janela(controle)
        controle.iniciar()
        janela.rodar()
        return 0
    finally:
        if controle is not None:
            controle.encerrar()
        registro.evento("LAUNCHER_CLOSED")
        trava.liberar()


if __name__ == "__main__":
    sys.exit(main())
