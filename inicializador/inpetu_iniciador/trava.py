"""Instância única: só um iniciador por vez cuida do sistema e do banco (trava de arquivo; no Windows, msvcrt)."""
import os

from inpetu_iniciador import so

if so.WINDOWS:
    import msvcrt

    def _travar(arquivo):
        arquivo.seek(0)
        msvcrt.locking(arquivo.fileno(), msvcrt.LK_NBLCK, 1)

    def _destravar(arquivo):
        arquivo.seek(0)
        msvcrt.locking(arquivo.fileno(), msvcrt.LK_UNLCK, 1)
else:
    import fcntl

    def _travar(arquivo):
        fcntl.flock(arquivo.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)

    def _destravar(arquivo):
        fcntl.flock(arquivo.fileno(), fcntl.LOCK_UN)


class Trava:

    def __init__(self, caminho):
        self.caminho = caminho
        self._arquivo = None

    def adquirir(self):
        """Devolve False se outro iniciador já está aberto."""
        self.caminho.parent.mkdir(parents=True, exist_ok=True)
        arquivo = open(self.caminho, "a+", encoding="utf-8")
        try:
            _travar(arquivo)
        except OSError:
            arquivo.close()
            return False
        arquivo.seek(0)
        arquivo.truncate()
        arquivo.write(str(os.getpid()))
        arquivo.flush()
        self._arquivo = arquivo
        return True

    def liberar(self):
        if self._arquivo is not None:
            _destravar(self._arquivo)
            self._arquivo.close()
            self._arquivo = None
