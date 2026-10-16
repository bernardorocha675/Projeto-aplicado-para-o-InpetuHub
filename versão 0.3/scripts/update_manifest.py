"""Mantém a lista dos arquivos públicos gerenciados pelo atualizador (uso de manutenção)."""
import argparse
import hashlib
from pathlib import Path
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    import xml.etree.ElementTree as ET
    version = ET.parse(root / 'pom.xml').getroot().findtext('{http://maven.apache.org/POM/4.0.0}version')
    names = subprocess.check_output(['git', '-C', str(root), 'ls-files', '-z', '--', '.']).decode().split('\0')
    lines = [f'# InPETU Hub {version}\n']
    for name in sorted(n for n in names if n and n != 'scripts/update-files.sha256'):
        data = (root / name).read_bytes()
        if name.endswith('.bat'):
            data = data.replace(b'\r\n', b'\n')
        lines.append(hashlib.sha256(data).hexdigest() + '  ' + name + '\n')
    content = ''.join(lines).encode()
    target = root / 'scripts/update-files.sha256'
    if args.check:
        if not target.exists() or target.read_bytes() != content:
            raise SystemExit('Manifesto desatualizado: execute python scripts/update_manifest.py e inclua-o no commit.')
        print('Manifesto de atualização confere com os arquivos públicos.')
    else:
        target.write_bytes(content)


if __name__ == '__main__':
    main()
