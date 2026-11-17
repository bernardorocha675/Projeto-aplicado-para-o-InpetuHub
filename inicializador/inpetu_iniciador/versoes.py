"""Versões da linha 1.x (decisão 9 do plano): 1.2.3 ou 1.2.3-alpha.N, -beta.N, -rc.N. Outras linhas são ignoradas."""
import dataclasses
import re

FORMATO = re.compile(r"v?1\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-(alpha|beta|rc)\.([1-9]\d*))?")
PESO = {"alpha": 0, "beta": 1, "rc": 2, None: 3}


@dataclasses.dataclass(frozen=True, order=True)
class Versao:
    chave: tuple = dataclasses.field(repr=False)
    texto: str = dataclasses.field(compare=False)

    @staticmethod
    def de(texto):
        """A versão, ou None se o texto não for da linha 1.x."""
        m = FORMATO.fullmatch(texto or "")
        if m is None:
            return None
        menor, correcao, etapa, numero = m.groups()
        chave = (1, int(menor), int(correcao), PESO[etapa], int(numero or 0))
        return Versao(chave, texto.removeprefix("v"))

    def __str__(self):
        return self.texto
