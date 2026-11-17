"""Versões da linha 1.x: ordem com pré-releases e recusa das outras linhas."""
import unittest

from inpetu_iniciador.versoes import Versao


class VersoesTest(unittest.TestCase):

    def test_ordem_com_pre_releases(self):
        ordem = ["1.0.0-alpha.1", "1.0.0-alpha.2", "1.0.0-alpha.10", "1.0.0-beta.1", "1.0.0-rc.1", "1.0.0", "1.0.1",
                 "1.2.0", "1.10.0"]
        versoes = [Versao.de(v) for v in ordem]
        self.assertEqual(versoes, sorted(reversed(versoes)))
        self.assertEqual("1.0.0-alpha.2", str(Versao.de("v1.0.0-alpha.2")))

    def test_so_a_linha_1_e_aceita(self):
        for texto in ("v0.5.12", "2.0.0", "1.0", "1.0.0-SNAPSHOT", "1.0.0-gamma.1", "v1.0.0 ", "1.0.0-alpha", ""):
            self.assertIsNone(Versao.de(texto), texto)
        self.assertIsNotNone(Versao.de("1.0.0"))
