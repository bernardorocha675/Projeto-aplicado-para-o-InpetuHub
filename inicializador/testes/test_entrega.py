"""Passagem para o iniciador mais novo: só para uma versão maior, uma vez, e com a trava solta antes."""
from inpetu_iniciador.entrega import precisa_entregar
from inpetu_iniciador.versoes import Versao
from testes.apoio import ComPasta


class EntregaTest(ComPasta):

    def pasta(self, com_executavel=True):
        pasta = self.base / "versao"
        pasta.mkdir()
        if com_executavel:
            (pasta / "inpetu-hub").write_text("#!/bin/sh\n", encoding="utf-8")
            (pasta / "inpetu-hub").chmod(0o755)
        return pasta

    def test_entrega_so_para_um_iniciador_mais_novo_e_uma_vez(self):
        pasta = self.pasta()
        propria = Versao.de("1.0.0-alpha.1")
        self.assertEqual(pasta / "inpetu-hub", precisa_entregar(propria, Versao.de("1.0.0-alpha.2"), pasta, []))
        self.assertIsNone(precisa_entregar(propria, Versao.de("1.0.0-alpha.1"), pasta, []), "mesma versão")
        self.assertIsNone(precisa_entregar(Versao.de("1.0.0-alpha.3"), Versao.de("1.0.0-alpha.2"), pasta, []))
        self.assertIsNone(precisa_entregar(propria, Versao.de("1.0.0-alpha.2"), pasta, ["--entregue"]),
                          "quem já recebeu a passagem não passa de novo (sem laço)")

    def test_sem_executavel_nao_entrega(self):
        self.assertIsNone(precisa_entregar(Versao.de("1.0.0-alpha.1"), Versao.de("1.0.0-alpha.2"),
                                           self.pasta(com_executavel=False), []))
