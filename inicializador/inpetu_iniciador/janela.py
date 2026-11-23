"""A janela do iniciador (tkinter): estado, mensagem e os botões Iniciar, Parar, Abrir navegador e Salvar
relatório.

A janela só lê o estado do Controle a cada 200 ms (o tkinter não aceita chamadas de outras linhas).
"""
import signal
import tkinter as tk
from tkinter import filedialog, ttk

from inpetu_iniciador import relatorio
from inpetu_iniciador.controle import Estado

INTERVALO_MS = 200


class Janela:

    def __init__(self, controle, registrar=lambda evento: None, salvar_relatorio=lambda destino: False):
        self.controle = controle
        self.registrar = registrar
        self.salvar_relatorio = salvar_relatorio
        self.fechando = None
        self.raiz = tk.Tk()
        self.raiz.title("InPETU Hub")
        self.raiz.minsize(460, 220)
        quadro = ttk.Frame(self.raiz, padding=24)
        quadro.pack(fill="both", expand=True)
        ttk.Label(quadro, text="InPETU Hub", font=("TkDefaultFont", 18, "bold")).pack(anchor="w")
        self.estado = ttk.Label(quadro, font=("TkDefaultFont", 12, "bold"))
        self.estado.pack(anchor="w", pady=(12, 0))
        self.mensagem = ttk.Label(quadro, wraplength=420, justify="left")
        self.mensagem.pack(anchor="w", pady=(4, 16))
        botoes = ttk.Frame(quadro)
        botoes.pack(anchor="w")
        self.iniciar = ttk.Button(botoes, text="Iniciar", command=lambda: self._botao("BUTTON_START", self.controle.iniciar))
        self.parar = ttk.Button(botoes, text="Parar", command=lambda: self._botao("BUTTON_STOP", self.controle.parar))
        self.navegador = ttk.Button(botoes, text="Abrir navegador",
                                    command=lambda: self._botao("BUTTON_BROWSER", self.controle.abrir_navegador))
        self.relatorio = ttk.Button(botoes, text="Salvar relatório", command=self.pedir_relatorio)
        for botao in (self.iniciar, self.parar, self.navegador, self.relatorio):
            botao.pack(side="left", padx=(0, 8))
        self.aviso = ttk.Label(quadro, wraplength=460, justify="left")
        self.aviso.pack(anchor="w", pady=(12, 0))
        ttk.Label(quadro, text="Fechar esta janela para o sistema com segurança.", foreground="#5f6b77").pack(
            anchor="w", pady=(16, 0))
        self.raiz.protocol("WM_DELETE_WINDOW", self.fechar)
        self.atualizar()

    def rodar(self):
        # Desligar o computador, sair da sessão ou fechar o terminal: fecha como o X (para o sistema em ordem).
        # No Windows não há SIGHUP; o Tk avisa o fim da sessão pelo protocolo WM_SAVE_YOURSELF.
        for nome in ("SIGTERM", "SIGINT", "SIGHUP", "SIGBREAK"):
            if hasattr(signal, nome):
                signal.signal(getattr(signal, nome), lambda *_: self.raiz.after(0, self.fechar))
        self.raiz.protocol("WM_SAVE_YOURSELF", self.fechar)
        self.raiz.after(INTERVALO_MS, self._vigiar)
        self.raiz.mainloop()

    def atualizar(self):
        estado = self.controle.estado
        self.estado.configure(text=estado.value)
        self.mensagem.configure(text=self.controle.mensagem)
        ocupado = estado in (Estado.INICIANDO, Estado.PARANDO) or self.fechando is not None
        self.iniciar.state(["disabled"] if ocupado or estado == Estado.RODANDO else ["!disabled"])
        self.parar.state(["!disabled"] if estado == Estado.RODANDO and not ocupado else ["disabled"])
        self.navegador.state(["!disabled"] if estado == Estado.RODANDO and not ocupado else ["disabled"])

    def _botao(self, evento, acao):
        self.registrar(evento)
        acao()

    def escolher_arquivo(self):
        return filedialog.asksaveasfilename(parent=self.raiz, title="Salvar relatório de teste",
                                            initialfile=relatorio.nome_sugerido(), defaultextension=".txt",
                                            filetypes=[("Texto", "*.txt")])

    def pedir_relatorio(self):
        destino = self.escolher_arquivo()
        if not destino:
            return
        if self.salvar_relatorio(destino):
            self.aviso.configure(text="Relatório salvo. Confira o arquivo e mande para a equipe.")
        else:
            self.aviso.configure(text="Não foi possível salvar o relatório. Escolha outra pasta.")

    def fechar(self):
        """Fechar a janela para o sistema em ordem antes de sair."""
        if self.fechando is None:
            self.registrar("WINDOW_CLOSED")
            self.fechando = self.controle.parar()

    def _vigiar(self):
        self.atualizar()
        if self.fechando is not None and self.fechando.done():
            self.raiz.destroy()
            return
        self.raiz.after(INTERVALO_MS, self._vigiar)
