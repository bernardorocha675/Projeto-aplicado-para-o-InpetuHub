"""Prepara somente o Maven de desenvolvimento/CI, com checksum oficial."""
import hashlib
import os
from pathlib import Path, PurePosixPath
import shutil
import tempfile
import urllib.request
import zipfile

VERSION = '3.9.9'
project = Path(__file__).resolve().parents[2] / 'aplicativo'
tools = project / '.tools'
tools.mkdir(exist_ok=True)
target = tools / ('apache-maven-' + VERSION)
command = target / 'bin' / ('mvn.cmd' if os.name == 'nt' else 'mvn')
if not command.is_file():
    url = f'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/{VERSION}/apache-maven-{VERSION}-bin.zip'
    with tempfile.TemporaryDirectory(dir=tools, prefix='maven-') as temp:
        archive = Path(temp) / 'maven.zip'
        with urllib.request.urlopen(url, timeout=90) as response, archive.open('wb') as output:
            shutil.copyfileobj(response, output)
        with urllib.request.urlopen(url + '.sha512', timeout=30) as response:
            expected = response.read(4096).decode('ascii').split()[0].lower()
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
