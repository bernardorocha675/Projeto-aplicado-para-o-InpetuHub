"""Prepara somente o Maven de desenvolvimento/CI, com checksum oficial."""
import hashlib
import os
from pathlib import Path, PurePosixPath
import shutil
import tempfile
import time
import urllib.error
import urllib.request
import zipfile

VERSION = '3.9.9'
TENTATIVAS = 3
# O Maven Central às vezes responde 404 ou 5xx por instantes (visto na CI em 03/11/2026): esses códigos valem nova tentativa.
PASSAGEIROS = {404, 408, 429, 500, 502, 503, 504}


def baixar(url, timeout):
    """Conteúdo do endereço, com até TENTATIVAS tentativas para falhas passageiras de rede ou do servidor."""
    for tentativa in range(1, TENTATIVAS + 1):
        try:
            with urllib.request.urlopen(url, timeout=timeout) as response:
                return response.read()
        except urllib.error.HTTPError as erro:
            if erro.code not in PASSAGEIROS or tentativa == TENTATIVAS:
                raise
            motivo = f'HTTP {erro.code}'
        except (urllib.error.URLError, TimeoutError, ConnectionError) as erro:
            if tentativa == TENTATIVAS:
                raise
            motivo = type(erro).__name__
        print(f'Download do Maven falhou ({motivo}); nova tentativa {tentativa + 1} de {TENTATIVAS}.')
        time.sleep(5 * tentativa)


def main():
    project = Path(__file__).resolve().parents[2] / 'aplicativo'
    tools = project / '.tools'
    tools.mkdir(exist_ok=True)
    target = tools / ('apache-maven-' + VERSION)
    command = target / 'bin' / ('mvn.cmd' if os.name == 'nt' else 'mvn')
    if not command.is_file():
        url = f'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/{VERSION}/apache-maven-{VERSION}-bin.zip'
        with tempfile.TemporaryDirectory(dir=tools, prefix='maven-') as temp:
            archive = Path(temp) / 'maven.zip'
            archive.write_bytes(baixar(url, 90))
            expected = baixar(url + '.sha512', 30)[:4096].decode('ascii').split()[0].lower()
            if len(expected) != 128 or hashlib.sha512(archive.read_bytes()).hexdigest() != expected:
                raise SystemExit('Checksum do Maven inválido.')
            with zipfile.ZipFile(archive) as zipped:
                if zipped.testzip() is not None:
                    raise SystemExit('Arquivo do Maven corrompido.')
                for entry in zipped.infolist():
                    path = PurePosixPath(entry.filename)
                    if path.is_absolute() or '..' in path.parts or '\\' in entry.filename or ':' in entry.filename or not path.parts or path.parts[0] != target.name:
                        raise SystemExit('Caminho inválido no pacote do Maven.')
                zipped.extractall(temp)
            shutil.move(str(Path(temp) / target.name), target)
        if os.name != 'nt':
            command.chmod(0o755)
    if not command.is_file():
        raise SystemExit('Maven não foi preparado.')
    if os.environ.get('GITHUB_PATH'):
        with open(os.environ['GITHUB_PATH'], 'a', encoding='utf-8') as output:
            output.write(str(command.parent) + '\n')
    print('Maven de desenvolvimento preparado:', command)


if __name__ == '__main__':
    main()
