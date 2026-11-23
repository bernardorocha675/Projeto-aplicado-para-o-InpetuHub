"""Backup do banco antes de iniciar e log local sem dados pessoais."""
import datetime
import json

from inpetu_iniciador import backup
from inpetu_iniciador.pastas import Pastas
from inpetu_iniciador.registro import Registro
from testes.apoio import ComPasta

SESSAO = "0123456789abcdef0123456789abcdef"


class BackupTest(ComPasta):

    def setUp(self):
        super().setUp()
        self.pastas = Pastas.do_ambiente(self.ambiente)
        self.pastas.criar()

    def test_sem_banco_nao_ha_backup(self):
        self.assertIsNone(backup.copiar(self.pastas, datetime.datetime(2026, 11, 16, 10, 0, 0)))

    def test_copia_o_banco_com_data_e_hora_sem_mexer_no_original(self):
        banco = self.pastas.dados / "inpetu.mv.db"
        banco.write_bytes(b"banco de verdade")
        copia = backup.copiar(self.pastas, datetime.datetime(2026, 11, 16, 10, 5, 7))
        self.assertEqual(self.pastas.backups / "inpetu-20261116-100507.mv.db", copia)
        self.assertEqual(b"banco de verdade", copia.read_bytes())
        self.assertEqual(b"banco de verdade", banco.read_bytes())
        outra = backup.copiar(self.pastas, datetime.datetime(2026, 11, 16, 10, 5, 7))
        self.assertNotEqual(copia, outra, "no mesmo segundo, outro nome")

    def test_guarda_so_os_10_mais_novos(self):
        (self.pastas.dados / "inpetu.mv.db").write_bytes(b"x")
        (self.pastas.backups / "anotacao.txt").write_text("não é backup", encoding="utf-8")
        for minuto in range(12):
            backup.copiar(self.pastas, datetime.datetime(2026, 11, 16, 10, minuto, 0))
        nomes = sorted(p.name for p in self.pastas.backups.glob("inpetu-*.mv.db"))
        self.assertEqual(10, len(nomes))
        self.assertEqual("inpetu-20261116-100200.mv.db", nomes[0], "os dois mais antigos saem")
        self.assertTrue((self.pastas.backups / "anotacao.txt").exists(), "só apaga backups")


class RegistroTest(ComPasta):

    def test_uma_linha_json_por_evento_com_sessao_e_versao(self):
        pastas = Pastas.do_ambiente(self.ambiente)
        pastas.criar()
        registro = Registro(pastas, SESSAO)
        registro.evento("LAUNCHER_STARTED")
        registro.evento("SYSTEM_READY", port=8123, durationMs=4200)
        linhas = [json.loads(l) for l in registro.caminho.read_text(encoding="utf-8").splitlines()]
        self.assertEqual(["LAUNCHER_STARTED", "SYSTEM_READY"], [l["event"] for l in linhas])
        self.assertEqual(SESSAO, linhas[1]["sessionId"])
        self.assertEqual(8123, linhas[1]["port"])
        self.assertIn("launcherVersion", linhas[0])
        self.assertIn("time", linhas[0])

    def test_campos_fora_da_lista_e_textos_livres_nao_entram(self):
        pastas = Pastas.do_ambiente(self.ambiente)
        pastas.criar()
        registro = Registro(pastas, SESSAO)
        registro.evento("SYSTEM_START_FAILED", failure="DATABASE_IN_USE", caminho=str(pastas.raiz),
                        result="texto livre com /home/fulano")
        texto = registro.caminho.read_text(encoding="utf-8")
        self.assertIn('"failure": "DATABASE_IN_USE"', texto)
        for segredo in ("Fulano", "fulano", "caminho", "texto livre"):
            self.assertNotIn(segredo, texto)
        with self.assertRaises(ValueError):
            registro.evento("evento com espaço")
