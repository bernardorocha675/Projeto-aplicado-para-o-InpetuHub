#!/usr/bin/env python3
"""Gera o pacote do InPETU Hub para Linux e Windows (decisão 11): jar, Java mínimo (jlink), iniciador (PyInstaller),
bundle.json com o SHA-256 de cada arquivo e o zip InPETU-Hub-<Linux|Windows>-<x64|ARM64>-v<versão>.zip, mais a
linha dele no SHA256SUMS.txt. Gera o pacote do sistema em que roda (o PyInstaller não gera para outro sistema).

Uso: python3 scripts/build_pacote.py --versao 1.0.0-alpha.1 [--saida dist] [--pular-maven]
Precisa do JDK 25 (jlink e jdeps; JAVA_HOME ou o PATH) e do Python com o venv e o tkinter (python3-tk).
"""
import argparse
import hashlib
import json
import os
import pathlib
import platform as plataforma_do_sistema
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile

RAIZ = pathlib.Path(__file__).resolve().parents[1]
JAR = "inpetu-hub-sistema.jar"
VERSAO = re.compile(r"1\.(0|[1-9]\d*)\.(0|[1-9]\d*)(-(alpha|beta|rc)\.[1-9]\d*)?")
# O jdeps não enxerga estes: o Jetty lê as telas de dentro do jar (zipfs), o H2 e o Jetty usam o Unsafe, e os
# valores em reais e as datas precisam dos dados de idioma do pt-BR.
EXTRAS = ("jdk.zipfs", "jdk.unsupported", "jdk.localedata")
# O pt-BR herda do pt no CLDR: sem o pt, os valores saem como R$ 1,600.00 (achado no teste de fumaça).
IDIOMAS = "en,pt,pt-BR"
DATA_FIXA = (2026, 1, 1, 0, 0, 0)


def validar_versao(texto):
    if not VERSAO.fullmatch(texto or ""):
        raise SystemExit(f"Versão fora da linha 1.x (ex.: 1.0.0-alpha.1): {texto!r}")
    return texto


def plataforma(maquina=None, nome_do_so=None):
    maquina = (maquina or plataforma_do_sistema.machine()).lower()
    sistema = "Windows" if (nome_do_so or os.name) == "nt" else "Linux"
    return f"{sistema}-{'ARM64' if maquina in ('aarch64', 'arm64') else 'x64'}"


def windows(plataforma_):
    return plataforma_.startswith("Windows")


def opcoes_jlink(plataforma_):
    opcoes = [f"--include-locales={IDIOMAS}", "--strip-debug", "--no-header-files", "--no-man-pages",
              "--compress=zip-6"]
    if not windows(plataforma_):
        opcoes.append("--strip-native-debug-symbols=exclude-debuginfo-files")
    return opcoes


def opcoes_pyinstaller(plataforma_):
    # No Windows, sem a janela preta de console: a pessoa só vê a janela do iniciador.
    return ["--windowed"] if windows(plataforma_) else []


def no_venv(venv, programa, nome_do_so=None):
    if (nome_do_so or os.name) == "nt":
        return venv / "Scripts" / f"{programa}.exe"
    return venv / "bin" / programa


def nome_do_zip(plataforma_, versao):
    return f"InPETU-Hub-{plataforma_}-v{versao}.zip"


def modulos(do_jdeps):
    nomes = [m for m in do_jdeps.strip().split(",") if m]
    return ",".join(sorted(set(nomes) | set(EXTRAS)))


def sha256(arquivo):
    soma = hashlib.sha256()
    with open(arquivo, "rb") as f:
        for bloco in iter(lambda: f.read(1 << 20), b""):
            soma.update(bloco)
    return soma.hexdigest()


def arquivos(pasta):
    """Os arquivos do pacote em ordem fixa; link é recusado (o iniciador recusa link ao instalar)."""
    lista = []
    for caminho in sorted(pasta.rglob("*")):
        if caminho.is_symlink():
            raise SystemExit(f"O pacote não pode ter link: {caminho.relative_to(pasta)}")
        if caminho.is_file():
            lista.append(caminho)
    return lista


def materializar(pasta):
    """Troca cada link pela cópia do que ele aponta (o JDK de algumas distribuições e o PyInstaller criam links)."""
    for caminho in sorted(pasta.rglob("*"), key=lambda c: len(c.parts)):
        if not caminho.is_symlink():
            continue
        alvo = caminho.resolve()
        if not alvo.exists():
            raise SystemExit(f"Link quebrado no pacote: {caminho.relative_to(pasta)}")
        caminho.unlink()
        if alvo.is_dir():
            shutil.copytree(alvo, caminho, symlinks=False)
        else:
            shutil.copy2(alvo, caminho)


def escrever_bundle(pasta, versao, plataforma_):
    exe = ".exe" if windows(plataforma_) else ""
    dados = {"versao": versao, "plataforma": plataforma_.lower(), "iniciador": f"inpetu-hub{exe}",
             "java": f"runtime/bin/java{exe}", "jar": f"app/{JAR}",
             "arquivos": {c.relative_to(pasta).as_posix(): sha256(c) for c in arquivos(pasta)
                          if c.name != "bundle.json" or c.parent != pasta}}
    (pasta / "bundle.json").write_text(json.dumps(dados, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")


def zipar(pasta, saida, plataforma_, versao):
    """O zip com uma pasta de mesmo nome dentro, permissões preservadas e data fixa (mesmo conteúdo, mesmo zip)."""
    saida.mkdir(parents=True, exist_ok=True)
    destino = saida / nome_do_zip(plataforma_, versao)
    raiz = destino.stem + "/"
    with zipfile.ZipFile(destino, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for caminho in arquivos(pasta):
            info = zipfile.ZipInfo(raiz + caminho.relative_to(pasta).as_posix(), DATA_FIXA)
            info.external_attr = (0o100755 if os.access(caminho, os.X_OK) else 0o100644) << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            with open(caminho, "rb") as f, z.open(info, "w") as entrada:
                shutil.copyfileobj(f, entrada)
    return destino


def atualizar_somas(saida, pacote):
    somas = saida / "SHA256SUMS.txt"
    linhas = []
    if somas.exists():
        linhas = [l for l in somas.read_text(encoding="utf-8").splitlines() if not l.endswith("  " + pacote.name)]
    linhas.append(f"{sha256(pacote)}  {pacote.name}")
    somas.write_text("\n".join(linhas) + "\n", encoding="utf-8")


def rodar(comando, **opcoes):
    print("+", " ".join(str(c) for c in comando), flush=True)
    return subprocess.run([str(c) for c in comando], check=True, **opcoes)


def ferramenta(nome):
    java_home = os.environ.get("JAVA_HOME")
    if java_home and shutil.which(nome, path=str(pathlib.Path(java_home) / "bin")):
        return pathlib.Path(shutil.which(nome, path=str(pathlib.Path(java_home) / "bin")))
    encontrada = shutil.which(nome)
    if not encontrada:
        raise SystemExit(f"{nome} não encontrado: instale o JDK 25 ou defina JAVA_HOME.")
    return pathlib.Path(encontrada)


def java_minimo(jar, destino, plataforma_):
    deps = rodar([ferramenta("jdeps"), "--print-module-deps", "--ignore-missing-deps", "--multi-release", "25", jar],
                 capture_output=True, text=True).stdout
    rodar([ferramenta("jlink"), "--add-modules", modulos(deps), *opcoes_jlink(plataforma_), "--output", destino])
    # O JDK de algumas distribuições traz os símbolos de depuração (centenas de MB) como links; não vão no pacote.
    for depuracao in destino.rglob("*.debuginfo"):
        depuracao.unlink()


def iniciador(montagem, trabalho, plataforma_):
    venv = trabalho / "venv"
    rodar([sys.executable, "-m", "venv", venv])
    pip = no_venv(venv, "pip")
    rodar([pip, "install", "--disable-pip-version-check", "--quiet", "-r", RAIZ / "scripts" / "requisitos-pacote.txt"])
    entrada = trabalho / "inpetu_hub.py"
    entrada.write_text("from inpetu_iniciador.__main__ import main\nimport sys\nsys.exit(main())\n", encoding="utf-8")
    rodar([no_venv(venv, "pyinstaller"), "--noconfirm", "--clean", "--onedir", *opcoes_pyinstaller(plataforma_),
           "--name", "inpetu-hub",
           "--paths", RAIZ / "inicializador", "--hidden-import", "tkinter", "--distpath", trabalho / "dist",
           "--workpath", trabalho / "build", "--specpath", trabalho, entrada])
    for item in (trabalho / "dist" / "inpetu-hub").iterdir():
        shutil.move(str(item), montagem / item.name)


def main(lista=None):
    leitor = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    leitor.add_argument("--versao", required=True)
    leitor.add_argument("--saida", type=pathlib.Path, default=RAIZ / "dist")
    leitor.add_argument("--pular-maven", action="store_true", help="usa o jar já gerado em sistema/target")
    argumentos = leitor.parse_args(lista)
    versao = validar_versao(argumentos.versao)
    plataforma_ = plataforma()
    jar = RAIZ / "sistema" / "target" / JAR
    if not argumentos.pular_maven:
        mvnw = str(RAIZ / "sistema" / "mvnw.cmd") if os.name == "nt" else "./mvnw"
        rodar([mvnw, "-B", "-ntp", "-q", "clean", "package", "-DskipTests"], cwd=RAIZ / "sistema")
    if not jar.is_file():
        raise SystemExit("Jar não encontrado: rode sem --pular-maven.")
    with tempfile.TemporaryDirectory(prefix="inpetu-pacote-") as temporaria:
        trabalho = pathlib.Path(temporaria)
        montagem = trabalho / "montagem"
        (montagem / "app").mkdir(parents=True)
        shutil.copy2(jar, montagem / "app" / JAR)
        java_minimo(jar, montagem / "runtime", plataforma_)
        iniciador(montagem, trabalho, plataforma_)
        materializar(montagem)
        escrever_bundle(montagem, versao, plataforma_)
        destino = zipar(montagem, argumentos.saida, plataforma_, versao)
    atualizar_somas(argumentos.saida, destino)
    print(f"Pacote: {destino} ({destino.stat().st_size // 1_000_000} MB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
