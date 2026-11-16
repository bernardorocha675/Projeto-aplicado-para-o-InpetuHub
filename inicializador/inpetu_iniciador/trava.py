"""Instância única: só um iniciador por vez cuida do sistema e do banco (trava de arquivo do Linux)."""
import fcntl
import os


class Trava:

    def __init__(self, caminho):
        self.caminho = caminho
        self._arquivo = None

    def adquirir(self):
        """Devolve False se outro iniciador já está aberto."""
        self.caminho.parent.mkdir(parents=True, exist_ok=True)
        arquivo = open(self.caminho, "a+", encoding="utf-8")
        try:
            fcntl.flock(arquivo.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
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
            fcntl.flock(self._arquivo.fileno(), fcntl.LOCK_UN)
            self._arquivo.close()
            self._arquivo = None
