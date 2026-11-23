"""O gerador do pacote: versão aceita, bundle.json com as somas, zip sem links e SHA256SUMS."""
import hashlib
import json
import pathlib
import shutil
import stat
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
import build_pacote  # noqa: E402


class BuildPacoteTest(unittest.TestCase):

    def setUp(self):
        self.base = pathlib.Path(tempfile.mkdtemp(prefix="pacote ação "))
        self.addCleanup(shutil.rmtree, self.base, ignore_errors=True)
        self.pasta = self.base / "montagem"
        for nome, conteudo, modo in [("inpetu-hub", b"#!/bin/sh\n", 0o755), ("runtime/bin/java", b"java", 0o755),
                                     ("runtime/lib/modules", b"m" * 100, 0o644),
                                     ("app/inpetu-hub-sistema.jar", b"jar", 0o644)]:
            arquivo = self.pasta / nome
            arquivo.parent.mkdir(parents=True, exist_ok=True)
            arquivo.write_bytes(conteudo)
            arquivo.chmod(modo)

    def test_so_versoes_da_linha_1(self):
        for boa in ("1.0.0-alpha.1", "1.0.0", "1.2.3-rc.2"):
            self.assertEqual(boa, build_pacote.validar_versao(boa))
        for ruim in ("v1.0.0", "1.0.0-SNAPSHOT", "0.6.0", "1.0", "1.0.0-alpha.1; rm -rf /"):
            with self.assertRaises(SystemExit, msg=ruim):
                build_pacote.validar_versao(ruim)

    def test_nome_do_pacote_segue_o_do_antigo(self):
        self.assertEqual("InPETU-Hub-Linux-x64-v1.0.0-alpha.1.zip",
                         build_pacote.nome_do_zip("Linux-x64", "1.0.0-alpha.1"))
        self.assertEqual("Linux-ARM64", build_pacote.plataforma("aarch64", "posix"))
        self.assertEqual("Linux-x64", build_pacote.plataforma("x86_64", "posix"))
        self.assertEqual("Windows-x64", build_pacote.plataforma("AMD64", "nt"))
        self.assertEqual("Windows-ARM64", build_pacote.plataforma("ARM64", "nt"))
        self.assertEqual("InPETU-Hub-Windows-x64-v1.0.0.zip", build_pacote.nome_do_zip("Windows-x64", "1.0.0"))

    def test_no_windows_o_bundle_aponta_para_os_exe(self):
        build_pacote.escrever_bundle(self.pasta, "1.0.0", "Windows-x64")
        dados = json.loads((self.pasta / "bundle.json").read_text(encoding="utf-8"))
        self.assertEqual(("windows-x64", "runtime/bin/java.exe", "inpetu-hub.exe"),
                         (dados["plataforma"], dados["java"], dados["iniciador"]))

    def test_opcoes_do_jlink_e_do_pyinstaller_por_sistema(self):
        self.assertIn("--strip-native-debug-symbols=exclude-debuginfo-files", build_pacote.opcoes_jlink("Linux-x64"))
        self.assertNotIn("--strip-native-debug-symbols=exclude-debuginfo-files",
                         build_pacote.opcoes_jlink("Windows-x64"), "só existe no jlink do Linux")
        self.assertIn("--windowed", build_pacote.opcoes_pyinstaller("Windows-ARM64"), "sem janela de console")
        self.assertNotIn("--windowed", build_pacote.opcoes_pyinstaller("Linux-x64"))
        self.assertEqual(pathlib.Path("v") / "Scripts" / "pip.exe", build_pacote.no_venv(pathlib.Path("v"), "pip", "nt"))
        self.assertEqual(pathlib.Path("v") / "bin" / "pip", build_pacote.no_venv(pathlib.Path("v"), "pip", "posix"))

    def test_bundle_tem_a_versao_os_caminhos_e_a_soma_de_cada_arquivo(self):
        build_pacote.escrever_bundle(self.pasta, "1.0.0-alpha.1", "Linux-x64")
        dados = json.loads((self.pasta / "bundle.json").read_text(encoding="utf-8"))
        self.assertEqual("1.0.0-alpha.1", dados["versao"])
        self.assertEqual("linux-x64", dados["plataforma"])
        self.assertEqual("runtime/bin/java", dados["java"])
        self.assertEqual("app/inpetu-hub-sistema.jar", dados["jar"])
        self.assertEqual(hashlib.sha256(b"jar").hexdigest(), dados["arquivos"]["app/inpetu-hub-sistema.jar"])
        self.assertNotIn("bundle.json", dados["arquivos"])
        self.assertEqual(4, len(dados["arquivos"]))

    @unittest.skipIf(sys.platform == "win32", "no Windows não há permissão de execução nem link sem privilégio")
    def test_zip_numa_pasta_com_as_permissoes_e_sem_links(self):
        build_pacote.escrever_bundle(self.pasta, "1.0.0-alpha.1", "Linux-x64")
        destino = build_pacote.zipar(self.pasta, self.base / "saida", "Linux-x64", "1.0.0-alpha.1")
        with zipfile.ZipFile(destino) as z:
            nomes = z.namelist()
            self.assertTrue(all(n.startswith("InPETU-Hub-Linux-x64-v1.0.0-alpha.1/") for n in nomes), nomes)
            java = z.getinfo("InPETU-Hub-Linux-x64-v1.0.0-alpha.1/runtime/bin/java")
            self.assertTrue((java.external_attr >> 16) & 0o111, "java executável")
            jar = z.getinfo("InPETU-Hub-Linux-x64-v1.0.0-alpha.1/app/inpetu-hub-sistema.jar")
            self.assertFalse((jar.external_attr >> 16) & 0o111)
        (self.pasta / "runtime" / "link").symlink_to("bin/java")
        with self.assertRaises(SystemExit):
            build_pacote.zipar(self.pasta, self.base / "outra", "Linux-x64", "1.0.0-alpha.1")

    @unittest.skipIf(sys.platform == "win32", "criar link no Windows exige privilégio")
    def test_links_viram_copias_antes_de_montar(self):
        (self.pasta / "runtime" / "legal").mkdir()
        (self.pasta / "runtime" / "legal" / "LICENSE").symlink_to(self.pasta / "runtime" / "bin" / "java")
        (self.pasta / "runtime" / "lib" / "pasta-link").symlink_to(self.pasta / "runtime" / "bin")
        build_pacote.materializar(self.pasta)
        licenca = self.pasta / "runtime" / "legal" / "LICENSE"
        self.assertFalse(licenca.is_symlink())
        self.assertEqual(b"java", licenca.read_bytes())
        self.assertTrue((self.pasta / "runtime" / "lib" / "pasta-link" / "java").is_file())
        build_pacote.escrever_bundle(self.pasta, "1.0.0-alpha.1", "Linux-x64")
        build_pacote.zipar(self.pasta, self.base / "saida", "Linux-x64", "1.0.0-alpha.1")
        (self.pasta / "quebrado").symlink_to(self.base / "nao-existe")
        with self.assertRaises(SystemExit):
            build_pacote.materializar(self.pasta)

    def test_mesmo_conteudo_da_o_mesmo_zip(self):
        build_pacote.escrever_bundle(self.pasta, "1.0.0-alpha.1", "Linux-x64")
        um = build_pacote.zipar(self.pasta, self.base / "um", "Linux-x64", "1.0.0-alpha.1").read_bytes()
        dois = build_pacote.zipar(self.pasta, self.base / "dois", "Linux-x64", "1.0.0-alpha.1").read_bytes()
        self.assertEqual(hashlib.sha256(um).hexdigest(), hashlib.sha256(dois).hexdigest())

    def test_sha256sums_acrescenta_ou_troca_a_linha_do_pacote(self):
        saida = self.base / "saida"
        saida.mkdir()
        a = saida / "InPETU-Hub-Linux-x64-v1.0.0-alpha.1.zip"
        b = saida / "InPETU-Hub-Linux-ARM64-v1.0.0-alpha.1.zip"
        a.write_bytes(b"a")
        b.write_bytes(b"b")
        build_pacote.atualizar_somas(saida, a)
        build_pacote.atualizar_somas(saida, b)
        a.write_bytes(b"a2")
        build_pacote.atualizar_somas(saida, a)
        linhas = (saida / "SHA256SUMS.txt").read_text(encoding="utf-8").splitlines()
        self.assertEqual([f"{hashlib.sha256(b'b').hexdigest()}  {b.name}", f"{hashlib.sha256(b'a2').hexdigest()}  {a.name}"],
                         sorted(linhas, key=lambda l: "ARM64" not in l))

    def test_modulos_do_java_incluem_os_que_o_jdeps_nao_ve(self):
        modulos = build_pacote.modulos("java.base,java.sql")
        for necessario in ("java.base", "java.sql", "jdk.zipfs", "jdk.unsupported", "jdk.localedata"):
            self.assertIn(necessario, modulos.split(","))
        self.assertIn("pt", build_pacote.IDIOMAS.split(","), "o pt-BR herda do pt: sem ele, R$ 1,600.00")


if __name__ == "__main__":
    unittest.main()
