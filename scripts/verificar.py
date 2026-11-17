#!/usr/bin/env python3
"""Verificação local do sistema novo (substitui o verificar-tudo do antigo, decisão 7):

  python3 scripts/verificar.py            testes e formatação do Java, testes do iniciador (com o jar) e dos scripts
  python3 scripts/verificar.py --pacote   e também gera o pacote desta máquina e roda o teste de fumaça

Mostra cada etapa e termina com o resumo; sai com 1 se alguma falhar.
"""
import argparse
import os
import pathlib
import subprocess
import sys
import tempfile
import time

RAIZ = pathlib.Path(__file__).resolve().parents[1]
VERSAO_DE_TESTE = "1.0.0-alpha.999"


def etapa(nome, comando, pasta, ambiente=None):
    print(f"\n=== {nome}", flush=True)
    inicio = time.monotonic()
    codigo = subprocess.run(comando, cwd=pasta, env={**os.environ, **(ambiente or {})}).returncode
    return nome, codigo == 0, time.monotonic() - inicio


def main():
    leitor = argparse.ArgumentParser(description="Verificação local do sistema novo.")
    leitor.add_argument("--pacote", action="store_true", help="gera o pacote desta máquina e roda a fumaça")
    argumentos = leitor.parse_args()
    jar = RAIZ / "sistema" / "target" / "inpetu-hub-sistema.jar"
    resultados = [etapa("Java: testes e formatação (./mvnw clean verify)", ["./mvnw", "-B", "-ntp", "clean", "verify"],
                        RAIZ / "sistema")]
    resultados.append(etapa("Iniciador: testes (com o jar real)", [sys.executable, "-m", "unittest", "discover", "-s",
                                                                    "testes", "-t", "."], RAIZ / "inicializador",
                            {"INPETU_JAR": str(jar)}))
    resultados.append(etapa("Scripts: testes", [sys.executable, "-m", "unittest", "discover", "-s", "scripts/tests",
                                                "-t", "scripts"], RAIZ))
    if argumentos.pacote:
        with tempfile.TemporaryDirectory(prefix="inpetu-verificar-") as saida:
            resultados.append(etapa("Pacote: gerar", [sys.executable, "scripts/build_pacote.py", "--versao",
                                                      VERSAO_DE_TESTE, "--pular-maven", "--saida", saida], RAIZ))
            zips = list(pathlib.Path(saida).glob("*.zip"))
            if zips:
                resultados.append(etapa("Pacote: fumaça", [sys.executable, "scripts/smoke_pacote.py", str(zips[0])],
                                        RAIZ))
    print("\n=== Resumo")
    for nome, aprovado, segundos in resultados:
        print(f"{'APROVADO' if aprovado else 'FALHOU  '}  {nome} ({segundos:.0f} s)")
    if not argumentos.pacote:
        print("NÃO RODADO  Pacote e fumaça (use --pacote)")
    return 0 if all(aprovado for _, aprovado, _ in resultados) else 1


if __name__ == "__main__":
    sys.exit(main())
