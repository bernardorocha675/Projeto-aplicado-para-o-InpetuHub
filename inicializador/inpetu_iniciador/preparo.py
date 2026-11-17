"""O que acontece antes de subir o sistema: a versão certa instalada e a passagem para um iniciador mais novo."""
import dataclasses
import pathlib

from inpetu_iniciador.atualizador import Atualizador, plataforma
from inpetu_iniciador.entrega import precisa_entregar
from inpetu_iniciador.instalacao import Instalacao, PacoteInvalido
from inpetu_iniciador.sistema import ErroDeSubida, Pacote


@dataclasses.dataclass(frozen=True)
class Preparado:
    pacote: Pacote
    mensagem: str
    entregar: pathlib.Path = None


def preparar(pastas, registro, embutido, releases, argumentos, sistema_operacional=None):
    """`embutido`: a pasta do executável aberto (com bundle.json). `releases`: None para não consultar o GitHub."""
    instalacao = Instalacao(pastas)
    propria = instalacao.versao_da_pasta(embutido) if embutido else None
    atual = instalacao.atual()
    if propria is not None and (atual is None or propria > atual[0]):
        try:
            instalacao.instalar_pasta(embutido)
        except (PacoteInvalido, OSError) as erro:
            registro.evento("LAUNCHER_FAILED", failure="PACKAGE_INCOMPLETE")
            raise ErroDeSubida("O pacote do sistema está incompleto. Baixe o pacote de novo.",
                               "PACKAGE_INCOMPLETE") from erro
        instalacao.marcar_atual(propria)
        registro.evento("EMBEDDED_INSTALLED")
    mensagem = ""
    if releases is not None:
        atualizador = Atualizador(pastas, registro, sistema_operacional or plataforma(), releases, instalacao)
        mensagem = atualizador.atualizar().mensagem
    atual = instalacao.atual()
    if atual is None:
        registro.evento("LAUNCHER_FAILED", failure="NOT_INSTALLED")
        raise ErroDeSubida("O pacote do sistema não foi encontrado. Baixe o pacote de novo.", "NOT_INSTALLED")
    versao, pasta = atual
    return Preparado(Pacote.em(pasta), mensagem, precisa_entregar(propria, versao, pasta, argumentos))
