"""Empacota somente conteúdo público rastreado; publica apenas na CI de main."""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import tempfile
import sys
import xml.etree.ElementTree as ET
import zipfile


def run(*args):
    return subprocess.check_output(args, text=True).strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='Confere o pacote sem publicar.')
    args = parser.parse_args()
    os.chdir(run('git', 'rev-parse', '--show-toplevel'))
    version = Path('VERSION').read_text().strip()
    if not re.fullmatch(r'(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:fix)?', version):
        raise SystemExit('VERSION precisa conter um número como 0.3.1 ou 0.3.1fix.')
    major, minor, _ = version.split('.')
    folder = f'versão {major}.{minor}'
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    pom_version = ET.parse(f'{folder}/pom.xml').getroot().findtext('m:version', namespaces=ns)
    if pom_version != version:
        raise SystemExit('VERSION e a versão do projeto no pom.xml precisam coincidir.')
    subprocess.run([sys.executable, f'{folder}/scripts/update_manifest.py', '--check'], check=True)
    if not Path('RELEASE-NOTES.md').read_text().startswith(f'# InPETU Hub {version}\n'):
        raise SystemExit('Atualize o número e o conteúdo de RELEASE-NOTES.md.')
    paths = [folder, '.gitignore', 'README.md', '.github/SECURITY.md', 'VERSION', 'RELEASE-NOTES.md', 'RELEASES.md']
    subprocess.run(['git', 'diff', '--exit-code', 'HEAD', '--', *paths], check=True)
    tracked = list(filter(None, run('git', 'ls-files', '-z', '--', *paths).split('\0')))
    forbidden = {'.secrets', '.tools', 'data', 'backups', 'target', 'logs', 'relatorios-testes'}
    for name in tracked:
        p = PurePosixPath(name)
        if forbidden.intersection(p.parts) or p.name in {'.env', '.env.local', 'accounts.properties'} or p.suffix in {'.p12', '.jks', '.pem', '.key', '.db', '.zip', '.inpetu'} or '.db.' in p.name:
            raise SystemExit(f'Arquivo privado ou gerado não pode integrar a release: {name}')
    sha = run('git', 'rev-parse', 'HEAD')
    tag = f'v{version}'
    with tempfile.TemporaryDirectory(prefix='inpetu-release-') as tmp:
        archive = Path(tmp) / f'inpetu-hub-{tag}.zip'
        subprocess.run(['git', 'archive', '--format=zip', '--output', str(archive), sha, '--', *paths], check=True)
        with zipfile.ZipFile(archive) as z:
            if z.testzip() is not None or set(z.namelist()) - {n for n in z.namelist() if n.endswith('/')} != set(tracked):
                raise SystemExit('O ZIP não corresponde aos arquivos públicos selecionados.')
        checksum = Path(tmp) / 'SHA256SUMS.txt'
        checksum.write_text(f'{hashlib.sha256(archive.read_bytes()).hexdigest()}  {archive.name}\n')
        print(f'Pacote conferido: {archive.name}, {len(tracked)} arquivos, commit {sha}.', flush=True)
        if args.check:
            return
        if os.environ.get('GITHUB_ACTIONS') != 'true' or os.environ.get('GITHUB_REF') != 'refs/heads/main' or os.environ.get('GITHUB_EVENT_NAME') not in {'push', 'workflow_dispatch'} or os.environ.get('GITHUB_SHA') != sha:
            raise SystemExit('Publicação permitida somente na CI do commit de main.')
        repo = os.environ['GITHUB_REPOSITORY']
        existing = subprocess.run(['gh', 'release', 'view', tag, '--repo', repo, '--json', 'isDraft,url'], text=True, capture_output=True)
        if existing.returncode == 0:
            release = json.loads(existing.stdout)
            if release['isDraft']:
                raise SystemExit('Existe um rascunho com esta tag; revise-o antes de continuar.')
            print('Release já publicada; nenhuma alteração: ' + release['url'])
            return
        # Never attach a new release to a pre-existing tag at another commit.
        ref = subprocess.run(['gh', 'api', f'repos/{repo}/git/ref/tags/{tag}'], text=True, capture_output=True)
        if ref.returncode == 0:
            obj = json.loads(ref.stdout)['object']
            if obj['type'] != 'commit' or obj['sha'] != sha:
                raise SystemExit('Tag já existe em outro objeto; use um novo número de versão.')
        elif 'HTTP 404' not in ref.stderr:
            raise SystemExit('Não foi possível conferir a tag no GitHub; publicação interrompida.')
        subprocess.run(['gh', 'release', 'create', tag, str(archive), str(checksum), '--repo', repo, '--target', sha, '--title', f'InPETU Hub {version}', '--notes-file', 'RELEASE-NOTES.md', '--latest'], check=True)


if __name__ == '__main__':
    main()
