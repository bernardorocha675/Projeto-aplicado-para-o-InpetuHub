"""O ponto de entrada: segunda cópia recusada e pacote incompleto avisado, sem abrir nada."""
import contextlib
import io
import json
import os
import unittest.mock

from inpetu_iniciador.__main__ import main
from inpetu_iniciador.pastas import Pastas
from inpetu_iniciador.trava import Trava
from testes.apoio import ComPasta


class PrincipalTest(ComPasta):

    def rodar(self, *argumentos):
        saida = io.StringIO()
        with unittest.mock.patch.dict(os.environ, self.ambiente), contextlib.redirect_stderr(saida), \
                unittest.mock.patch("inpetu_iniciador.__main__.avisar", lambda texto: print(texto, file=saida)):
            codigo = main(list(argumentos))
        return codigo, saida.getvalue()

    def test_segunda_copia_e_recusada_sem_mexer_no_log(self):
        pastas = Pastas.do_ambiente(self.ambiente)
        outra = Trava(pastas.trava)
        self.assertTrue(outra.adquirir())
        self.addCleanup(outra.liberar)
        codigo, saida = self.rodar("--pacote", str(self.base / "nada"), "--sem-janela")
        self.assertEqual(1, codigo)
        self.assertIn("já está aberto", saida)
        self.assertFalse((pastas.logs / "iniciador.log").exists())

    def test_pacote_incompleto_e_avisado_e_registrado(self):
        codigo, saida = self.rodar("--pacote", str(self.base / "pacote vazio"), "--sem-janela")
        self.assertEqual(1, codigo)
        self.assertIn("incompleto", saida)
        pastas = Pastas.do_ambiente(self.ambiente)
        eventos = [json.loads(l) for l in (pastas.logs / "iniciador.log").read_text(encoding="utf-8").splitlines()]
        self.assertEqual(["LAUNCHER_STARTED", "LAUNCHER_FAILED", "LAUNCHER_CLOSED"], [e["event"] for e in eventos])
        self.assertEqual("PACKAGE_INCOMPLETE", eventos[1]["failure"])
        self.assertTrue(Trava(pastas.trava).adquirir(), "a trava foi solta ao sair")
