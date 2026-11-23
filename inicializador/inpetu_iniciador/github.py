"""Consulta pública às Releases do GitHub, sem token (nunca há credencial no executável). Só endereços do GitHub.

Falhas viram ErroDeAtualizacao com o tipo (RATE_LIMITED, HTTP_STATUS, TIMEOUT, NETWORK, INVALID_RESPONSE) e o
status, nunca o endereço, os cabeçalhos ou o corpo da resposta.
"""
import json
import socket
import urllib.error
import urllib.parse
import urllib.request

from inpetu_iniciador import VERSAO

HOSTS = ("api.github.com", "github.com", "objects.githubusercontent.com", "release-assets.githubusercontent.com")
LIMITE_API = 5_000_000


class ErroDeAtualizacao(Exception):

    def __init__(self, tipo, status=None):
        super().__init__(tipo)
        self.tipo = tipo
        self.status = status


class Releases:

    def __init__(self, base, repositorio, tempo=15, permitir_local=False):
        self.permitir_local = permitir_local
        self._conferir(base)
        self.base = base.rstrip("/")
        self.repositorio = repositorio
        self.tempo = tempo

    def listar(self, paginas=3):
        """As releases, da mais nova para a mais antiga (até 3 páginas de 30)."""
        todas = []
        for pagina in range(1, paginas + 1):
            corpo = self.ler(f"{self.base}/repos/{self.repositorio}/releases?per_page=30&page={pagina}", LIMITE_API)
            try:
                lista = json.loads(corpo)
            except ValueError as erro:
                raise ErroDeAtualizacao("INVALID_RESPONSE") from erro
            if not isinstance(lista, list):
                raise ErroDeAtualizacao("INVALID_RESPONSE")
            todas.extend(r for r in lista if isinstance(r, dict))
            if len(lista) < 30:
                break
        return todas

    def ler(self, endereco, limite):
        with self._abrir(endereco) as resposta:
            corpo = resposta.read(limite + 1)
        if len(corpo) > limite:
            raise ErroDeAtualizacao("INVALID_RESPONSE")
        return corpo

    def baixar(self, endereco, destino, limite):
        """Grava o arquivo em destino, sem passar do limite de tamanho."""
        total = 0
        with self._abrir(endereco) as resposta, open(destino, "wb") as arquivo:
            while True:
                try:
                    bloco = resposta.read(1 << 20)
                except (TimeoutError, socket.timeout) as erro:
                    raise ErroDeAtualizacao("TIMEOUT") from erro
                except OSError as erro:
                    raise ErroDeAtualizacao("NETWORK") from erro
                if not bloco:
                    return
                total += len(bloco)
                if total > limite:
                    raise ErroDeAtualizacao("INVALID_RESPONSE")
                arquivo.write(bloco)

    def _abrir(self, endereco):
        self._conferir(endereco)
        pedido = urllib.request.Request(endereco, headers={"User-Agent": f"InPETU-Hub-iniciador/{VERSAO}",
                                                            "Accept": "application/vnd.github+json"})
        try:
            resposta = urllib.request.build_opener(_SoGitHub(self)).open(pedido, timeout=self.tempo)
        except urllib.error.HTTPError as erro:
            erro.close()
            raise ErroDeAtualizacao("RATE_LIMITED" if erro.code in (403, 429) else "HTTP_STATUS", erro.code) from None
        except (TimeoutError, socket.timeout):
            raise ErroDeAtualizacao("TIMEOUT") from None
        except urllib.error.URLError as erro:
            tipo = "TIMEOUT" if isinstance(erro.reason, (TimeoutError, socket.timeout)) else "NETWORK"
            raise ErroDeAtualizacao(tipo) from None
        except OSError:
            raise ErroDeAtualizacao("NETWORK") from None
        return resposta

    def _conferir(self, endereco):
        partes = urllib.parse.urlsplit(endereco)
        if self.permitir_local and partes.scheme == "http" and partes.hostname == "127.0.0.1":
            return
        if partes.scheme != "https" or partes.hostname not in HOSTS:
            raise ValueError("endereço fora do GitHub")


class _SoGitHub(urllib.request.HTTPRedirectHandler):
    """Os downloads do GitHub redirecionam; o destino também precisa ser do GitHub."""

    def __init__(self, releases):
        self.releases = releases

    def redirect_request(self, pedido, arquivo, codigo, mensagem, cabecalhos, novo):
        try:
            self.releases._conferir(novo)
        except ValueError:
            raise ErroDeAtualizacao("INVALID_RESPONSE") from None
        return super().redirect_request(pedido, arquivo, codigo, mensagem, cabecalhos, novo)
