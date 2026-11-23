"""As versões instaladas em InPETU-Hub/1/versoes/<versão>, a atual (arquivo versoes/atual) e a extração segura.

Uma versão nova é extraída numa pasta provisória, conferida (bundle.json da mesma versão, Java e jar presentes) e só
então renomeada. A anterior nunca é apagada na hora: ficam as três mais novas e sempre a atual.
"""
import hashlib
import json
import os
import pathlib
import shutil
import stat
import tempfile
import zipfile

from inpetu_iniciador.versoes import Versao

LIMITE_ARQUIVOS = 20_000
LIMITE_TOTAL = 1_500_000_000


class PacoteInvalido(Exception):
    """O pacote baixado não é um pacote completo desta versão."""


class Instalacao:

    def __init__(self, pastas):
        self.pastas = pastas

    @property
    def pasta(self):
        return self.pastas.versoes

    def atual(self):
        """(versão, pasta) da versão atual, ou None."""
        try:
            versao = Versao.de((self.pasta / "atual").read_text(encoding="utf-8").strip())
        except OSError:
            return None
        if versao is None or not (self.pasta / str(versao) / "bundle.json").is_file():
            return None
        return versao, self.pasta / str(versao)

    def marcar_atual(self, versao):
        """Troca a versão atual de uma vez (grava num arquivo temporário e renomeia)."""
        temporario = self.pasta / "atual.novo"
        temporario.write_text(str(versao), encoding="utf-8")
        os.replace(temporario, self.pasta / "atual")

    def instalar_zip(self, arquivo, versao_esperada):
        """Extrai o zip com segurança e devolve a pasta da versão instalada."""
        provisoria = self._provisoria()
        try:
            with zipfile.ZipFile(arquivo) as z:
                _extrair(z, provisoria)
            return self._efetivar(provisoria, versao_esperada)
        except (zipfile.BadZipFile, OSError) as erro:
            shutil.rmtree(provisoria, ignore_errors=True)
            raise PacoteInvalido("zip inválido") from erro
        except Exception:
            shutil.rmtree(provisoria, ignore_errors=True)
            raise

    def instalar_pasta(self, origem):
        """Copia um pacote já extraído (a pasta do executável) e devolve a pasta da versão instalada."""
        versao = self.versao_da_pasta(origem)
        if versao is None:
            raise PacoteInvalido("pasta sem bundle.json")
        destino = self.pasta / str(versao)
        if destino.is_dir():
            return destino
        provisoria = self._provisoria()
        try:
            shutil.copytree(origem, provisoria / "pacote", symlinks=False)
            return self._efetivar(provisoria, str(versao))
        except Exception:
            shutil.rmtree(provisoria, ignore_errors=True)
            raise

    def versao_da_pasta(self, pasta):
        try:
            dados = json.loads((pathlib.Path(pasta) / "bundle.json").read_text(encoding="utf-8"))
        except (OSError, ValueError):
            return None
        return Versao.de(dados.get("versao", "")) if isinstance(dados, dict) else None

    def limpar_antigas(self, manter=3):
        """Apaga versões antigas: ficam no máximo {manter}, sempre com a atual e depois as mais novas."""
        atual = self.atual()
        versoes = sorted((Versao.de(p.name), p) for p in self.pasta.iterdir() if p.is_dir() and Versao.de(p.name))
        guardar = {atual[1]} if atual is not None else set()
        for _, pasta in reversed(versoes):
            if len(guardar) >= manter:
                break
            guardar.add(pasta)
        for _, pasta in versoes:
            if pasta not in guardar:
                _apagar_inteira(pasta)

    def limpar_sobras(self):
        """Apaga o que uma instalação ou limpeza interrompida deixou (só com a trava do iniciador)."""
        for sobra in self.pasta.iterdir():
            if sobra.is_dir() and sobra.name.startswith((".parcial-", ".apagar-")):
                shutil.rmtree(sobra, ignore_errors=True)

    def _provisoria(self):
        self.pasta.mkdir(parents=True, exist_ok=True)
        return pathlib.Path(tempfile.mkdtemp(prefix=".parcial-", dir=self.pasta))

    def _efetivar(self, provisoria, versao_esperada):
        raiz = _raiz_do_pacote(provisoria)
        dados = json.loads((raiz / "bundle.json").read_text(encoding="utf-8"))
        if not isinstance(dados, dict) or dados.get("versao") != versao_esperada:
            raise PacoteInvalido("bundle.json de outra versão")
        for chave in ("java", "jar"):
            relativo = dados.get(chave)
            if not isinstance(relativo, str) or not (raiz / relativo).is_file():
                raise PacoteInvalido("pacote incompleto")
        somas = dados.get("arquivos", {})
        if not isinstance(somas, dict):
            raise PacoteInvalido("bundle.json inválido")
        for relativo, esperada in somas.items():
            arquivo = (raiz / relativo).resolve()
            if raiz.resolve() not in arquivo.parents or not arquivo.is_file() or _sha256(arquivo) != esperada:
                raise PacoteInvalido("arquivo do pacote diferente do bundle.json")
        destino = self.pasta / versao_esperada
        if destino.exists():
            shutil.rmtree(provisoria, ignore_errors=True)
            return destino
        os.replace(raiz, destino)
        shutil.rmtree(provisoria, ignore_errors=True)
        return destino


def _apagar_inteira(pasta):
    """Renomeia antes de apagar: no Windows, uma versão em uso não pode ser renomeada e fica inteira (sem isso, os
    arquivos livres seriam apagados e a versão ficaria pela metade)."""
    lixo = pasta.with_name(f".apagar-{pasta.name}")
    shutil.rmtree(lixo, ignore_errors=True)
    try:
        os.replace(pasta, lixo)
    except OSError:
        return
    shutil.rmtree(lixo, ignore_errors=True)


def _sha256(arquivo):
    soma = hashlib.sha256()
    with open(arquivo, "rb") as f:
        for bloco in iter(lambda: f.read(1 << 20), b""):
            soma.update(bloco)
    return soma.hexdigest()


def _raiz_do_pacote(pasta):
    """O bundle.json fica na raiz do zip ou numa única pasta dentro dele."""
    for candidata in (pasta, *[p for p in pasta.iterdir() if p.is_dir()]):
        if (candidata / "bundle.json").is_file():
            return candidata
    raise PacoteInvalido("sem bundle.json")


def _extrair(z, destino):
    infos = z.infolist()
    if len(infos) > LIMITE_ARQUIVOS or sum(i.file_size for i in infos) > LIMITE_TOTAL:
        raise PacoteInvalido("pacote grande demais")
    base = destino.resolve()
    for info in infos:
        nome = info.filename
        partes = pathlib.PurePosixPath(nome).parts
        if nome.startswith("/") or "\\" in nome or ".." in partes or (partes and ":" in partes[0]):
            raise PacoteInvalido("caminho perigoso no pacote")
        modo = info.external_attr >> 16
        if stat.S_ISLNK(modo):
            raise PacoteInvalido("link no pacote")
        alvo = (destino / nome).resolve()
        if base != alvo and base not in alvo.parents:
            raise PacoteInvalido("caminho fora da pasta")
        if info.is_dir():
            alvo.mkdir(parents=True, exist_ok=True)
            continue
        alvo.parent.mkdir(parents=True, exist_ok=True)
        with z.open(info) as origem, open(alvo, "wb") as saida:
            shutil.copyfileobj(origem, saida)
        alvo.chmod(0o755 if modo & 0o111 else 0o644)
