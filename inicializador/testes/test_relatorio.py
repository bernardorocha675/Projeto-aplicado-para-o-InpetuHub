"""O relatório de teste: linha do tempo das últimas sessões, só com o que está no formato esperado."""
import json
import unittest

from inpetu_iniciador import relatorio
from inpetu_iniciador.pastas import Pastas
from inpetu_iniciador.registro import Registro
from testes.apoio import ComPasta

A, B = "a" * 32, "b" * 32


def linha(hora, mensagem, sessao=A, **extra):
    return json.dumps({"time": f"2026-11-23T{hora}.000Z", "appVersion": "1.0.0-alpha.2", "launcherVersion":
                       "1.0.0-alpha.2", "sessionId": sessao, "level": "INFO", "logger": "x", "requestId": "-",
                       "message": mensagem, **extra})


class RelatorioTest(ComPasta):

    def setUp(self):
        super().setUp()
        self.pastas = Pastas.do_ambiente(self.ambiente)
        self.pastas.criar()

    def escrever(self, *linhas, nome="sistema.log"):
        (self.pastas.logs / nome).write_text("".join(l + "\n" for l in linhas), encoding="utf-8")

    def test_junta_os_dois_logs_em_ordem_e_conta_os_erros(self):
        Registro(self.pastas, A).evento("LAUNCHER_STARTED")
        self.escrever(linha("14:00:02", "event=HTTP method=GET route=/agenda status=200 durationMs=12"),
                      linha("14:00:01", "event=SCREEN type=CLIQUE target=DIA page=/agenda"),
                      linha("14:00:03", "event=REQUEST_FAILED method=POST route=/admin/reservas/{id}/aprovar status=500",
                            level="ERROR", exceptionType="java.lang.IllegalStateException",
                            rootFrames=["br.com.inpetu.hub.reserva.Reserva.aprovar:42"], failure="OTHER",
                            requestId="12345678-1234-1234-1234-123456789abc"))
        texto = relatorio.gerar(self.pastas, A)
        self.assertIn("sistema 1.0.0-alpha.2", texto)
        self.assertLess(texto.index("SCREEN"), texto.index("route=/agenda status=200"))
        self.assertIn("LAUNCHER_STARTED", texto)
        self.assertIn("Erros: 1", texto)
        self.assertIn("IllegalStateException", texto)
        self.assertIn("Reserva.aprovar:42", texto)
        self.assertIn("12345678-1234-1234-1234-123456789abc", texto)

    def test_so_copia_o_que_esta_no_formato(self):
        self.escrever("Exception in thread main /home/Fulano/x", "{quebrado Fulano",
                      linha("14:00:01", "event=HTTP method=GET route=/home/Fulano status=200"),
                      linha("14:00:02", "nome=Fulano Sigiloso"),
                      linha("14:00:03", "event=AUDIT action=LOGIN_SENHA target=autenticacao result=OK senha=Fulano"),
                      linha("14:00:04", "event=SCREEN type=CLIQUE target=Fulano page=/agenda"),
                      linha("14:00:05", "event=TASK_FAILED task=thread", exceptionType="/home/Fulano",
                            rootFrames=["Fulano Sigiloso"], failure="Fulano"),
                      json.dumps({"time": "Fulano", "sessionId": A, "message": "event=SERVER_READY"}),
                      json.dumps(["Fulano"]),
                      linha("14:00:06", "event=SERVER_READY"))
        (self.pastas.logs / "iniciador.log").write_text(
            json.dumps({"time": "2026-11-23T14:00:07.000+00:00", "sessionId": A, "event": "Fulano"}) + "\n" +
            json.dumps({"time": "2026-11-23T14:00:08.000+00:00", "sessionId": A, "event": "SYSTEM_READY",
                        "result": "Fulano", "port": 1234}) + "\n", encoding="utf-8")
        texto = relatorio.gerar(self.pastas, A)
        self.assertNotIn("Fulano", texto)
        self.assertNotIn("Sigiloso", texto)
        self.assertIn("event=SERVER_READY", texto)
        self.assertIn("SYSTEM_READY", texto)

    def test_hora_sem_fracao_fica_na_ordem_certa(self):
        self.escrever(linha("14:00:01", "event=SERVER_READY").replace("01.000Z", "01.500Z"),
                      linha("14:00:01", "event=SERVER_STOPPED").replace("01.000Z", "01Z"))
        texto = relatorio.gerar(self.pastas, A)
        self.assertLess(texto.index("SERVER_STOPPED"), texto.index("SERVER_READY"))

    def test_linhas_internas_conhecidas_nao_contam_como_fora_do_formato(self):
        self.escrever("InPETU Hub pronto em http://127.0.0.1:1234/", linha("14:00:01", "FRAMEWORK_EVENT"),
                      linha("14:00:02", "event=SERVER_READY"))
        self.assertIn("Linhas fora do formato: 0", relatorio.gerar(self.pastas, A))

    def test_so_as_cinco_sessoes_mais_novas_incluindo_logs_girados(self):
        sessoes = [f"{i:032x}" for i in range(7)]
        self.escrever(*[linha(f"0{i}:00:00", "event=SERVER_READY", sessao=s) for i, s in enumerate(sessoes[:4])],
                      nome="sistema.log.1")
        self.escrever(*[linha(f"0{i}:00:00", "event=SERVER_READY", sessao=s)
                        for i, s in enumerate(sessoes[4:], start=4)])
        texto = relatorio.gerar(self.pastas, sessoes[-1])
        for sessao in sessoes[2:]:
            self.assertIn(sessao[-8:], texto)
        self.assertNotIn(f"Sessão {sessoes[1][-8:]}", texto)

    def test_salvar_grava_o_arquivo_e_registra(self):
        registro = Registro(self.pastas, A)
        registro.evento("LAUNCHER_STARTED")
        destino = self.base / "Meus Documentos" / "relatório.txt"
        destino.parent.mkdir()
        self.assertTrue(relatorio.salvar(self.pastas, registro, destino))
        self.assertIn("LAUNCHER_STARTED", destino.read_text(encoding="utf-8"))
        self.assertIn("REPORT_SAVED", registro.caminho.read_text(encoding="utf-8"))
        self.assertFalse(relatorio.salvar(self.pastas, registro, self.base / "nao-existe" / "r.txt"))

    def test_nome_sugerido_tem_so_versao_e_data(self):
        self.assertRegex(relatorio.nome_sugerido(), r"^InPETU-relatorio-v[0-9A-Za-z.\-]+-\d{8}-\d{4}\.txt$")

    def test_listas_acompanham_as_do_sistema(self):
        import pathlib
        import re
        java = pathlib.Path(__file__).resolve().parents[2] / "sistema/src/main/java/br/com/inpetu/hub/comum"
        rotas = (java / "log/RotasSeguras.java").read_text(encoding="utf-8")
        rotas = set(re.findall('"([^"]+)"', re.search(r"CONHECIDAS = Set\.of\((.*?)\);", rotas, re.S).group(1)))
        self.assertEqual(set(), rotas - relatorio.ROTAS)
        tela = (java / "TelaServlet.java").read_text(encoding="utf-8")
        for nome in ("CAMPOS", "SCRIPTS", "CLIQUES", "FALHAS"):
            for codigo in re.findall('"([^"]+)"', re.search(nome + r" = Set\.of\((.*?)\);", tela, re.S).group(1)):
                self.assertTrue(relatorio._alvo(codigo), codigo)

    def test_sem_logs_ainda_gera(self):
        self.assertIn("Erros: 0", relatorio.gerar(self.pastas, B))


if __name__ == "__main__":
    unittest.main()
