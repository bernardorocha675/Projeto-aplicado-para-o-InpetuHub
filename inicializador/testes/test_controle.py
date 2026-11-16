"""O controle por trás da janela: estados, mensagens e o botão de abrir o navegador."""
from inpetu_iniciador.controle import Controle, Estado
from inpetu_iniciador.pastas import Pastas
from inpetu_iniciador.registro import Registro
from inpetu_iniciador.sistema import Pacote, Sistema
from testes.apoio import ComPasta, modo_falso

SESSAO = "0123456789abcdef0123456789abcdef"


class ControleTest(ComPasta):

    def controle(self):
        pastas = Pastas.do_ambiente(self.ambiente)
        pastas.criar()
        sistema = Sistema(Pacote.em(self.pacote_falso()), pastas, Registro(pastas, SESSAO), SESSAO,
                          prazo_parada=5, prazo_subida=20)
        abertos = []
        controle = Controle(sistema, abrir=abertos.append)
        self.addCleanup(lambda: controle.parar().result(timeout=20))
        mudancas = []
        controle.ao_mudar(lambda: mudancas.append(controle.estado))
        return controle, mudancas, abertos

    def test_iniciar_abre_o_navegador_uma_vez_e_parar_volta_ao_inicio(self):
        modo_falso(self, "normal")
        controle, mudancas, abertos = self.controle()
        self.assertEqual(Estado.PARADO, controle.estado)
        controle.iniciar().result(timeout=30)
        self.assertEqual(Estado.RODANDO, controle.estado)
        self.assertIn("Pronto", controle.mensagem)
        self.assertEqual([controle.url], abertos, "abre o navegador ao ficar pronto")
        controle.abrir_navegador()
        self.assertEqual(2, len(abertos))
        controle.parar().result(timeout=30)
        self.assertEqual(Estado.PARADO, controle.estado)
        self.assertEqual([Estado.INICIANDO, Estado.RODANDO, Estado.PARANDO, Estado.PARADO], mudancas)
        controle.iniciar().result(timeout=30)
        self.assertEqual(2, len(abertos), "na segunda vez não abre de novo sozinho")

    def test_falha_mostra_a_mensagem_e_deixa_tentar_de_novo(self):
        modo_falso(self, "banco_em_uso")
        controle, mudancas, abertos = self.controle()
        controle.iniciar().result(timeout=30)
        self.assertEqual(Estado.ERRO, controle.estado)
        self.assertIn("banco", controle.mensagem)
        self.assertEqual([], abertos)
        modo_falso(self, "normal")
        controle.iniciar().result(timeout=30)
        self.assertEqual(Estado.RODANDO, controle.estado)
