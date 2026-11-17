"""A janela (tkinter): mostra o estado, liga os botões ao controle e fecha só depois de parar o sistema.

Só roda com o tkinter e uma tela (DISPLAY, ou o Windows); na CI do Linux, sem tela, é pulado.
"""
import concurrent.futures
import os
import unittest

from inpetu_iniciador.controle import Estado

try:
    import tkinter
    TELA = (os.name == "nt" or bool(os.environ.get("DISPLAY"))) and tkinter.TkVersion >= 8.6
except ImportError:
    TELA = False


class ControleFalso:

    def __init__(self):
        self.estado = Estado.PARADO
        self.mensagem = "O sistema está parado. Clique em Iniciar."
        self.chamadas = []
        self.parada = concurrent.futures.Future()

    def iniciar(self):
        self.chamadas.append("iniciar")

    def parar(self):
        self.chamadas.append("parar")
        return self.parada

    def abrir_navegador(self):
        self.chamadas.append("navegador")


@unittest.skipUnless(TELA, "sem tkinter ou sem tela")
class JanelaTest(unittest.TestCase):

    def setUp(self):
        from inpetu_iniciador.janela import Janela
        self.controle = ControleFalso()
        self.janela = Janela(self.controle)
        self.addCleanup(lambda: self._existe() and self.janela.raiz.destroy())

    def habilitados(self):
        return [b.cget("text") for b in (self.janela.iniciar, self.janela.parar, self.janela.navegador)
                if not b.instate(["disabled"])]

    def test_botoes_seguem_o_estado(self):
        self.assertEqual("InPETU Hub", self.janela.raiz.title())
        self.assertEqual(["Iniciar"], self.habilitados())
        self.assertEqual("Parado", self.janela.estado.cget("text"))
        for estado in (Estado.INICIANDO, Estado.PARANDO):
            self.controle.estado = estado
            self.janela.atualizar()
            self.assertEqual([], self.habilitados(), estado)
        self.controle.estado = Estado.RODANDO
        self.controle.mensagem = "Pronto em http://127.0.0.1:1234/"
        self.janela.atualizar()
        self.assertEqual(["Parar", "Abrir navegador"], self.habilitados())
        self.assertEqual("Pronto em http://127.0.0.1:1234/", self.janela.mensagem.cget("text"))
        self.controle.estado = Estado.ERRO
        self.janela.atualizar()
        self.assertEqual(["Iniciar"], self.habilitados(), "depois de um erro dá para tentar de novo")

    def test_botoes_chamam_o_controle(self):
        self.janela.iniciar.invoke()
        self.controle.estado = Estado.RODANDO
        self.janela.atualizar()
        self.janela.navegador.invoke()
        self.janela.parar.invoke()
        self.assertEqual(["iniciar", "navegador", "parar"], self.controle.chamadas)

    def test_fechar_espera_o_sistema_parar(self):
        self.controle.estado = Estado.RODANDO
        self.janela.fechar()
        self.janela.raiz.update()
        self.janela._vigiar()
        self.assertTrue(self._existe(), "ainda parando: a janela fica")
        self.assertEqual([], self.habilitados(), "fechando: botões desligados")
        self.janela.fechar()
        self.assertEqual(["parar"], self.controle.chamadas, "fechar duas vezes não pede duas paradas")
        self.controle.parada.set_result("GRACEFUL")
        self.janela._vigiar()
        self.assertFalse(self._existe(), "parou: a janela fecha")

    def _existe(self):
        try:
            return bool(self.janela.raiz.winfo_exists())
        except tkinter.TclError:
            return False
