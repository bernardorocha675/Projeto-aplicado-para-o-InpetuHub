"""Passagem para o iniciador de uma versão mais nova instalada (o executável baixado pela pessoa fica antigo).

A passagem troca o processo (exec): a trava do iniciador é solta junto, porque o arquivo dela fecha no exec.
"""
import os

EXECUTAVEL = "inpetu-hub"
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
    os.execv(str(executavel), [str(executavel), *argumentos, MARCA])
