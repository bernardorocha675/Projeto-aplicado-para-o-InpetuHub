"""Onde ficam os dados do sistema novo: InPETU-Hub/1 na pasta de configuração da pessoa (no Linux, ~/.config; no
Windows, %LOCALAPPDATA%, a mesma base do sistema antigo).

Nunca usa InPETU-Hub/0.4, a pasta do sistema antigo (decisão 4 do plano: começar limpo).
"""
import dataclasses
import os
import pathlib

from inpetu_iniciador import so


@dataclasses.dataclass(frozen=True)
class Pastas:
    raiz: pathlib.Path

    @staticmethod
    def do_ambiente(ambiente=None, nome_do_so=None):
        ambiente = os.environ if ambiente is None else ambiente
        return Pastas(so.pasta_de_configuracao(ambiente, nome_do_so) / "InPETU-Hub" / "1")

    @property
    def dados(self):
        return self.raiz / "dados"

    @property
    def backups(self):
        return self.raiz / "backups"

    @property
    def logs(self):
        return self.raiz / "logs"

    @property
    def versoes(self):
        return self.raiz / "versoes"

    @property
    def trava(self):
        return self.raiz / "iniciador.trava"

    @property
    def parada(self):
        return self.raiz / "sistema.parar"

    @property
    def pid(self):
        return self.raiz / "sistema.pid"

    def criar(self):
        for pasta in (self.dados, self.backups, self.logs, self.versoes):
            pasta.mkdir(parents=True, exist_ok=True)
