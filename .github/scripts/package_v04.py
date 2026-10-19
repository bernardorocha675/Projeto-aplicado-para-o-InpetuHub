"""Empacota iniciador nativo + aplicativo + runtime privado, sem configuracoes de usuario."""
import hashlib
import json
import os
from pathlib import Path
import plistlib
import shutil
import subprocess
import sys
import zipfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[2]
project = root / 'aplicativo'
version = (root / 'VERSION').read_text().strip()
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
if ET.parse(project / 'pom.xml').getroot().findtext('m:version', namespaces=ns) != version:
    raise SystemExit('VERSION e pom.xml divergem.')
goos = os.environ['INPETU_GOOS']
arch = os.environ['INPETU_GOARCH']
platform = {'windows': 'Windows', 'darwin': 'macOS', 'linux': 'Linux'}[goos]
label = {'amd64': 'x64', 'arm64': 'ARM64'}[arch]
dist = project / 'dist'
stage = dist / 'stage'
shutil.rmtree(stage, ignore_errors=True)
stage.mkdir(parents=True)
if goos == 'darwin':
    app = stage / 'InPETU Hub.app'
    binary = app / 'Contents/MacOS/InPETU-Hub'
    bundle = app / 'Contents/Resources'
else:
    bundle = stage
    binary = bundle / ('InPETU-Hub.exe' if goos == 'windows' else 'InPETU-Hub')
binary.parent.mkdir(parents=True, exist_ok=True)
bundle.mkdir(parents=True, exist_ok=True)
env = dict(os.environ, CGO_ENABLED='0', GOOS=goos, GOARCH=arch)
flags = f'-s -w -X main.buildVersion={version}'
if goos == 'windows':
    flags += ' -H=windowsgui'
subprocess.run(['go', 'build', '-trimpath', '-ldflags', flags, '-o', str(binary), '.'],
               cwd=root / 'iniciador', env=env, check=True)
launcher = 'InPETU-Hub.exe' if goos == 'windows' else 'InPETU-Hub'
if goos == 'darwin':
    shutil.copy2(binary, bundle / launcher)
    info = {'CFBundleName': 'InPETU Hub', 'CFBundleDisplayName': 'InPETU Hub',
            'CFBundleIdentifier': 'br.edu.unisenai.inpetuhub',
            'CFBundleExecutable': 'InPETU-Hub', 'CFBundlePackageType': 'APPL',
            'CFBundleVersion': version, 'CFBundleShortVersionString': version,
            'NSHighResolutionCapable': True}
    with (app / 'Contents/Info.plist').open('wb') as stream:
        plistlib.dump(info, stream)
shutil.copy2(project / f'target/inpetu-hub-{version}.jar', bundle / 'app.jar')
go_root = Path(subprocess.check_output(['go','env','GOROOT'], text=True).strip())
shutil.copy2(go_root / 'LICENSE', bundle / 'LICENSE-Go.txt')
java_home = Path(os.environ['JAVA_HOME'])
jlink = java_home / 'bin' / ('jlink.exe' if goos == 'windows' else 'jlink')
subprocess.run([str(jlink), '--add-modules',
                'java.se,jdk.crypto.ec,jdk.unsupported,jdk.zipfs,jdk.charsets,jdk.management,jdk.naming.dns,jdk.localedata,jdk.jfr',
                '--strip-debug', '--no-header-files', '--no-man-pages', '--compress=zip-6',
                '--output', str(bundle / 'runtime')], check=True)
# ZIPs nao carregam symlinks: todas as licencas e arquivos do runtime sao arquivos reais.
for path in sorted((bundle / 'runtime').rglob('*'), key=lambda p: len(p.parts)):
    if path.is_symlink():
        target = path.resolve()
        path.unlink()
        if target.is_dir():
            shutil.copytree(target, path)
        else:
            shutil.copy2(target, path)
java = 'runtime/bin/java.exe' if goos == 'windows' else 'runtime/bin/java'
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
manifest = {'Version': version, 'GOOS': goos, 'GOARCH': arch, 'Jar': 'app.jar',
            'Java': java, 'Launcher': launcher, 'JarSHA256': sha(bundle / 'app.jar'),
            'JavaSHA256': sha(bundle / java)}
(bundle / 'bundle.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
(bundle / 'LEIA-PRIMEIRO.txt').write_text(
    'InPETU Hub ' + version + '\n\n'
    'Abra o iniciador deste pacote. Ele configura o aplicativo sem VS Code, Java ou Maven externos.\n'
    'As cinco contas publicas estao em docs/ACESSOS.md no codigo da release e na tela do iniciador.\n'
    'O banco da 0.4 e novo; a instalacao da 0.3 nao e apagada nem importada automaticamente.\n'
    'Use dados de demonstracao. Para encerrar, use Parar e Fechar iniciador.\n'
    'Executaveis sem certificado comercial: Windows/macOS podem pedir confirmacao do sistema.\n',
    encoding='utf-8')
archive = dist / f'InPETU-Hub-{platform}-{label}-v{version}.zip'
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as zipped:
    for path in sorted(stage.rglob('*')):
        if path.is_file():
            zipped.write(path, path.relative_to(stage).as_posix())
if goos == 'windows':
    shutil.copy2(binary, dist / f'InPETU-Hub-Windows-{label}.exe')
elif goos == 'linux':
    shutil.copy2(binary, dist / f'InPETU-Hub-Linux-{label}.run')
(dist / 'smoke.json').write_text(json.dumps({'executable': str(binary), 'bundle': str(bundle)}), encoding='utf-8')
print(f'Pacote criado: {archive.name}, {archive.stat().st_size} bytes.')
