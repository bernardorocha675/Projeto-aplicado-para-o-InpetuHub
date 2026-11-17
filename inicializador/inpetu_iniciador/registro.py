"""Log local do iniciador: uma linha JSON por evento, só com campos da lista e valores curtos.

Nunca entram caminhos (a pasta da pessoa tem o nome dela), formulários, senhas nem mensagens de erro.
"""
import datetime
import json
import re

from inpetu_iniciador import VERSAO

CAMPOS = {"port", "durationMs", "exitCode", "result", "failure", "count", "attempt", "status"}
PALAVRA = re.compile(r"[A-Z][A-Z_]{0,39}")
TAMANHO_MAXIMO = 1_000_000


class Registro:

    def __init__(self, pastas, sessao):
        self.caminho = pastas.logs / "iniciador.log"
        self.sessao = sessao
        self.caminho.parent.mkdir(parents=True, exist_ok=True)
        girar(self.caminho)

    def evento(self, nome, **campos):
        if not PALAVRA.fullmatch(nome):
            raise ValueError("nome de evento fora do formato")
        linha = {"time": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="milliseconds"),
                 "launcherVersion": VERSAO, "sessionId": self.sessao, "event": nome}
        for chave, valor in campos.items():
            if chave not in CAMPOS:
                continue
            if isinstance(valor, bool):
                continue
            if isinstance(valor, int) and 0 <= valor <= 10**9:
                linha[chave] = valor
            elif isinstance(valor, str) and PALAVRA.fullmatch(valor):
                linha[chave] = valor
        with open(self.caminho, "a", encoding="utf-8") as arquivo:
            arquivo.write(json.dumps(linha, ensure_ascii=False) + "\n")


def girar(caminho, copias=3):
    """Ao abrir, um log grande vira .1 (o .1 vira .2...), para a pasta não crescer sem fim."""
    if not caminho.exists() or caminho.stat().st_size < TAMANHO_MAXIMO:
        return
    caminho.with_name(f"{caminho.name}.{copias}").unlink(missing_ok=True)
    for numero in range(copias - 1, 0, -1):
        antigo = caminho.with_name(f"{caminho.name}.{numero}")
        if antigo.exists():
            antigo.rename(caminho.with_name(f"{caminho.name}.{numero + 1}"))
    caminho.rename(caminho.with_name(f"{caminho.name}.1"))
