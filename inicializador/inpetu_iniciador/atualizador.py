"""Procura e instala a versão mais nova da linha 1.x para este sistema (pacote + SHA256SUMS conferido).

Sem internet ou com o GitHub fora, o sistema sobe com a versão instalada; a falha vai para o log só com o tipo.
"""
import dataclasses
import hashlib
import tempfile

from inpetu_iniciador import so
from inpetu_iniciador.github import ErroDeAtualizacao
from inpetu_iniciador.instalacao import PacoteInvalido
from inpetu_iniciador.versoes import Versao

LIMITE_PACOTE = 600_000_000
LIMITE_SOMAS = 64_000
MENSAGENS = {
    "RATE_LIMITED": "O GitHub limitou as consultas por agora. Usando a versão instalada; tente de novo mais tarde.",
    "TIMEOUT": "A consulta de atualizações demorou demais. Usando a versão instalada.",
    "NETWORK": "Sem conexão para procurar atualizações. Usando a versão instalada.",
    "INVALID_RESPONSE": "A resposta do GitHub veio incompleta. Usando a versão instalada.",
    "INTEGRITY": "O pacote baixado não confere com o SHA256SUMS. Usando a versão instalada.",
    "PACKAGE_INVALID": "O pacote baixado está incompleto. Usando a versão instalada.",
}


@dataclasses.dataclass(frozen=True)
class Resultado:
    tipo: str
    mensagem: str
    versao: Versao = None
    falha: str = None


def plataforma():
    """Linux-x64, Linux-ARM64, Windows-x64 ou Windows-ARM64, como no nome dos pacotes."""
    return so.plataforma()


class Atualizador:

    def __init__(self, pastas, registro, sistema_operacional, releases, instalacao):
        self.pastas = pastas
        self.registro = registro
        self.sistema_operacional = sistema_operacional
        self.releases = releases
        self.instalacao = instalacao

    def atualizar(self):
        atual = self.instalacao.atual()
        versao_atual = atual[0] if atual else None
        try:
            escolhida = self._escolher()
            if escolhida is None:
                return Resultado("SEM_VERSAO_COMPLETA", "Nenhuma versão completa para este sistema foi publicada.")
            versao, pacote, somas = escolhida
            if versao_atual is not None and versao <= versao_atual:
                return Resultado("JA_NA_ULTIMA", f"Você já está na versão mais recente ({versao_atual}).", versao_atual)
            self._instalar(versao, pacote, somas)
        except ErroDeAtualizacao as erro:
            campos = {"result": erro.tipo}
            if erro.status is not None:
                campos["status"] = erro.status
            self.registro.evento("UPDATE_CHECK_FAILED", **campos)
            mensagem = MENSAGENS.get(erro.tipo) or f"O GitHub respondeu com erro (status {erro.status}). Usando a" \
                " versão instalada."
            return Resultado("FALHA", mensagem, versao_atual, erro.tipo)
        self.instalacao.marcar_atual(versao)
        self.instalacao.limpar_antigas()
        self.registro.evento("UPDATE_INSTALLED")
        return Resultado("ATUALIZADO", f"Atualizado para a versão {versao}.", versao)

    def _escolher(self):
        # O GitHub lista por data; uma correção da 1.0 pode sair depois da 1.1. Vale a maior versão.
        candidatas = [(Versao.de(r.get("tag_name", "")), r) for r in self.releases.listar() if not r.get("draft")]
        candidatas = [c for c in candidatas if c[0] is not None]
        for versao, release in sorted(candidatas, key=lambda c: c[0], reverse=True):
            nome_pacote = f"InPETU-Hub-{self.sistema_operacional}-v{versao}.zip"
            ativos = {a.get("name"): a for a in release.get("assets", []) if isinstance(a, dict)}
            pacote, somas = ativos.get(nome_pacote), ativos.get("SHA256SUMS.txt")
            if pacote is None or pacote.get("state") != "uploaded":
                self.registro.evento("UPDATE_RELEASE_INCOMPLETE", result="MISSING_PACKAGE")
                continue
            if somas is None or somas.get("state") != "uploaded":
                self.registro.evento("UPDATE_RELEASE_INCOMPLETE", result="MISSING_SUMS")
                continue
            return versao, pacote, somas
        return None

    def _instalar(self, versao, pacote, somas):
        texto = self.releases.ler(somas["browser_download_url"], LIMITE_SOMAS).decode("utf-8", "replace")
        esperada = None
        for linha in texto.splitlines():
            partes = linha.split()
            if len(partes) == 2 and partes[1].lstrip("*") == pacote["name"]:
                esperada = partes[0].lower()
        if esperada is None:
            raise ErroDeAtualizacao("INTEGRITY")
        with tempfile.TemporaryDirectory(dir=self.pastas.raiz) as pasta:
            arquivo = f"{pasta}/pacote.zip"
            self.releases.baixar(pacote["browser_download_url"], arquivo, LIMITE_PACOTE)
            soma = hashlib.sha256()
            with open(arquivo, "rb") as f:
                for bloco in iter(lambda: f.read(1 << 20), b""):
                    soma.update(bloco)
            if soma.hexdigest() != esperada:
                raise ErroDeAtualizacao("INTEGRITY")
            try:
                self.instalacao.instalar_zip(arquivo, str(versao))
            except PacoteInvalido as erro:
                raise ErroDeAtualizacao("PACKAGE_INVALID") from erro
