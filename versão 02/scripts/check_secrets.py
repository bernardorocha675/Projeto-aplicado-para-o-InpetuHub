"""Bloqueia arquivos privados e credenciais conhecidas de produção no conteúdo versionado."""
import pathlib
import re
import subprocess
import sys

root = pathlib.Path(__file__).resolve().parents[2]
tracked = subprocess.check_output(['git', '-C', str(root), 'ls-files', '-z']).decode().split('\0')
errors = []
private_values = []
private = root / 'versão 02/.secrets/accounts.properties'
if private.is_file():
    for line in private.read_text().splitlines():
        if '=' in line:
            key, value = line.split('=', 1)
            if key.endswith(('.login', '.password')) and value:
                private_values.append(value)
for name in filter(None, tracked):
    if not (name.startswith('versão 02/') or name.startswith('.github/')):
        continue
    p = pathlib.PurePosixPath(name)
    if any(part in ('.secrets', '.tools', 'data', 'backups', 'target') for part in p.parts) or p.name in ('.env', '.env.local') or p.suffix in ('.p12', '.jks', '.pem', '.key', '.zip', '.inpetu'):
        errors.append((name, 'arquivo privado versionado'))
        continue
    path = root / name
    try:
        content = subprocess.check_output(['git', '-C', str(root), 'show', ':' + name]).decode()
    except (UnicodeDecodeError, FileNotFoundError):
        continue
    if any(value in content for value in private_values):
        errors.append((name, 'dado presente no arquivo privado de contas'))
    if re.search(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----', content):
        errors.append((name, 'chave privada'))
    if re.search(r'\b(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|AKIA[A-Z0-9]{16})\b', content):
        errors.append((name, 'padrão de token'))
    if name.endswith(('.env.example', '.env.local.example')):
        for line in content.splitlines():
            if re.match(r'(?:.*PASSWORD|ADMIN_LOGIN|MAIL_USERNAME)=.+', line) and not line.startswith('#'):
                errors.append((name, 'credencial preenchida em exemplo'))
for name, reason in errors:
    print(f'{name}: {reason}')
print(f'Verificação dos arquivos versionados: {len(errors)} problema(s).')
sys.exit(bool(errors))
