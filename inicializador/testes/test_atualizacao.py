"""Atualização pelas Releases do GitHub: só a linha 1.x, pacote completo e conferido, falhas tipadas e a versão
anterior preservada. Porta dos testes Go do iniciador antigo (403, 429, 500, tempo esgotado, release incompleta)."""
import json
import zipfile

from inpetu_iniciador.atualizador import Atualizador
from inpetu_iniciador.github import Releases
from inpetu_iniciador.instalacao import Instalacao
from inpetu_iniciador.pastas import Pastas
from inpetu_iniciador.registro import Registro
from testes.apoio import ComPasta
from testes.github_falso import GitHubFalso, pacote_zip

SESSAO = "0123456789abcdef0123456789abcdef"
REPO = "dono/repositorio"


class AtualizacaoTest(ComPasta):

    def setUp(self):
        super().setUp()
        self.github = GitHubFalso()
        self.addCleanup(self.github.fechar)
        self.pastas = Pastas.do_ambiente(self.ambiente)
        self.pastas.criar()
        self.registro = Registro(self.pastas, SESSAO)
        self.instalacao = Instalacao(self.pastas)

    def atualizador(self, tempo=5):
        releases = Releases(self.github.base, REPO, tempo=tempo, permitir_local=True)
        return Atualizador(self.pastas, self.registro, "Linux-x64", releases, self.instalacao)

    def eventos(self):
        return [json.loads(l) for l in self.registro.caminho.read_text(encoding="utf-8").splitlines()]

    def instalada(self, versao):
        """Uma versão já instalada (como se tivesse vindo de antes)."""
        zip_caminho = self.base / f"{versao}.zip"
        zip_caminho.write_bytes(pacote_zip(versao))
        return self.instalacao.instalar_zip(zip_caminho, versao)

    def test_instala_a_mais_nova_da_linha_1_e_preserva_a_anterior(self):
        anterior = self.instalada("1.0.0-alpha.1")
        self.instalacao.marcar_atual("1.0.0-alpha.1")
        self.github.publicar("1.0.0-alpha.2")
        self.github.publicar("1.0.0-alpha.3")
        self.github.releases.insert(0, {"tag_name": "v0.5.13", "draft": False, "prerelease": False, "assets": []})
        resultado = self.atualizador().atualizar()
        self.assertEqual("ATUALIZADO", resultado.tipo)
        self.assertEqual("1.0.0-alpha.3", str(resultado.versao))
        self.assertEqual("1.0.0-alpha.3", str(self.instalacao.atual()[0]))
        self.assertTrue(anterior.is_dir(), "a versão anterior continua lá")
        atual = self.instalacao.atual()[1]
        self.assertTrue((atual / "runtime" / "bin" / "java").stat().st_mode & 0o111, "java continua executável")
        self.assertIn("Atualizado para a versão 1.0.0-alpha.3", resultado.mensagem)
        self.assertIn("UPDATE_INSTALLED", [e["event"] for e in self.eventos()])

    def test_ja_na_mais_recente_nao_baixa_nada(self):
        self.instalada("1.0.0-alpha.2")
        self.instalacao.marcar_atual("1.0.0-alpha.2")
        self.github.publicar("1.0.0-alpha.2")
        resultado = self.atualizador().atualizar()
        self.assertEqual("JA_NA_ULTIMA", resultado.tipo)
        self.assertEqual("Você já está na versão mais recente (1.0.0-alpha.2).", resultado.mensagem)
        self.assertFalse(any("/download/" in p for p in self.github.pedidos))

    def test_release_incompleta_mais_nova_e_pulada(self):
        self.github.publicar("1.0.0-alpha.1")
        self.github.publicar("1.0.0-alpha.2", sums=False)
        self.github.publicar("1.0.0-alpha.3", estado="starter")
        self.github.publicar("1.0.0-alpha.4", plataforma="Linux-ARM64")
        self.github.publicar("1.0.0-alpha.5", rascunho=True)
        resultado = self.atualizador().atualizar()
        self.assertEqual("1.0.0-alpha.1", str(resultado.versao), "a mais nova completa para este sistema")
        incompletas = [e.get("result") for e in self.eventos() if e["event"] == "UPDATE_RELEASE_INCOMPLETE"]
        self.assertIn("MISSING_SUMS", incompletas)
        self.assertIn("MISSING_PACKAGE", incompletas)

    def test_sem_versao_da_linha_1_publicada(self):
        self.github.releases.append({"tag_name": "v0.5.12", "draft": False, "prerelease": False, "assets": []})
        resultado = self.atualizador().atualizar()
        self.assertEqual("SEM_VERSAO_COMPLETA", resultado.tipo)

    def test_falhas_da_consulta_sao_tipadas_sem_guardar_corpo_nem_endereco(self):
        casos = [(403, "RATE_LIMITED", "limitou"), (429, "RATE_LIMITED", "limitou"), (500, "HTTP_STATUS", "500")]
        for status, tipo, trecho in casos:
            self.github.status_api = status
            resultado = self.atualizador().atualizar()
            self.assertEqual("FALHA", resultado.tipo, status)
            self.assertEqual(tipo, resultado.falha, status)
            self.assertIn(trecho, resultado.mensagem)
        self.github.status_api = 200
        self.github.atraso_api = 2
        resultado = self.atualizador(tempo=0.5).atualizar()
        self.assertEqual("TIMEOUT", resultado.falha)
        self.github.fechar()
        resultado = self.atualizador().atualizar()
        self.assertEqual("NETWORK", resultado.falha)
        self.assertIn("Sem conexão", resultado.mensagem)
        texto = self.registro.caminho.read_text(encoding="utf-8")
        self.assertNotIn("segredo", texto)
        self.assertNotIn("127.0.0.1", texto)
        falhas = [(e.get("result"), e.get("status")) for e in self.eventos() if e["event"] == "UPDATE_CHECK_FAILED"]
        self.assertIn(("RATE_LIMITED", 403), falhas)

    def test_soma_errada_nao_instala_e_mantem_a_atual(self):
        self.instalada("1.0.0-alpha.1")
        self.instalacao.marcar_atual("1.0.0-alpha.1")
        self.github.publicar("1.0.0-alpha.2", soma_errada=True)
        resultado = self.atualizador().atualizar()
        self.assertEqual("FALHA", resultado.tipo)
        self.assertEqual("INTEGRITY", resultado.falha)
        self.assertEqual("1.0.0-alpha.1", str(self.instalacao.atual()[0]))
        self.assertEqual([], [p.name for p in self.pastas.versoes.iterdir() if "alpha.2" in p.name], "nada sobra")

    def test_so_enderecos_do_github_sao_aceitos(self):
        with self.assertRaises(ValueError):
            Releases("http://exemplo.com", REPO)
        Releases("https://api.github.com", REPO)


class InstalacaoTest(ComPasta):

    def setUp(self):
        super().setUp()
        self.pastas = Pastas.do_ambiente(self.ambiente)
        self.pastas.criar()
        self.instalacao = Instalacao(self.pastas)

    def zip(self, nome, conteudo):
        caminho = self.base / nome
        caminho.write_bytes(conteudo)
        return caminho

    def test_zip_com_caminho_perigoso_ou_link_e_recusado(self):
        perigosos = {"travessia": {"../fora.txt": "x"}, "absoluto": {"/etc/fora": "x"}}
        link = zipfile.ZipInfo("InPETU-Hub-Linux-x64-v1.0.0/runtime/link")
        link.external_attr = 0o120777 << 16
        perigosos["link"] = {"link": link}
        for nome, extras in perigosos.items():
            caminho = self.zip(f"{nome}.zip", pacote_zip("1.0.0", extras=extras))
            with self.assertRaises(Exception, msg=nome):
                self.instalacao.instalar_zip(caminho, "1.0.0")
            self.assertFalse((self.base / "fora.txt").exists())
            self.assertEqual([], list(self.pastas.versoes.iterdir()), f"{nome}: nada fica na pasta de versões")

    def test_bundle_de_outra_versao_ou_incompleto_e_recusado(self):
        outra = self.zip("outra.zip", pacote_zip("1.0.0", bundle={"versao": "1.0.1", "plataforma": "linux-x64"}))
        with self.assertRaises(Exception):
            self.instalacao.instalar_zip(outra, "1.0.0")
        sem_bundle = self.zip("sem.zip", pacote_zip("1.0.0", bundle={}))
        with self.assertRaises(Exception):
            self.instalacao.instalar_zip(sem_bundle, "1.0.0")

    def test_instala_da_pasta_do_executavel_quando_ela_e_mais_nova(self):
        pasta = self.base / "Pacote baixado"
        with zipfile.ZipFile(self.zip("p.zip", pacote_zip("1.0.0-alpha.4", pasta_raiz=False))) as z:
            z.extractall(pasta)
        (pasta / "runtime" / "bin" / "java").chmod(0o755)
        destino = self.instalacao.instalar_pasta(pasta)
        self.assertEqual("1.0.0-alpha.4", str(self.instalacao.versao_da_pasta(destino)))
        self.instalacao.marcar_atual("1.0.0-alpha.4")
        self.assertEqual(destino, self.instalacao.atual()[1])

    def test_guarda_as_tres_mais_novas_e_nunca_apaga_a_atual(self):
        for numero in range(1, 6):
            caminho = self.zip(f"{numero}.zip", pacote_zip(f"1.0.0-alpha.{numero}"))
            self.instalacao.instalar_zip(caminho, f"1.0.0-alpha.{numero}")
        self.instalacao.marcar_atual("1.0.0-alpha.1")
        self.instalacao.limpar_antigas(manter=3)
        restantes = sorted(p.name for p in self.pastas.versoes.iterdir() if p.is_dir())
        self.assertEqual(["1.0.0-alpha.1", "1.0.0-alpha.4", "1.0.0-alpha.5"], restantes)
