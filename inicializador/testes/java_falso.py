#!/usr/bin/env python3
"""Faz o papel do Java nos testes: lê os mesmos argumentos que o sistema, responde /saude e atende o arquivo de parada.

Modo pela variável FALSO_MODO: normal, ignora_parada (só sai se for morto), falha (sai com código 3 antes de subir),
banco_em_uso (falha como o sistema quando outra cópia usa o banco). FALSO_ARGS: arquivo onde grava os argumentos.
"""
import http.server
import json
import os
import pathlib
import sys
import threading
import time


def main():
    argumentos = sys.argv[1:]
    if os.environ.get("FALSO_ARGS"):
        pathlib.Path(os.environ["FALSO_ARGS"]).write_text(json.dumps(argumentos), encoding="utf-8")
        ambiente = {chave: os.environ.get(chave) for chave in ("LC_ALL", "LANG", "LD_LIBRARY_PATH")}
        pathlib.Path(os.environ["FALSO_ARGS"] + ".ambiente").write_text(json.dumps(ambiente), encoding="utf-8")
    propriedades = dict(a[2:].split("=", 1) for a in argumentos if a.startswith("-D") and "=" in a)
    porta = int(argumentos[-1])
    modo = os.environ.get("FALSO_MODO", "normal")
    if modo == "falha":
        print('{"message":"event=SERVER_START_FAILED failure=OTHER"}', file=sys.stderr, flush=True)
        sys.exit(3)
    if modo == "banco_em_uso":
        print('{"message":"event=SERVER_START_FAILED failure=DATABASE_IN_USE"}', file=sys.stderr, flush=True)
        sys.exit(1)
    parada = pathlib.Path(propriedades["inpetu.parada"])
    parada.unlink(missing_ok=True)

    class Saude(http.server.BaseHTTPRequestHandler):
        def do_GET(self):
            self.send_response(200 if self.path == "/saude" else 404)
            self.end_headers()
            self.wfile.write(b"ok")

        def log_message(self, *args):
            pass

    servidor = http.server.HTTPServer(("127.0.0.1", porta), Saude)
    threading.Thread(target=servidor.serve_forever, daemon=True).start()
    print('{"message":"event=SERVER_READY"}', file=sys.stderr, flush=True)
    while True:
        if parada.exists() and modo != "ignora_parada":
            parada.unlink()
            servidor.shutdown()
            print('{"message":"event=SERVER_STOPPED"}', file=sys.stderr, flush=True)
            sys.exit(0)
        time.sleep(0.1)


if __name__ == "__main__":
    main()
