"""Pasta de dados InPETU-Hub/1, instância única e porta livre só em 127.0.0.1."""
import pathlib
import socket
import unittest

from inpetu_iniciador import porta
from inpetu_iniciador.pastas import Pastas
from inpetu_iniciador.trava import Trava
from testes.apoio import ComPasta


class PastasTest(ComPasta):

    def test_pasta_fica_na_configuracao_do_usuario_e_aceita_espaco_e_acento(self):
        pastas = Pastas.do_ambiente(self.ambiente)
        self.assertEqual(self.base / "Configuração de Fulano" / "InPETU-Hub" / "1", pastas.raiz)
        pastas.criar()
        for pasta in (pastas.dados, pastas.backups, pastas.logs):
            self.assertTrue(pasta.is_dir(), pasta)
        self.assertEqual(pastas.raiz / "sistema.parar", pastas.parada)

    def test_sem_xdg_usa_a_pasta_config_da_pessoa(self):
        pastas = Pastas.do_ambiente({"HOME": str(self.base)})
        self.assertEqual(self.base / ".config" / "InPETU-Hub" / "1", pastas.raiz)

    def test_nunca_usa_a_pasta_do_sistema_antigo(self):
        self.assertNotIn("0.4", str(Pastas.do_ambiente(self.ambiente).raiz))


class TravaTest(ComPasta):

    def test_so_uma_instancia_por_vez_e_a_trava_solta_ao_fechar(self):
        caminho = self.base / "iniciador.trava"
        primeira = Trava(caminho)
        segunda = Trava(caminho)
        self.assertTrue(primeira.adquirir())
        self.assertFalse(segunda.adquirir(), "outro iniciador já está aberto")
        primeira.liberar()
        self.assertTrue(segunda.adquirir())
        segunda.liberar()


class PortaTest(unittest.TestCase):

    def test_porta_livre_em_127_0_0_1(self):
        numero = porta.livre()
        self.assertGreater(numero, 1024)
        with socket.socket() as s:
            s.bind(("127.0.0.1", numero))


if __name__ == "__main__":
    unittest.main()
