"""Antes de subir: instala o pacote do executável se for mais novo, procura atualização e decide a passagem."""
import zipfile

from inpetu_iniciador.github import Releases
from inpetu_iniciador.instalacao import Instalacao
from inpetu_iniciador.pastas import Pastas
from inpetu_iniciador.preparo import preparar
from inpetu_iniciador.registro import Registro
from inpetu_iniciador.sistema import ErroDeSubida
from testes.apoio import ComPasta
from testes.github_falso import GitHubFalso, pacote_zip

SESSAO = "0123456789abcdef0123456789abcdef"


class PreparoTest(ComPasta):

    def setUp(self):
        super().setUp()
        self.github = GitHubFalso()
        self.addCleanup(self.github.fechar)
        self.pastas = Pastas.do_ambiente(self.ambiente)
        self.pastas.criar()
        self.registro = Registro(self.pastas, SESSAO)

    def executavel_baixado(self, versao):
        """A pasta que a pessoa extraiu do zip baixado, com o executável do iniciador."""
        pasta = self.base / f"Downloads de Fulano/InPETU-Hub {versao}"
        (self.base / "z.zip").write_bytes(pacote_zip(versao, pasta_raiz=False))
        with zipfile.ZipFile(self.base / "z.zip") as z:
            z.extractall(pasta)
        for nome in ("runtime/bin/java", "inpetu-hub"):
            (pasta / nome).chmod(0o755)
        return pasta

    def preparar(self, embutido, atualizar=True):
        releases = Releases(self.github.base, "dono/repo", tempo=5, permitir_local=True)
        return preparar(self.pastas, self.registro, embutido, releases if atualizar else None, [])

    def test_primeira_abertura_instala_o_pacote_do_executavel(self):
        preparo = self.preparar(self.executavel_baixado("1.0.0-alpha.1"))
        self.assertEqual(self.pastas.versoes / "1.0.0-alpha.1", preparo.pacote.java.parents[2])
        self.assertEqual("1.0.0-alpha.1", str(Instalacao(self.pastas).atual()[0]))
        self.assertIsNone(preparo.entregar)
        self.assertIn("Nenhuma versão completa", preparo.mensagem)

    def test_versao_nova_do_github_e_usada_e_o_iniciador_novo_recebe_a_passagem(self):
        self.github.publicar("1.0.0-alpha.2")
        preparo = self.preparar(self.executavel_baixado("1.0.0-alpha.1"))
        self.assertEqual("1.0.0-alpha.2", str(Instalacao(self.pastas).atual()[0]))
        self.assertEqual(self.pastas.versoes / "1.0.0-alpha.2" / "inpetu-hub", preparo.entregar)
        self.assertIn("Atualizado", preparo.mensagem)

    def test_sem_internet_usa_a_versao_instalada(self):
        self.preparar(self.executavel_baixado("1.0.0-alpha.1"))
        self.github.fechar()
        preparo = self.preparar(self.base / "Downloads de Fulano/InPETU-Hub 1.0.0-alpha.1")
        self.assertIn("Sem conexão", preparo.mensagem)
        self.assertEqual("1.0.0-alpha.1", str(Instalacao(self.pastas).atual()[0]))

    def test_executavel_mais_velho_que_o_instalado_nao_volta_a_versao(self):
        self.github.publicar("1.0.0-alpha.3")
        self.preparar(self.executavel_baixado("1.0.0-alpha.2"))
        preparo = self.preparar(self.base / "Downloads de Fulano/InPETU-Hub 1.0.0-alpha.2", atualizar=False)
        self.assertEqual("1.0.0-alpha.3", str(Instalacao(self.pastas).atual()[0]))
        self.assertIsNotNone(preparo.entregar, "o iniciador da 1.0.0-alpha.3 assume")

    def test_sem_nada_instalado_e_sem_pacote_e_erro_claro(self):
        with self.assertRaises(ErroDeSubida) as erro:
            self.preparar(self.base / "pasta vazia", atualizar=False)
        self.assertIn("pacote", str(erro.exception))
