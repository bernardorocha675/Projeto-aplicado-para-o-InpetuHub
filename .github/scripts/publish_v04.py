"""Publica somente os seis pacotes nativos aprovados no mesmo commit e o codigo publico."""
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import tempfile
import xml.etree.ElementTree as ET
import zipfile

def run(*args):
    return subprocess.check_output(args, text=True).strip()

def main():
    os.chdir(run('git','rev-parse','--show-toplevel'))
    version=Path('VERSION').read_text().strip()
    if not re.fullmatch(r'0\.(4|5)\.(0|[1-9]\d*)',version):
        raise SystemExit('Publicador limitado a releases numericas 0.4.x e 0.5.x.')
    ns={'m':'http://maven.apache.org/POM/4.0.0'}
    if ET.parse('aplicativo/pom.xml').getroot().findtext('m:version',namespaces=ns)!=version:
        raise SystemExit('VERSION e pom.xml divergem.')
    if not Path('RELEASE-NOTES.md').read_text().startswith(f'# InPETU Hub {version}\n'):
        raise SystemExit('Atualize RELEASE-NOTES.md.')
    sha=run('git','rev-parse','HEAD')
    if os.environ.get('GITHUB_ACTIONS')!='true' or os.environ.get('GITHUB_REF')!='refs/heads/main' or os.environ.get('GITHUB_SHA')!=sha or os.environ.get('GITHUB_EVENT_NAME') not in ('push','workflow_dispatch'):
        raise SystemExit('Publicacao permitida somente na CI do commit testado de main.')
    folder=Path('release-assets')
    expected={f'InPETU-Hub-{system}-{arch}-v{version}.zip' for system in ('Windows','macOS','Linux') for arch in ('x64','ARM64')}
    expected|={f'InPETU-Hub-Windows-{arch}.exe' for arch in ('x64','ARM64')}
    expected|={f'InPETU-Hub-Linux-{arch}.run' for arch in ('x64','ARM64')}
    actual={p.name for p in folder.iterdir() if p.is_file()}
    if actual!=expected:
        raise SystemExit('Artefatos nativos ausentes ou inesperados: '+str(sorted(actual^expected)))
    for path in folder.glob('*.zip'):
        with zipfile.ZipFile(path) as zipped:
            if zipped.testzip() is not None:
                raise SystemExit('ZIP nativo corrompido.')
            names=zipped.namelist()
            if not any(n.endswith('bundle.json') for n in names) or not any(n.endswith('app.jar') for n in names):
                raise SystemExit('Pacote nativo incompleto.')
            for name in names:
                parts=PurePosixPath(name).parts
                if {'data','.secrets','backups','logs','.env.local','.env'}.intersection(parts):
                    raise SystemExit('Dados privados no pacote nativo.')
    paths=['aplicativo','iniciador','receptor','docs','continuidade','CLAUDE.md',
           'README.md','.gitignore','.gitattributes','.gitleaks.toml','.gitleaksignore',
           '.github/SECURITY.md','.github/dependabot.yml',
           '.github/workflows','.github/scripts',
           'Testes/README.md','Testes/configuracao.json',
           'VERSION','RELEASE-NOTES.md','RELEASES.md']
    tracked=list(filter(None,run('git','ls-files','-z','--',*paths).split('\0')))
    for name in tracked:
        p=PurePosixPath(name)
        if {'.secrets','.tools','data','backups','target','logs','relatorios-testes','dist'}.intersection(p.parts) or p.name in {'.env','.env.local','accounts.properties'} or p.suffix in {'.key','.pem','.jks','.p12','.db','.zip','.inpetu'}:
            raise SystemExit('Arquivo privado/gerado no codigo publico: '+name)
    source=folder/f'InPETU-Hub-codigo-v{version}.zip'
    subprocess.run(['git','archive','--format=zip','--output',str(source),sha,'--',*paths],check=True)
    with zipfile.ZipFile(source) as zipped:
        if zipped.testzip() is not None:
            raise SystemExit('ZIP de codigo invalido.')
    assets=sorted(p for p in folder.iterdir() if p.is_file())
    checksums=folder/'SHA256SUMS.txt'
    checksums.write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n' for p in assets),encoding='utf-8')
    tag='v'+version
    repo=os.environ['GITHUB_REPOSITORY']
    existing=subprocess.run(['gh','release','view',tag,'--repo',repo,'--json','isDraft,url'],text=True,capture_output=True)
    if existing.returncode==0:
        release=json.loads(existing.stdout)
        if release['isDraft']:
            raise SystemExit('Rascunho existente; use um numero novo apos revisao.')
        print('Release imutavel ja publicada: '+release['url'])
        return
    ref=subprocess.run(['gh','api',f'repos/{repo}/git/ref/tags/{tag}'],text=True,capture_output=True)
    if ref.returncode==0:
        obj=json.loads(ref.stdout)['object']
        if obj['type']!='commit' or obj['sha']!=sha:
            raise SystemExit('Tag existente em outro objeto; nao sera substituida.')
    elif 'HTTP 404' not in ref.stderr:
        raise SystemExit('Nao foi possivel conferir a tag.')
    subprocess.run(['gh','release','create',tag,*[str(p) for p in assets],str(checksums),
                    '--repo',repo,'--target',sha,'--title','InPETU Hub '+version,
                    '--notes-file','RELEASE-NOTES.md','--latest'],check=True)
    print('Publicados os seis pacotes nativos, iniciadores e codigo; nenhum banco ou chave foi incluido.')

if __name__=='__main__':
    main()
