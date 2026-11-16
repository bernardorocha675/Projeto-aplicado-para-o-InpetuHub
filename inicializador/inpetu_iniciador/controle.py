"""O que a janela mostra e faz, sem depender do tkinter: estado, mensagem e as ações em segundo plano."""
import concurrent.futures
import enum
import threading
import webbrowser

from inpetu_iniciador.sistema import ErroDeSubida


class Estado(enum.Enum):
    PARADO = "Parado"
    INICIANDO = "Iniciando…"
    RODANDO = "Rodando"
    PARANDO = "Parando…"
    ERRO = "Não iniciou"


class Controle:

    def __init__(self, sistema, abrir=webbrowser.open):
        self.sistema = sistema
        self.abrir = abrir
        self.estado = Estado.PARADO
        self.mensagem = "O sistema está parado. Clique em Iniciar."
        self.url = None
        self._ja_abriu = False
        self._ouvintes = []
        self._trava = threading.Lock()
        self._fila = concurrent.futures.ThreadPoolExecutor(max_workers=1, thread_name_prefix="inpetu-controle")

    def ao_mudar(self, ouvinte):
        self._ouvintes.append(ouvinte)

    def iniciar(self):
        return self._fila.submit(self._iniciar)

    def parar(self):
        return self._fila.submit(self._parar)

    def abrir_navegador(self):
        if self.url:
            self.abrir(self.url)

    def encerrar(self):
        """Ao fechar a janela: para o sistema e espera."""
        self.parar().result()
        self._fila.shutdown(wait=True)

    def _iniciar(self):
        if self.estado == Estado.RODANDO:
            return
        self._mudar(Estado.INICIANDO, "Iniciando o sistema e conferindo o banco…")
        try:
            self.url = self.sistema.iniciar()
        except ErroDeSubida as erro:
            self.url = None
            self._mudar(Estado.ERRO, str(erro))
            return
        self._mudar(Estado.RODANDO, f"Pronto em {self.url}")
        if not self._ja_abriu:
            self._ja_abriu = True
            self.abrir(self.url)

    def _parar(self):
        if self.estado not in (Estado.RODANDO, Estado.INICIANDO) and not self.sistema.rodando:
            return
        self._mudar(Estado.PARANDO, "Parando o sistema e gravando o banco…")
        self.sistema.parar()
        self.url = None
        self._mudar(Estado.PARADO, "O sistema está parado. Clique em Iniciar.")

    def _mudar(self, estado, mensagem):
        with self._trava:
            self.estado = estado
            self.mensagem = mensagem
        for ouvinte in list(self._ouvintes):
            ouvinte()
