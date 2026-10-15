"""Teste do formato real de backup com dados fictícios e diretórios temporários."""
import os
import pathlib
import shutil
import secrets
import subprocess
import tempfile

root = pathlib.Path(__file__).resolve().parents[1]
java = root / '.tools' / 'jdk' / 'bin' / ('java.exe' if os.name == 'nt' else 'java')
if not java.is_file():
    java = pathlib.Path(shutil.which('java') or '')
source = root / 'scripts' / 'Backup.java'
password = secrets.token_urlsafe(24)


def run(folder, operation, archive, secret=password):
    return subprocess.run([str(java), '--source', '17', str(source), operation, str(archive)],
                          cwd=folder, input=secret + '\n', text=True, capture_output=True, timeout=90)


with tempfile.TemporaryDirectory(prefix='inpetu-backup-test-') as temp:
    base = pathlib.Path(temp)
    original = base / 'original'
    original.mkdir()
    contents = {'.env.local': b'LOCAL_DB_PASSWORD=ficticio\n',
                '.secrets/mfa.key': b'chave-ficticia',
                'data/inpetu-v02.mv.db': os.urandom(8192)}
    for name, content in contents.items():
        file = original / name
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_bytes(content)
    archive = base / 'valid.inpetu'
    assert run(original, 'backup', archive).returncode == 0, 'Falha ao criar backup.'
    encrypted = archive.read_bytes()
    assert run(original, 'backup', archive).returncode != 0
    assert archive.read_bytes() == encrypted, 'Backup existente foi modificado.'
    restored = base / 'restored'
    restored.mkdir()
    assert run(restored, 'restore', archive).returncode == 0, 'Falha ao restaurar backup.'
    for name, content in contents.items():
        assert (restored / name).read_bytes() == content, 'Restauração alterou conteúdo.'
    assert run(restored, 'restore', archive).returncode != 0, 'Sobrescrita não recusada.'
    for name, content in contents.items():
        assert (restored / name).read_bytes() == content
    damaged = base / 'tampered.inpetu'
    altered = bytearray(encrypted)
    altered[-1] ^= 1
    damaged.write_bytes(altered)
    for label, file, secret in [('senha', archive, password + '-errada'),
                                 ('integridade', damaged, password)]:
        destination = base / label
        destination.mkdir()
        assert run(destination, 'restore', file, secret).returncode != 0
        assert not (destination / '.env.local').exists()
        assert not (destination / 'data').exists(), 'Conteúdo não autenticado foi extraído.'
print('Backup: restauração íntegra, senha inválida, adulteração e proteção contra sobrescrita aprovados.')
