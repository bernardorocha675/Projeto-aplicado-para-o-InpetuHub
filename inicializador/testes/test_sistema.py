"""Sobe e para o sistema (aqui, o Java falso): porta livre, backup antes, parada em ordem e força só depois do prazo."""
import json
import os
import subprocess
import time
import urllib.request

from inpetu_iniciador.pastas import Pastas
from inpetu_iniciador.registro import Registro
from inpetu_iniciador.sistema import ErroDeSubida, Pacote, Sistema
from testes.apoio import ComPasta, modo_falso

SESSAO = "0123456789abcdef0123456789abcdef"


def saude(url):
    try:
        with urllib.request.urlopen(url + "saude", timeout=2) as resposta:
            return resposta.status
    except OSError:
        return 0


class SistemaTest(ComPasta):

    def setUp(self):
        super().setUp()
        self.pastas = Pastas.do_ambiente(self.ambiente)
        self.pastas.criar()
        self.registro = Registro(self.pastas, SESSAO)
        self.pacote = Pacote.em(self.pacote_falso())

    def sistema(self, prazo_parada=10):
        sistema = Sistema(self.pacote, self.pastas, self.registro, SESSAO, prazo_parada=prazo_parada,
                          prazo_subida=20)
        self.addCleanup(sistema.parar)
        return sistema

    def eventos(self):
        return [json.loads(l)["event"] for l in self.registro.caminho.read_text(encoding="utf-8").splitlines()]

    def test_sobe_com_os_argumentos_certos_e_para_em_ordem(self):
        argumentos = self.base / "args.json"
        modo_falso(self, "normal", argumentos)
        sistema = self.sistema()
        url = sistema.iniciar()
        self.assertRegex(url, r"^http://127\.0\.0\.1:\d+/$")
        self.assertEqual(200, saude(url))
        self.assertTrue(sistema.rodando)
        recebidos = json.loads(argumentos.read_text(encoding="utf-8"))
        self.assertIn(f"-Dinpetu.dados={self.pastas.dados}", recebidos, "caminho com espaço e acento inteiro")
        self.assertIn(f"-Dinpetu.parada={self.pastas.parada}", recebidos)
        self.assertIn(f"-Dinpetu.session.id={SESSAO}", recebidos)
        self.assertEqual(["-jar", str(self.pacote.jar)], recebidos[-3:-1])
        self.assertEqual(url.split(":")[-1].rstrip("/"), recebidos[-1])
        self.assertTrue(self.pastas.pid.exists())

        self.assertEqual("GRACEFUL", sistema.parar())
        self.assertFalse(sistema.rodando)
        self.assertEqual(0, saude(url))
        self.assertFalse(self.pastas.pid.exists())
        self.assertFalse(self.pastas.parada.exists())
        self.assertEqual("NOT_RUNNING", sistema.parar())
        self.assertIn("SYSTEM_READY", self.eventos())
        self.assertIn("SYSTEM_STOPPED", self.eventos())
        saida = (self.pastas.logs / "sistema.log").read_text(encoding="utf-8")
        self.assertIn("SERVER_READY", saida, "a saída do Java vai para o log local")

    def test_sem_atender_a_parada_e_encerrado_a_forca_depois_do_prazo(self):
        modo_falso(self, "ignora_parada")
        sistema = self.sistema(prazo_parada=1)
        sistema.iniciar()
        inicio = time.monotonic()
        self.assertEqual("FORCED", sistema.parar())
        self.assertGreaterEqual(time.monotonic() - inicio, 1)
        self.assertFalse(sistema.rodando)
        self.assertIn("SYSTEM_STOP_FORCED", self.eventos())

    def test_falha_ao_subir_vira_mensagem_e_registra_a_categoria(self):
        modo_falso(self, "banco_em_uso")
        sistema = self.sistema()
        with self.assertRaises(ErroDeSubida) as erro:
            sistema.iniciar()
        self.assertEqual("DATABASE_IN_USE", erro.exception.falha)
        self.assertIn("banco", str(erro.exception))
        self.assertFalse(sistema.rodando)
        self.assertIn("SYSTEM_START_FAILED", self.eventos())
        modo_falso(self, "falha")
        with self.assertRaises(ErroDeSubida) as outra:
            sistema.iniciar()
        self.assertEqual("OTHER", outra.exception.falha)

    def test_faz_backup_do_banco_antes_de_subir(self):
        modo_falso(self, "normal")
        (self.pastas.dados / "inpetu.mv.db").write_bytes(b"dados")
        self.sistema().iniciar()
        self.assertEqual(1, len(list(self.pastas.backups.glob("inpetu-*.mv.db"))))
        self.assertIn("DATABASE_BACKED_UP", self.eventos())

    def test_sistema_esquecido_por_uma_execucao_anterior_e_parado_antes(self):
        modo_falso(self, "normal")
        orfao = subprocess.Popen([str(self.pacote.java), f"-Dinpetu.dados={self.pastas.dados}",
                                  f"-Dinpetu.parada={self.pastas.parada}", "-jar", str(self.pacote.jar), "18999"],
                                 stderr=subprocess.DEVNULL)
        self.addCleanup(lambda: orfao.poll() is None and orfao.kill())
        self.pastas.pid.write_text(str(orfao.pid), encoding="utf-8")
        for _ in range(100):
            if saude("http://127.0.0.1:18999/") == 200:
                break
            time.sleep(0.1)
        self.sistema().iniciar()
        self.assertEqual(0, orfao.wait(timeout=10), "o esquecido saiu pelo arquivo de parada")
        self.assertIn("ORPHAN_STOPPED", self.eventos())

    def test_pid_de_outro_programa_nao_e_tocado(self):
        modo_falso(self, "normal")
        self.pastas.pid.write_text(str(os.getpid()), encoding="utf-8")
        sistema = self.sistema()
        sistema.iniciar()
        self.assertNotIn("ORPHAN_STOPPED", self.eventos())

    def test_pacote_incompleto_e_recusado(self):
        (self.pacote.jar).unlink()
        with self.assertRaises(ErroDeSubida):
            Pacote.em(self.pacote.jar.parent.parent)
