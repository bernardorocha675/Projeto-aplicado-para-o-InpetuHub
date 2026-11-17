"""Passagem para o iniciador de uma versão mais nova instalada (o executável baixado pela pessoa fica antigo).

No Linux a passagem troca o processo (exec); no Windows abre o novo e encerra este. A trava é solta antes.
"""
import os

from inpetu_iniciador import so

EXECUTAVEL = so.INICIADOR
MARCA = "--entregue"


def precisa_entregar(propria, instalada, pasta, argumentos):
    """O executável para quem passar, ou None. Só para uma versão maior e uma vez só (sem laço)."""
    if MARCA in argumentos or instalada is None or propria is None or instalada <= propria:
        return None
    executavel = pasta / EXECUTAVEL
    if not executavel.is_file() or not os.access(executavel, os.X_OK):
        return None
    return executavel


def entregar(executavel, argumentos):
    so.trocar_processo(executavel, [*argumentos, MARCA])
