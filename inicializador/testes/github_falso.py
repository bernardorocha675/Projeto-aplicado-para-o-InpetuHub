"""Um GitHub falso para os testes: a lista de releases, os pacotes e o SHA256SUMS, com respostas configuráveis."""
import hashlib
import http.server
import io
import json
import threading
import time
import zipfile

from inpetu_iniciador import so


def pacote_zip(versao, plataforma=None, extras=None, bundle=None, pasta_raiz=True):
    """Um zip de pacote com bundle.json, runtime/bin/java, app/inpetu-hub-sistema.jar e o executável do iniciador."""
    plataforma = plataforma or so.plataforma()
    buffer = io.BytesIO()
    raiz = f"InPETU-Hub-{plataforma}-v{versao}/" if pasta_raiz else ""
    with zipfile.ZipFile(buffer, "w") as z:
        dados = bundle if bundle is not None else {"versao": versao, "plataforma": plataforma.lower(),
                                                    "iniciador": so.INICIADOR, "java": so.JAVA,
                                                    "jar": "app/inpetu-hub-sistema.jar"}
        z.writestr(raiz + "bundle.json", json.dumps(dados))
        for nome, conteudo, executavel in [(so.JAVA, "#!/bin/sh\n", True), (so.INICIADOR, "#!/bin/sh\n", True),
                                           ("app/inpetu-hub-sistema.jar", "jar", False)]:
            info = zipfile.ZipInfo(raiz + nome)
            info.external_attr = (0o100755 if executavel else 0o100644) << 16
            z.writestr(info, conteudo)
        for nome, conteudo in (extras or {}).items():
            if isinstance(conteudo, zipfile.ZipInfo):
                z.writestr(conteudo, "x")
            else:
                z.writestr(nome, conteudo)
    return buffer.getvalue()


class GitHubFalso:
    """Servidor local. `releases` é a lista devolvida pela API; `arquivos` mapeia caminho → bytes."""

    def __init__(self):
        self.releases = []
        self.arquivos = {}
        self.status_api = 200
        self.atraso_api = 0
        self.pedidos = []
        falso = self

        class Atendente(http.server.BaseHTTPRequestHandler):
            def do_GET(self):
                falso.pedidos.append(self.path)
                if self.path.startswith("/repos/"):
                    time.sleep(falso.atraso_api)
                    if falso.status_api != 200:
                        self.send_response(falso.status_api)
                        self.end_headers()
                        self.wfile.write(b'{"message":"segredo do corpo nao vai para o log"}')
                        return
                    pagina = int(self.path.split("page=")[-1]) if "page=" in self.path else 1
                    corpo = json.dumps(falso.releases if pagina == 1 else []).encode()
                elif self.path in falso.arquivos:
                    corpo = falso.arquivos[self.path]
                else:
                    self.send_response(404)
                    self.end_headers()
                    return
                self.send_response(200)
                self.send_header("Content-Length", str(len(corpo)))
                self.end_headers()
                self.wfile.write(corpo)

            def log_message(self, *args):
                pass

            def handle(self):
                try:
                    super().handle()
                except (BrokenPipeError, ConnectionResetError):
                    pass  # O cliente desistiu (o teste de tempo esgotado faz isso de propósito).

        self.servidor = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Atendente)
        threading.Thread(target=self.servidor.serve_forever, daemon=True).start()
        self.base = f"http://127.0.0.1:{self.servidor.server_port}"

    def fechar(self):
        self.servidor.shutdown()
        self.servidor.server_close()

    def publicar(self, versao, plataforma=None, zip_bytes=None, sums=True, estado="uploaded",
                 rascunho=False, pre=None, soma_errada=False):
        """Acrescenta uma release com o pacote (e o SHA256SUMS) servidos pelo falso."""
        plataforma = plataforma or so.plataforma()
        nome = f"InPETU-Hub-{plataforma}-v{versao}.zip"
        conteudo = zip_bytes if zip_bytes is not None else pacote_zip(versao, plataforma)
        caminho_zip = f"/download/v{versao}/{nome}"
        self.arquivos[caminho_zip] = conteudo
        ativos = [{"name": nome, "state": estado, "size": len(conteudo),
                   "browser_download_url": self.base + caminho_zip}]
        if sums:
            soma = hashlib.sha256(b"outro" if soma_errada else conteudo).hexdigest()
            caminho_sums = f"/download/v{versao}/SHA256SUMS.txt"
            self.arquivos[caminho_sums] = f"{soma}  {nome}\n{'0' * 64}  outro.zip\n".encode()
            ativos.append({"name": "SHA256SUMS.txt", "state": "uploaded", "size": 100,
                           "browser_download_url": self.base + caminho_sums})
        self.releases.insert(0, {"tag_name": f"v{versao}", "draft": rascunho,
                                 "prerelease": "-" in versao if pre is None else pre, "assets": ativos})
