"""Apoio dos testes: pasta temporária com espaço e acento, pacote com o Java falso e um ambiente isolado."""
import os
import pathlib
import shutil
import sys
import tempfile
import unittest

AQUI = pathlib.Path(__file__).resolve().parent


class ComPasta(unittest.TestCase):
    """Cada teste ganha uma pasta própria, com espaço e acento no nome, como as do time."""

    def setUp(self):
        self.base = pathlib.Path(tempfile.mkdtemp(prefix="inpetu ação "))
        self.addCleanup(shutil.rmtree, self.base, ignore_errors=True)
        self.ambiente = {"XDG_CONFIG_HOME": str(self.base / "Configuração de Fulano")}

    def pacote_falso(self):
        """Uma pasta de pacote com runtime/bin/java (o Java falso) e app/inpetu-hub-sistema.jar (vazio)."""
        pacote = self.base / "Pacote InPETU"
        (pacote / "runtime" / "bin").mkdir(parents=True)
        (pacote / "app").mkdir()
        java = pacote / "runtime" / "bin" / "java"
        java.write_text(f"#!/bin/sh\nexec \"{sys.executable}\" \"{AQUI / 'java_falso.py'}\" \"$@\"\n", encoding="utf-8")
        java.chmod(0o755)
        (pacote / "app" / "inpetu-hub-sistema.jar").write_bytes(b"")
        return pacote


def modo_falso(teste, modo, args=None):
    """Liga um modo do Java falso só durante o teste."""
    anterior = {chave: os.environ.get(chave) for chave in ("FALSO_MODO", "FALSO_ARGS")}
    os.environ["FALSO_MODO"] = modo
    if args:
        os.environ["FALSO_ARGS"] = str(args)

    def restaurar():
        for chave, valor in anterior.items():
            if valor is None:
                os.environ.pop(chave, None)
            else:
                os.environ[chave] = valor

    teste.addCleanup(restaurar)
