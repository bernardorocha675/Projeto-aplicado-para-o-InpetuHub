"""Iniciador do InPETU Hub (linha 1.x): abre o sistema no computador, só em 127.0.0.1."""
import json
import pathlib
import sys


def _versao():
    """No pacote, a versão vem do bundle.json ao lado do executável; no desenvolvimento, 1.0.0-SNAPSHOT."""
    if getattr(sys, "frozen", False):
        try:
            dados = json.loads((pathlib.Path(sys.executable).parent / "bundle.json").read_text(encoding="utf-8"))
            return str(dados["versao"])
        except (OSError, ValueError, KeyError, TypeError):
            pass
    return "1.0.0-SNAPSHOT"


VERSAO = _versao()
