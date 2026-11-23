"""O que muda entre Linux e Windows: nome do pacote, pasta de dados, nomes dos executáveis e a passagem."""
import pathlib
import unittest

from inpetu_iniciador import so
from inpetu_iniciador.pastas import Pastas


class SistemaOperacionalTest(unittest.TestCase):

    def test_nome_do_pacote_de_cada_sistema(self):
        self.assertEqual("Linux-x64", so.plataforma("x86_64", "posix"))
        self.assertEqual("Linux-ARM64", so.plataforma("aarch64", "posix"))
        self.assertEqual("Windows-x64", so.plataforma("AMD64", "nt"))
        self.assertEqual("Windows-ARM64", so.plataforma("ARM64", "nt"))

    def test_executaveis_com_exe_so_no_windows(self):
        self.assertEqual(("runtime/bin/java.exe", "inpetu-hub.exe"), so.nomes("nt"))
        self.assertEqual(("runtime/bin/java", "inpetu-hub"), so.nomes("posix"))
        self.assertEqual(so.nomes(), (so.JAVA, so.INICIADOR))

    def test_no_windows_os_dados_ficam_no_localappdata_como_no_antigo(self):
        ambiente = {"LOCALAPPDATA": r"C:\Users\Fulano\AppData\Local", "APPDATA": r"C:\Users\Fulano\AppData\Roaming",
                    "XDG_CONFIG_HOME": "/nao/usar"}
        self.assertEqual(pathlib.Path(r"C:\Users\Fulano\AppData\Local") / "InPETU-Hub" / "1",
                         Pastas.do_ambiente(ambiente, "nt").raiz)
        self.assertEqual(pathlib.Path(r"C:\Users\Fulano\AppData\Roaming") / "InPETU-Hub" / "1",
                         Pastas.do_ambiente({"APPDATA": r"C:\Users\Fulano\AppData\Roaming"}, "nt").raiz)
        self.assertEqual(pathlib.Path("/home/fulano/.config/InPETU-Hub/1"),
                         Pastas.do_ambiente({"HOME": "/home/fulano", "LOCALAPPDATA": "x"}, "posix").raiz)

    def test_passagem_no_windows_abre_o_novo_e_encerra_este(self):
        chamadas = []
        so.trocar_processo(pathlib.Path("C:/v/inpetu-hub.exe"), ["--pacote", "P"], "nt",
                           abrir=lambda comando, **opcoes: chamadas.append(("abrir", comando)),
                           sair=lambda codigo: chamadas.append(("sair", codigo)))
        self.assertEqual([("abrir", [str(pathlib.Path("C:/v/inpetu-hub.exe")), "--pacote", "P"]), ("sair", 0)],
                         chamadas)

    def test_sessao_continua_so_na_passagem_e_no_formato(self):
        from inpetu_iniciador import entrega
        sessao = "0123456789abcdef0123456789abcdef"
        self.assertEqual(sessao, entrega.sessao([entrega.MARCA], {entrega.SESSAO: sessao}))
        for argumentos, valor in (([], sessao), ([entrega.MARCA], "/home/fulano"), ([entrega.MARCA], "")):
            nova = entrega.sessao(argumentos, {entrega.SESSAO: valor})
            self.assertRegex(nova, "^[0-9a-f]{32}$")
            self.assertNotEqual(sessao, nova)

    def test_passagem_no_linux_troca_o_processo(self):
        chamadas = []
        executavel = pathlib.Path("/v/inpetu-hub")
        so.trocar_processo(executavel, ["-x"], "posix",
                           trocar=lambda caminho, argv: chamadas.append((caminho, argv)))
        self.assertEqual([(str(executavel), [str(executavel), "-x"])], chamadas)


if __name__ == "__main__":
    unittest.main()
