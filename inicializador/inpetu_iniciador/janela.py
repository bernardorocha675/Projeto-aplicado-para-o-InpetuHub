"""A janela do iniciador (tkinter): estado, mensagem e os botões Iniciar, Parar e Abrir navegador.

A janela só lê o estado do Controle a cada 200 ms (o tkinter não aceita chamadas de outras linhas).
"""
import tkinter as tk
from tkinter import ttk

from inpetu_iniciador.controle import Estado

INTERVALO_MS = 200


class Janela:

    def __init__(self, controle):
        self.controle = controle
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
        self.iniciar = ttk.Button(botoes, text="Iniciar", command=self.controle.iniciar)
        self.parar = ttk.Button(botoes, text="Parar", command=self.controle.parar)
        self.navegador = ttk.Button(botoes, text="Abrir navegador", command=self.controle.abrir_navegador)
        for botao in (self.iniciar, self.parar, self.navegador):
            botao.pack(side="left", padx=(0, 8))
        ttk.Label(quadro, text="Fechar esta janela para o sistema com segurança.", foreground="#5f6b77").pack(
            anchor="w", pady=(16, 0))
        self.raiz.protocol("WM_DELETE_WINDOW", self.fechar)
        self.atualizar()

    def rodar(self):
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

    def fechar(self):
        """Fechar a janela para o sistema em ordem antes de sair."""
        if self.fechando is None:
            self.fechando = self.controle.parar()

    def _vigiar(self):
        self.atualizar()
        if self.fechando is not None and self.fechando.done():
            self.raiz.destroy()
            return
        self.raiz.after(INTERVALO_MS, self._vigiar)
