"""O iniciador com o jar de verdade (só quando INPETU_JAR aponta para ele e há um java no PATH, como na CI)."""
import os
import pathlib
import shutil
import unittest
import urllib.request

from inpetu_iniciador.pastas import Pastas
from inpetu_iniciador.registro import Registro
from inpetu_iniciador.sistema import Pacote, Sistema
from testes.apoio import ComPasta

SESSAO = "fedcba9876543210fedcba9876543210"
JAR = os.environ.get("INPETU_JAR")


def status(url):
    try:
        with urllib.request.urlopen(url, timeout=5) as resposta:
            return resposta.status
    except OSError:
        return 0


@unittest.skipUnless(JAR and shutil.which("java"), "sem INPETU_JAR ou sem java no PATH")
class SistemaRealTest(ComPasta):

    def test_sobe_responde_para_em_ordem_e_sobe_de_novo_com_backup(self):
        pacote = self.base / "Pacote Ação"
        (pacote / "runtime" / "bin").mkdir(parents=True)
        (pacote / "app").mkdir()
        (pacote / "runtime" / "bin" / "java").symlink_to(shutil.which("java"))
        shutil.copy(pathlib.Path(JAR), pacote / "app" / "inpetu-hub-sistema.jar")
        pastas = Pastas.do_ambiente(self.ambiente)
        pastas.criar()
        sistema = Sistema(Pacote.em(pacote), pastas, Registro(pastas, SESSAO), SESSAO, prazo_parada=30,
                          prazo_subida=120)
        self.addCleanup(sistema.parar)
        for vez in (1, 2):
            url = sistema.iniciar()
            self.assertEqual(200, status(url + "agenda"), f"vez {vez}")
            self.assertEqual("GRACEFUL", sistema.parar(), f"vez {vez}: encerrou pelo arquivo de parada")
        self.assertTrue((pastas.dados / "inpetu.mv.db").exists())
        self.assertEqual(1, len(list(pastas.backups.glob("inpetu-*.mv.db"))), "backup antes da segunda subida")
        log = (pastas.logs / "sistema.log").read_text(encoding="utf-8")
        self.assertEqual(2, log.count("event=SERVER_STOPPED"), "o sistema registrou as duas paradas")
        self.assertIn(f'"sessionId":"{SESSAO}"', log, "a sessão do iniciador vai para o log do sistema")
        self.assertNotIn(str(self.base), log, "o log não guarda a pasta da pessoa")
